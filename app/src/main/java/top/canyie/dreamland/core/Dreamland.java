package top.canyie.dreamland.core;

import android.app.Activity;
import android.app.AndroidAppHelper;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.res.Resources;
import android.content.res.ResourcesKey;
import android.content.res.TypedArray;
import android.content.res.XResources;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_InitPackageResources;
import de.robv.android.xposed.callbacks.XCallback;
import top.canyie.dreamland.BuildConfig;
import top.canyie.dreamland.ipc.IDreamlandManager;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.List;

import top.canyie.dreamland.utils.reflect.Reflection;
import top.canyie.pine.utils.Primitives;
import top.canyie.pine.xposed.PineXposed;

import static de.robv.android.xposed.XposedBridge.*;
import static de.robv.android.xposed.XposedHelpers.*;

/**
 * Created by canyie on 2019/11/12.
 */
public final class Dreamland {
    public static final String TAG = "Dreamland";
    public static final int VERSION = BuildConfig.VERSION_CODE;
    public static final String MANAGER_PACKAGE_NAME = "top.canyie.dreamland.manager";
    public static final String OLD_MANAGER_PACKAGE_NAME = "com.canyie.dreamland.manager";
    public static final File BASE_DIR = new File("/data/misc/dreamland/");
    private static final String XRESOURCES_CONFLICTING_PACKAGE = "com.sygic.aura";

    public static String processName = "";
    public static String packageName = "";
    public static ApplicationInfo appInfo;
    public static boolean isSystem;
    public static ClassLoader classLoader;
    public static boolean disableResourcesHook;
    private static boolean hooked;
    private static final CopyOnWriteSortedSet<XC_InitPackageResources> sInitPackageResourcesCallbacks = new CopyOnWriteSortedSet<>();

    public static void ready(IDreamlandManager manager) {
        if (canLoadXposedModules()) {
            hooked = true;
            if (MANAGER_PACKAGE_NAME.equals(packageName)) {
                Log.i(TAG, "This is dreamland manager.");
                try {
                    Reflection.on("top.canyie.dreamland.manager.core.Dreamland", classLoader)
                            .method("init", int.class, IBinder.class)
                            .callStatic(VERSION, manager.asBinder());
                } catch (Throwable e) {
                    // should never happen
                    Log.e(TAG, "Failed to init manager", e);
                }
                return; // Don't load xposed modules in manager process.
            } else if (OLD_MANAGER_PACKAGE_NAME.equals(packageName)) {
                Log.w(TAG, "Detected old dreamland manager");
                try {
                    Class<?> mainActivityClass = classLoader.loadClass("com.canyie.dreamland.manager.ui.activities.MainActivity");
                    XposedHelpers.findAndHookMethod(
                            Activity.class,"onCreate", Bundle.class, new XC_MethodHook() {
                                @Override protected void afterHookedMethod(MethodHookParam param) {
                                    if (param.thisObject.getClass() != mainActivityClass) return;
                                    String msg = "The Dreamland manager is obsolete " +
                                            "and not compatible with current framework version! \n" +
                                            "Please upgrade it!";
                                    Toast.makeText((Context) param.thisObject, msg, Toast.LENGTH_SHORT).show();
                                }
                            });
                } catch (Exception e) {
                    Log.e(TAG, "Failed to hook old dreamland manager", e);
                }
                return;
            }
            callLoadPackage();
        }
    }
				
				public static void callLoadPackage() {
								Log.i(TAG, "Loading xposed-style modules for package " + packageName + " process " + processName);
								PineXposed.onPackageLoad(packageName, processName, appInfo, true, classLoader);
				}

    private static boolean canLoadXposedModules() {
        return classLoader != null && !hooked;
    }

    public static void loadXposedModules(String[] modules, boolean mainZygote) {
        if (modules == null || modules.length == 0) {
            //Log.d(TAG, "No module needed to load, skip.");
            return;
        }

        for (String module : modules) {
            if (TextUtils.isEmpty(module)) {
                Log.e(TAG, "Module list contains empty, skipping");
                Log.e(TAG, "Module list: " + Arrays.toString(modules));
                continue;
            }
            Log.i(TAG, "Loading xposed module " + module);
            // Only main zygote (non-secondary zygote) will starts the system server
            PineXposed.loadModule(new File(module), mainZygote);
        }
    }

