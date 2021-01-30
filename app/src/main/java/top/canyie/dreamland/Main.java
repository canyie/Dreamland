package top.canyie.dreamland;

import android.annotation.SuppressLint;
import android.app.LoadedApk;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.res.CompatibilityInfo;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XResources;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Keep;

import dalvik.system.InMemoryDexClassLoader;
import dalvik.system.PathClassLoader;
import de.robv.android.xposed.DexCreator;
import de.robv.android.xposed.XposedBridge;
import top.canyie.dreamland.core.Dreamland;
import top.canyie.dreamland.core.PackageMonitor;
import top.canyie.dreamland.ipc.BinderServiceProxy;
import top.canyie.dreamland.ipc.IDreamlandManager;
import top.canyie.dreamland.ipc.DreamlandManagerService;
import top.canyie.dreamland.utils.AppConstants;
import top.canyie.dreamland.utils.RuntimeUtils;
import top.canyie.dreamland.utils.reflect.Reflection;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;

import mirror.android.app.ActivityThread;
import mirror.android.os.ServiceManager;
import top.canyie.pine.Pine;
import top.canyie.pine.PineConfig;
import top.canyie.pine.callback.MethodHook;

import static top.canyie.dreamland.core.Dreamland.TAG;

/**
 * Created by canyie on 2019/11/12.
 */
@SuppressWarnings("unused") @SuppressLint({"PrivateApi", "DiscouragedPrivateApi"}) @Keep
public final class Main {
    private static final String TARGET_BINDER_SERVICE_NAME = Context.CLIPBOARD_SERVICE;
    private static final String TARGET_BINDER_SERVICE_DESCRIPTOR = "android.content.IClipboard";
    private static boolean classLoaderReady;
    private static boolean clipboardServiceReplaced, packageManagerReady, activityManagerReady;
    //private static int sWebViewZygoteUid = -1;
    private static boolean mainZygote;
    private static boolean inited;

    private static void commonInit(boolean system) {
        inited = true;
        ClassLoader classLoader = Main.class.getClassLoader();

        // Try initialize resources hook first.
        // This needs to be performed as early as possible to avoid trying to load XResources when XResourcesSuperClass is not loaded.
        try {
            initXResources(classLoader);
        } catch (Throwable e) {
            Log.e(TAG, "Resources hook initialize failed", e);
            Dreamland.disableResourcesHook = true;
        }

        PineConfig.debug = true;
        PineConfig.debuggable = false;

        // Don't load another .so file, all codes are included in libriru_dreamland.so
        PineConfig.libLoader = null;

        // Don't disable hidden api policy by default, only do it in enabled apps.
        // Module needs it, but framework itself don't.
        // Framework's dex on /system/framework, is a platform dex file,
        // and we will disable any restriction for platform domain.
        PineConfig.disableHiddenApiPolicy = false;
        Pine.ensureInitialized();
        if (system) Pine.setJitCompilationAllowed(false);

        String realAbi = SystemProperties.get( "ro.product.cpu.abi", "");
        if (TextUtils.isEmpty(realAbi)) {
            Log.e(TAG, "System property 'ro.product.cpu.abi' is missing on the device");
            mainZygote = false;
        } else {
            // For 32-bit process running on 64-bit device, Build.CPU_ABI is 32-bit api
            mainZygote = Build.CPU_ABI.equalsIgnoreCase(realAbi);
        }
    }

    public static int zygoteInit() {
        try {
            commonInit(true);
            return 0;
        } catch (Throwable e) {
            try {
                Log.e(TAG, "Dreamland init error", e);
            } catch (Throwable ignored) {
            }
        }
        return 1;
    }

    @SuppressLint("NewApi")
    private static void initXResources(ClassLoader myCL) throws Exception {
        Resources res = Resources.getSystem();

        Class resClass = res.getClass();
        Class<?> taClass = TypedArray.class;
        try {
            TypedArray ta = res.obtainTypedArray(res.getIdentifier("preloaded_drawables", "array", "android"));
            taClass = ta.getClass();
            ta.recycle();
        } catch (Resources.NotFoundException e) {
            XposedBridge.log(e);
        }

        ClassLoader dummy;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Load dex from memory to prevent some detections
            RuntimeUtils.makeExtendable(resClass);
            RuntimeUtils.makeExtendable(taClass);
            ByteBuffer dexForXResources = DexCreator.create("XResources", resClass);
            ByteBuffer dexForXTypedArray = DexCreator.create("XTypedArray", taClass);
            ByteBuffer[] buffers = new ByteBuffer[] { dexForXResources, dexForXTypedArray };
            dummy = new InMemoryDexClassLoader(buffers, myCL.getParent());
        } else {
            String dexForXResources = ensureSuperDexFor("XResources", resClass, Resources.class);
            String dexForXTypedArray = ensureSuperDexFor("XTypedArray", taClass, TypedArray.class);
            dummy = new PathClassLoader(dexForXResources + File.pathSeparator + dexForXTypedArray, myCL.getParent());
        }

