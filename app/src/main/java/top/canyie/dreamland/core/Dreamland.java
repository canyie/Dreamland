package top.canyie.dreamland.core;

import android.app.AndroidAppHelper;
import android.content.pm.ApplicationInfo;
import android.content.res.Resources;
import android.content.res.ResourcesHidden;
import android.content.res.ResourcesKey;
import android.content.res.TypedArray;
import android.content.res.XResources;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;

import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_InitPackageResources;
import de.robv.android.xposed.callbacks.XCallback;
import dev.rikka.tools.refine.Refine;
import top.canyie.dreamland.BuildConfig;
import top.canyie.dreamland.ipc.IDreamlandManager;

import java.io.File;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import top.canyie.dreamland.ipc.ModuleInfo;
import top.canyie.dreamland.utils.reflect.Reflection;
import top.canyie.dreamland.utils.reflect.UncheckedNoSuchMethodException;
import top.canyie.pine.Pine;
import top.canyie.pine.utils.Primitives;
import top.canyie.pine.xposed.ModuleClassLoader;
import top.canyie.pine.xposed.PineXposed;

import static de.robv.android.xposed.XposedBridge.*;
import static de.robv.android.xposed.XposedHelpers.*;

/**
 * Created by canyie on 2019/11/12.
 */
public final class Dreamland {
    public static final String TAG = "Dreamland";
    public static final int VERSION = BuildConfig.VERSION_CODE;
    public static final String VERSION_NAME = BuildConfig.VERSION_NAME;
    public static final String MANAGER_PACKAGE_NAME = "top.canyie.dreamland.manager";
    public static final File BASE_DIR = new File("/data/misc/dreamland/");
    private static final String XRESOURCES_CONFLICTING_PACKAGE = "com.sygic.aura";

    public static Set<String> loadedPackages = Collections.synchronizedSet(new HashSet<>(1, 1));
    public static Set<String> loadedModules = new HashSet<>();

    public static boolean isSystem;
    public static boolean disableResourcesHook;
    private static boolean triedInitResHook;
    private static final CopyOnWriteSortedSet<XC_InitPackageResources> sInitPackageResourcesCallbacks = new CopyOnWriteSortedSet<>();

    public static void packageReady(IDreamlandManager manager, String packageName, String processName,
                                    ApplicationInfo appInfo, ClassLoader classLoader,
                                    boolean isFirstApp, boolean mainZygote, ModuleInfo[] modules) {
        if (MANAGER_PACKAGE_NAME.equals(packageName)) {
            Log.i(TAG, "This app is dreamland manager.");

            try {
                Reflection<?> ref = Reflection.on("top.canyie.dreamland.manager.core.Dreamland", classLoader);
                IBinder binder = manager.asBinder();
                try {
                    ref.method("init", String.class, int.class, IBinder.class)
                            .callStatic(VERSION_NAME, VERSION, binder);
                } catch (UncheckedNoSuchMethodException ignored) {
                    // Try beta version
                    ref.method("init", int.class, IBinder.class)
                            .callStatic(VERSION, binder);
                }
            } catch (Throwable e) {
                // should never happen
                Log.e(TAG, "Failed to init manager", e);
            }
            return; // Don't load xposed modules in manager process.
        }

        if (modules == null) {
            try {
                modules = manager.getEnabledModulesFor(packageName);
            } catch (RemoteException e) {
                Log.e(TAG, "Failure from remote dreamland service", e);
                return;
            }

            if (modules == null || modules.length == 0) {
                Log.i(Dreamland.TAG, "No module needs to hook into package " + packageName);
                return;
            }
        }

        prepareModulesFor(manager, packageName, processName, modules, mainZygote);
        PineXposed.onPackageLoad(packageName, processName, appInfo, isFirstApp, classLoader);
    }

    public static void prepareModulesFor(IDreamlandManager dm, String packageName, String processName,
                                         ModuleInfo[] modules, boolean mainZygote) {
        try {
            startResourcesHook(dm);
        } catch (Throwable e) {
            Log.e(TAG, "Start resources hook failed", e);
            Dreamland.disableResourcesHook = true;
        }

        Log.i(TAG, "Loading xposed modules for package " + packageName + " process " + processName);
        loadXposedModules(modules, mainZygote);
    }