    public static void startResourcesHook(IDreamlandManager manager) throws Exception {
        disableResourcesHook = disableResourcesHook || !manager.isResourcesHookEnabled();

        PineXposed.setExtHandler(callback -> {
            if (callback instanceof IXposedHookInitPackageResources) {
                if (disableResourcesHook) {
                    Log.e(PineXposed.TAG, "    Cannot load callback class " + callback.getClass().getName());
                    Log.e(PineXposed.TAG, "    This class requires resources hook but it was disabled, skipping it");
                } else {
                    sInitPackageResourcesCallbacks.add(new XC_InitPackageResources.Wrapper((IXposedHookInitPackageResources) callback));
                }
            }
        });

        if (disableResourcesHook) return;

        findAndHookMethod("android.app.ApplicationPackageManager", null,
                "getResourcesForApplication", ApplicationInfo.class, new XC_MethodHook() {
                    @Override protected void beforeHookedMethod(XC_MethodHook.MethodHookParam param) {
                        ApplicationInfo app = (ApplicationInfo) param.args[0];
                        XResources.setPackageNameForResDir(app.packageName,
                                app.uid == android.os.Process.myUid() ? app.sourceDir : app.publicSourceDir);
                    }
                });

        /*
         * getTopLevelResources(a)
         *   -> getTopLevelResources(b)
         *     -> key = new ResourcesKey()
         *     -> r = new Resources()
         *     -> mActiveResources.put(key, r)
         *     -> return r
         */

        // Dreamland: only supports android 7.0+
        final Class<?> classGTLR = Class.forName("android.app.ResourcesManager");
        final ThreadLocal<Object> latestResKey = new ThreadLocal<>();

        hookAllMethods(classGTLR, "getOrCreateResources", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                // At least on OnePlus 5, the method has an additional parameter compared to AOSP.
                final int activityTokenIdx = getParameterIndexByType(param.method, IBinder.class);
                final int resKeyIdx = getParameterIndexByType(param.method, ResourcesKey.class);

                String resDir = ((ResourcesKey) param.args[resKeyIdx]).mResDir;
                XResources newRes = cloneToXResources(param, resDir);
                if (newRes == null) {
                    return;
                }

                Object activityToken = param.args[activityTokenIdx];
                synchronized (param.thisObject) {
                    List<WeakReference<Resources>> resourceReferences;
                    if (activityToken != null) {
                        Object activityResources = callMethod(param.thisObject, "getOrCreateActivityResourcesStructLocked", activityToken);
                        resourceReferences = (List<WeakReference<Resources>>) getObjectField(activityResources, "activityResources");
                    } else {
                        resourceReferences = (List<WeakReference<Resources>>) getObjectField(param.thisObject, "mResourceReferences");
                    }
                    resourceReferences.add(new WeakReference(newRes));
                }
            }
        });

        // Replace TypedArrays with XTypedArrays
        hookAllConstructors(TypedArray.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                TypedArray typedArray = (TypedArray) param.thisObject;
                Resources res = typedArray.getResources();
                if (res instanceof XResources) {
                    Primitives.setObjectClass(typedArray, XResources.XTypedArray.class);
                }
            }
        });

        // Replace system resources
        Resources originSystemRes = Resources.getSystem();
        XResources systemRes = new XResources(originSystemRes.getClassLoader());
        systemRes.setImpl(originSystemRes.getImpl());
        systemRes.initObject(null);
        setStaticObjectField(Resources.class, "mSystem", systemRes);

        XResources.init(latestResKey);
    }

    private static XResources cloneToXResources(XC_MethodHook.MethodHookParam param, String resDir) {
        Resources origin = (Resources) param.getResult();
        if (origin == null || origin instanceof XResources ||
                XRESOURCES_CONFLICTING_PACKAGE.equals(AndroidAppHelper.currentPackageName())) {
            return null;
        }

        // Replace the returned resources with our subclass.
        XResources newRes = new XResources(origin.getClassLoader());
        newRes.setImpl(origin.getImpl());
        newRes.initObject(resDir);

        // Invoke handleInitPackageResources().
        if (newRes.isFirstLoad()) {
            String packageName = newRes.getPackageName();
            XC_InitPackageResources.InitPackageResourcesParam resparam = new XC_InitPackageResources.InitPackageResourcesParam(sInitPackageResourcesCallbacks);
            resparam.packageName = packageName;
            resparam.res = newRes;
            XCallback.callAll(resparam);
        }

        param.setResult(newRes);
        return newRes;
    }
}
