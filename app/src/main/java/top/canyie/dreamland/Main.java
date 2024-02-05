package top.canyie.dreamland;

import android.annotation.SuppressLint;
import android.app.AndroidAppHelper;
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
import android.util.Log;

import androidx.annotation.Keep;

import dalvik.system.InMemoryDexClassLoader;
import dalvik.system.PathClassLoader;
import de.robv.android.xposed.DexCreator;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import top.canyie.dreamland.core.Dreamland;
import top.canyie.dreamland.core.PackageMonitor;
import top.canyie.dreamland.ipc.BinderServiceProxy;
import top.canyie.dreamland.ipc.IDreamlandManager;
import top.canyie.dreamland.ipc.DreamlandManagerService;
import top.canyie.dreamland.ipc.ModuleInfo;
import top.canyie.dreamland.utils.AppConstants;
import top.canyie.dreamland.utils.RuntimeUtils;
import top.canyie.dreamland.utils.reflect.Reflection;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;

import mirror.android.app.ActivityThread;
import mirror.android.os.ServiceManager;
import top.canyie.pine.Pine;
import top.canyie.pine.PineConfig;
import top.canyie.pine.callback.MethodHook;
import top.canyie.pine.enhances.PineEnhances;
import top.canyie.pine.xposed.PineXposed;

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
    public static boolean mainZygote;
    private static boolean inited;

    private static void commonInit() {
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

        Pine.ensureInitialized();
        Pine.setJitCompilationAllowed(false);
        PineEnhances.libLoader = () -> {};
        PineEnhances.enableDelayHook();
    }

    public static int zygoteInit() {
        try {
            commonInit();
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

        Class<? extends Resources> resClass = res.getClass();
        Class<?> taClass = TypedArray.class;
        try {
            @SuppressLint("DiscouragedApi")
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

    private static void hookPackageLoad(IDreamlandManager dm) throws Exception {
        Pine.hook(android.app.ActivityThread.class.getDeclaredMethod("handleBindApplication",
                ActivityThread.AppBindData.REF.unwrap()),
                new MethodHook() {
                    @Override public void beforeCall(Pine.CallFrame callFrame) {
                        android.app.ActivityThread activityThread = (android.app.ActivityThread) callFrame.thisObject;
                        Object appBindData = callFrame.args[0];
                        ApplicationInfo appInfo = ActivityThread.AppBindData.appInfo.getValue(appBindData);
                        if (appInfo == null) return;
                        CompatibilityInfo compatInfo = ActivityThread.AppBindData.compatInfo.getValue(appBindData);
                        ActivityThread.mBoundApplication.setValue(activityThread, appBindData);
                        LoadedApk loadedApk = activityThread.getPackageInfoNoCheck(appInfo, compatInfo);
                        XResources.setPackageNameForResDir(appInfo.packageName, loadedApk.getResDir());

                        String packageName = appInfo.packageName.equals("android") ? "system" : appInfo.packageName;
                        String processName = ActivityThread.AppBindData.processName.getValue(appBindData);
                        ClassLoader classLoader = loadedApk.getClassLoader();
                        Dreamland.packageReady(dm, packageName, processName, appInfo, classLoader, true, mainZygote, null);
                    }
                });

        // when a package is loaded for an existing process, trigger the callbacks as well
        Pine.hook(LoadedApk.class.getDeclaredConstructor(android.app.ActivityThread.class,
                ApplicationInfo.class, CompatibilityInfo.class, ClassLoader.class,
                boolean.class, boolean.class, boolean.class), new MethodHook() {
            @Override public void afterCall(Pine.CallFrame callFrame) {
                if (Dreamland.loadedPackages.isEmpty()) return; // First app should call handleBindApplication
                LoadedApk loadedApk = (LoadedApk) callFrame.thisObject;
                if (!XposedHelpers.getBooleanField(loadedApk, "mIncludeCode")) return;
                String packageName = loadedApk.getPackageName();
                XResources.setPackageNameForResDir(packageName, loadedApk.getResDir());

                if (AppConstants.ANDROID.equals(packageName)) return;
                if (!Dreamland.loadedPackages.add(packageName)) return; // Already hooked

                // OnePlus magic...
                if (Log.getStackTraceString(new Throwable()).
                        contains("android.app.ActivityThread$ApplicationThread.schedulePreload")) {
                    Log.d(TAG, "LoadedApk#<init> maybe oneplus's custom opt, skip");
                    return;
                }

                GetClassLoaderHook.install(dm, loadedApk, packageName, AndroidAppHelper.currentProcessName(), loadedApk.getApplicationInfo(), false);
            }
        });
    }

    public static void onSystemServerStart() {
        try {
            if (!inited) commonInit();
            mainZygote = true;

            Log.i(TAG, "System server is started!");

            // Start loading the properties asynchronously first to minimize time-consuming.
            DreamlandManagerService dms = DreamlandManagerService.start();

            Dreamland.isSystem = true;

            // Replace ServiceManager so we can know services are ready and replace them
            Object base = ServiceManager.getIServiceManager.callStatic();
            ServiceManager.sServiceManager.setStaticValue(Reflection.on("android.os.IServiceManager")
                    .proxy((proxy, method, args) -> {
                        if ("addService".equals(method.getName())) {
                            Object serviceName = args[0];
                            if (TARGET_BINDER_SERVICE_NAME.equals(serviceName)) {
                                // Replace the clipboard service so apps can acquire binder
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

            // We want to know if sepolicy patch rules is loaded properly,
            // so we hook the method even if no modules need to hook system server.
            try {
                // ActivityThread#systemMain() is inlined in Android T
                // So we also hook ZygoteInit#handleSystemServerProcess()
                // Call chain:
                //  ZygoteInit.handleSystemServerProcess()
                //  SystemServer.run()
                //    -> SystemServer.createSystemContext()
                //      -> ActivityThread.systemMain()
                //        -> ActivityThread.attach(true, 0)
                //          -> ActivityThread.getSystemContext()
                //            -> if (mSystemContext == null)
                //            -> mSystemContext = ContextImpl.createSystemContext()
                //            -> return mSystemContext
                //    -> SystemServer.startBootstrapServices()
                final var called = new boolean[] { false };
                var hook = new MethodHook() {
                    @Override public void afterCall(Pine.CallFrame callFrame) throws Throwable {
                        // Both systemMain() and handleSystemServerProcess() use this callback
                        if (called[0]) return;
                        called[0] = true;
                        dms.onSystemServerHookCalled();
                        Dreamland.loadedPackages.add(AppConstants.ANDROID);
                        ModuleInfo[] modules = dms.getEnabledModulesForSystemServer();
                        if (modules != null && modules.length != 0) {
                            ClassLoader cl = Thread.currentThread().getContextClassLoader();
                            final String packageName = AppConstants.ANDROID;
                            final String processName = AppConstants.ANDROID; // it's actually system_server, but other functions return this as well

                            Dreamland.prepareModulesFor(dms, packageName, processName, modules, true);
                            Class<?>[] paramTypes = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                                    ? new Class<?>[] { cl.loadClass("com.android.server.utils.TimingsTraceAndSlog") }
                                    : new Class<?>[0];
                            Pine.hook(cl.loadClass("com.android.server.SystemServer")
                                            .getDeclaredMethod("startBootstrapServices", paramTypes),
                                    new MethodHook() {
                                        @Override public void beforeCall(Pine.CallFrame callFrame) {
                                            PineXposed.onPackageLoad(packageName, processName, null, true, cl);
                                        }
                                    });
                        }

                        if (dms.hookLoadPackageInSystemServer()) {
                            Log.i(TAG, "Hooking package load in system server");
                            hookPackageLoad(dms);
                        } else {
                            Log.i(TAG, "No need to hook loadPackage in system server");
                        }
                    }
                };

                Pine.hook(android.app.ActivityThread.class.getDeclaredMethod("systemMain"), hook);
                for (Method method : Class.forName("com.android.internal.os.ZygoteInit").getDeclaredMethods()) {
                    if ("handleSystemServerProcess".equals(method.getName()))
                        Pine.hook(method, hook);
                }
            } catch (Throwable e) {
                Log.e(TAG, "Cannot hook methods in system_server. Maybe the SEPolicy patch rules not loaded properly by Magisk.", e);
            }
        } catch (Throwable e) {
            try {
                Log.e(TAG, "Dreamland error in system server", e);
            } catch (Throwable ignored) {
            }
        }
    }

    public static void onAppProcessStart(IBinder service, boolean requestedStartSystemServer) {
        try {
            if (!inited) commonInit();
            mainZygote = requestedStartSystemServer;

            Dreamland.isSystem = false;

            if (service == null) {
                // We only expect that for pre oreo.
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
            hookPackageLoad(dm);
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