    public static void loadXposedModules(ModuleInfo[] modules, boolean mainZygote) {
        ClassLoader initCL = Dreamland.class.getClassLoader();
        synchronized (loadedModules) {
            for (ModuleInfo module : modules) {
                String apk;
                if (module == null || TextUtils.isEmpty(apk = module.path)) {
                    Log.e(TAG, "Module list contains empty, skipping");
                    Log.e(TAG, "Module list: " + Arrays.toString(modules));
                    continue;
                }
                if (!loadedModules.add(apk)) continue;
                Log.i(TAG, "Loading xposed module " + apk);
                var librarySearchPath = new StringBuilder();
                if (!TextUtils.isEmpty(module.nativePath))
                    librarySearchPath.append(module.nativePath).append(File.pathSeparator);

                // linker on Android 6.0+ supports loading so file from uncompressed apk
                // If so file is uncompressed, appInfo.nativeLibraryDir will not exist
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    var abis = Pine.is64Bit() ? Build.SUPPORTED_64_BIT_ABIS : Build.SUPPORTED_32_BIT_ABIS;
                    for (String abi : abis)
                        librarySearchPath.append(apk).append("!/lib/").append(abi).append(File.pathSeparator);
                }

                var cl = new ModuleClassLoader(apk, librarySearchPath.toString(), initCL);
                // Only main zygote (non-secondary zygote) will starts the system server
                PineXposed.loadOpenedModule(apk, cl, mainZygote);
            }
        }
    }

    public static void startResourcesHook(IDreamlandManager manager) throws Exception {
        if (triedInitResHook) return;
        triedInitResHook = true;

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
                    @Override
                    protected void beforeHookedMethod(XC_MethodHook.MethodHookParam param) {
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
        final Class<?> classActivityRes = XposedHelpers.findClassIfExists("android.app.ResourcesManager$ActivityResource",
                classGTLR.getClassLoader());
        final ThreadLocal<Object> latestResKey = new ThreadLocal<>();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            var hook = new XC_MethodHook() {
                @Override protected void afterHookedMethod(MethodHookParam param) {
                    // At least on OnePlus 5, the method has an additional parameter compared to AOSP.
                    Object activityToken;
                    try {
                        final int activityTokenIdx = getParameterIndexByType(param.method, IBinder.class);
                        activityToken = param.args[activityTokenIdx];
                    } catch (NoSuchMethodError ignored) {
                        // Android S createResources()
                        activityToken = null;
                    }
                    final int resKeyIdx = getParameterIndexByType(param.method, ResourcesKey.class);

                    String resDir = ((ResourcesKey) param.args[resKeyIdx]).mResDir;
                    XResources newRes = cloneToXResources(param, resDir);
                    if (newRes == null) {
                        return;
                    }

                    synchronized (param.thisObject) {
                        List<Object> resourceReferences;
                        if (activityToken != null) {
                            Object activityResources = callMethod(param.thisObject,
                                    "getOrCreateActivityResourcesStructLocked", activityToken);
                            resourceReferences = (List<Object>) getObjectField(activityResources, "activityResources");
                        } else {
                            resourceReferences = (List<Object>) getObjectField(param.thisObject, "mResourceReferences");
                        }

                        if (activityToken == null || classActivityRes == null) {
                            resourceReferences.add(new WeakReference<>(newRes));
                        } else {
                            // Android S createResourcesForActivity(), ActivityResouces#activityResources is List<ActivityResource>
                            var activityRes = XposedHelpers.newInstance(classActivityRes);
                            XposedHelpers.setObjectField(activityRes, "resources", new WeakReference<>(newRes));
                            resourceReferences.add(activityRes);
                        }
                    }
                }
            };

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                hookAllMethods(classGTLR, "createResources", hook);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                    hookAllMethods(classGTLR, "createResourcesForActivity", hook);
            } else  {
                hookAllMethods(classGTLR, "getOrCreateResources", hook);
            }
        } else {
            hookAllConstructors(ResourcesKey.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    latestResKey.set(param.thisObject);
                }
            });

            hookAllMethods(classGTLR, "getTopLevelResources", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    latestResKey.set(null);
                }

                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    Object key = latestResKey.get();
                    if (key == null) {
                        return;
                    }
                    latestResKey.set(null);

                    String resDir = (String) getObjectField(key, "mResDir");
                    XResources newRes = cloneToXResources(param, resDir);
                    if (newRes == null) {
                        return;
                    }

                    @SuppressWarnings("unchecked")
                    var mActiveResources = (Map<Object, WeakReference<Resources>>) getObjectField(
                            param.thisObject, "mActiveResources");

                    synchronized (param.thisObject) {
                        var existing = mActiveResources.put(key, new WeakReference<>(newRes));
                        if (existing != null && existing.get() != null && existing.get().getAssets() != newRes.getAssets()) {
                            existing.get().getAssets().close();
                        }
                    }
                }
            });

            // This method exists only on CM-based ROMs
            hookAllMethods(classGTLR, "getTopLevelThemedResources", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    String resDir = (String) param.args[0];
                    cloneToXResources(param, resDir);
                }
            });
        }

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
        ResourcesHidden originSystemResWrapper = Refine.unsafeCast(originSystemRes);
        XResources systemRes = new XResources(originSystemResWrapper.getClassLoader());
        Refine.<ResourcesHidden>unsafeCast(systemRes).setImpl(originSystemResWrapper.getImpl());
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
        ResourcesHidden originWrapper = Refine.unsafeCast(origin);
        XResources newRes = new XResources(originWrapper.getClassLoader());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Refine.<ResourcesHidden>unsafeCast(newRes).setImpl(originWrapper.getImpl());
        } else {
            // No ResourcesImpl on pre Nougat, we have to manually copy all fields to the new one
            for (Class<?> clz = origin.getClass();clz != null && clz != Object.class;clz = clz.getSuperclass()) {
                for (Field field : clz.getDeclaredFields()) {
                    if (Modifier.isStatic(field.getModifiers())) continue;
                    try {
                        field.setAccessible(true);
                        field.set(newRes, field.get(origin));
                    } catch (Exception e) {
                        // Just in case...
                        XposedBridge.log(e);
                    }
                }
            }
        }
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