        // Inject a ClassLoader for the created classes as parent of XposedBridge's ClassLoader.
        RuntimeUtils.setParent(myCL, dummy);

        // native initialize resources hook.
        // this must be executed after XResourcesSuperClass is created,
        // because initXResourcesNative will initialize XResources.
        if (!initXResourcesNative(myCL)) {
            throw new IllegalStateException("Resources hook init failed from native");
        }
    }

    @SuppressLint("SetWorldReadable")
    private static String ensureSuperDexFor(String clz, Class<?> realSuperClz, Class<?> topClz) throws IOException {
        RuntimeUtils.makeExtendable(realSuperClz);
        File dexFile = DexCreator.ensure(clz, realSuperClz, topClz);
        dexFile.setReadable(true, false);
        return dexFile.getAbsolutePath();
    }

    public static void onSystemServerStart() {
        try {
            //if (!inited) commonInit(true);

            // Start loading the properties asynchronously first to minimize time-consuming.
            DreamlandManagerService dms = DreamlandManagerService.start();

            Dreamland.isSystem = true;

            // Call chain:
            //  SystemServer.run()
            //    -> SystemServer.createSystemContext()
            //      -> ActivityThread.systemMain()
            //        -> ActivityThread.attach(true, 0)
            //          -> ActivityThread.getSystemContext()
            //            -> if (mSystemContext == null)
            //            -> mSystemContext = ContextImpl.createSystemContext()
            //            -> return mSystemContext
            //    -> SystemServer.startBootstrapServices()

            Pine.hook(android.app.ActivityThread.class.getDeclaredMethod("systemMain"), new MethodHook() {
                @Override public void afterCall(Pine.CallFrame callFrame) throws Throwable {
                    String[] modules = dms.getEnabledModulesForSystemServer();
                    if (modules != null && modules.length != 0) {
                        ClassLoader cl = Thread.currentThread().getContextClassLoader();
                        Dreamland.packageName = AppConstants.ANDROID;
                        Dreamland.processName = AppConstants.ANDROID; // it's actually system_server, but other functions return this as well
                        Dreamland.appInfo = null;
                        Dreamland.classLoader = cl;
                        assert cl != null;
                        Pine.hook(cl.loadClass("com.android.server.SystemServer")
                                        .getDeclaredMethod("startBootstrapServices"),
                                new MethodHook() {
                                    @Override public void beforeCall(Pine.CallFrame callFrame) {
                                        Dreamland.loadXposedModules(modules, true);
                                        Dreamland.callLoadPackage();
                                    }
                                });
                    }
                }
            });

            Object base = ServiceManager.getIServiceManager.callStatic();
            ServiceManager.sServiceManager.setStaticValue(Reflection.on("android.os.IServiceManager")
                    .proxy((proxy, method, args) -> {
                        if ("addService".equals(method.getName())) {
                            Object serviceName = args[0];
                            if (TARGET_BINDER_SERVICE_NAME.equals(serviceName)) {
                                Log.i(TAG, "Replacing clipboard service");
                                args[1] = new BinderServiceProxy((Binder) args[1],
                                        TARGET_BINDER_SERVICE_DESCRIPTOR, dms, dms::isEnabledFor);
                                //args[2] = true; // Do not supports isolated processes yet
                                clipboardServiceReplaced = true;
                            } else if(Context.ACTIVITY_SERVICE.equals(serviceName)) {
                                Log.i(TAG, "Activity manager is available");
                                activityManagerReady = true;
                                PackageMonitor.startRegister();
                            } else if ("package".equals(serviceName)) {
                                Log.i(TAG, "Package manager is available");
                                dms.setPackageManager((IBinder) args[1]);
                                packageManagerReady = true;
                            }

                            if (clipboardServiceReplaced && activityManagerReady && packageManagerReady) {
                                ServiceManager.sServiceManager.setStaticValue(base);
                            }
                        }

                        try {
                            return method.invoke(base, args);
                        } catch (InvocationTargetException e) {
                            throw e.getTargetException();
                        }
                    }));
        } catch (Throwable e) {
            try {
                Log.e(TAG, "Dreamland error in system server", e);
            } catch (Throwable ignored) {
            }
        }
    }

    public static void onAppProcessStart(IBinder service) {
        try {
            /*if (inited) // JIT compilation was disabled in zygote
                Pine.setJitCompilationAllowed(true);
            else
                commonInit(false);*/

            Dreamland.isSystem = false;

            if (service == null) {
                IBinder clipboard;
                try {
                    clipboard = ServiceManager.getService.callStatic(TARGET_BINDER_SERVICE_NAME);
                } catch (Exception e) {
                    Log.e(TAG, "Couldn't find the clipboard service", e);
                    return;
                }

                if (clipboard == null) {
                    // Isolated process or google gril service process is not allowed to access clipboard service
                    Log.w(TAG, "Clipboard service is unavailable in current process, skipping");
                    return;
                }

                try {
                    service = BinderServiceProxy.getBinderFrom(clipboard, TARGET_BINDER_SERVICE_DESCRIPTOR);
                    if (service == null) {
                        // service is null => should not hook into this process.
                        // DreamlandManager is not exposed to disabled app now.
                        return;
                    }
                } catch (Exception e) {
                    if (e.getClass() == RuntimeException.class
                            && "Unknown transaction code".equals(e.getMessage())) {
                        // Unknown transaction => remote service doesn't handle it, ignore this process.
                        return;
                    }
                    Log.e(TAG, "Couldn't check whether the current process is needs to hook", e);
                    return;
                }
            }

            IDreamlandManager dm = IDreamlandManager.Stub.asInterface(service);

            // Disable hidden api policy for application domain because modules may need it.
            // Restrictions for platform domain was disabled in init()
            Pine.disableHiddenApiPolicy(true, false);

            Pine.setJitCompilationAllowed(true);

            Pine.hook(android.app.ActivityThread.class.getDeclaredMethod("handleBindApplication",
                    ActivityThread.AppBindData.REF.unwrap()),
                    new MethodHook() {
                        @Override public void beforeCall(Pine.CallFrame callFrame) {
                            android.app.ActivityThread activityThread = (android.app.ActivityThread) callFrame.thisObject;
                            Object appBindData = callFrame.args[0];
                            ApplicationInfo appInfo = ActivityThread.AppBindData.appInfo.getValue(appBindData);
                            CompatibilityInfo compatInfo = ActivityThread.AppBindData.compatInfo.getValue(appBindData);
                            ActivityThread.mBoundApplication.setValue(activityThread, appBindData);
                            LoadedApk loadedApk = activityThread.getPackageInfoNoCheck(appInfo, compatInfo);
                            XResources.setPackageNameForResDir(appInfo.packageName, loadedApk.getResDir());

                            try {
                                Dreamland.appInfo = appInfo;
                                Dreamland.packageName = appInfo.packageName.equals("android") ? "system" : appInfo.packageName;
                                Dreamland.processName = ActivityThread.AppBindData.processName.getValue(appBindData);
                                Dreamland.classLoader = loadedApk.getClassLoader();
                                Dreamland.ready(dm, mainZygote);
                            } catch (Exception e) {
                                Log.e(TAG, "Install hooks failed", e);
                            }
                        }
                    });

            /*Pine.hook(LoadedApk.class.getDeclaredMethod("getClassLoader"),
                    new MethodHook() {
                        @Override public void afterCall(Pine.CallFrame callFrame) {
                            ClassLoader classLoader = (ClassLoader) callFrame.getResult();
                            if (classLoader == null) {
                                Log.w(TAG, "LoadedApk.getClassLoader() return null, ignore.");
                                return;
                            }
                            if (!classLoaderReady) {
                                classLoaderReady = true;
                                Dreamland.classLoader = classLoader;

                                try {
                                    Dreamland.ready(dm);
                                } catch (Exception e) {
                                    Log.e(TAG, "Install hooks failed", e);
                                }
                            }
                        }
                    });*/
        } catch (Throwable e) {
            try {
                Log.e(TAG, "Dreamland error in app process", e);
            } catch (Throwable ignored) {
            }
        }
    }

    @SuppressWarnings("JavaJniMissingFunction")
    private static native boolean initXResourcesNative(ClassLoader classLoader);
}
