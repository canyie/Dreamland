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
import android.os.RemoteException;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseBooleanArray;

import androidx.annotation.Keep;

import de.robv.android.xposed.DexCreator;
import de.robv.android.xposed.XposedBridge;
import top.canyie.dreamland.core.Dreamland;
import top.canyie.dreamland.ipc.BinderServiceProxy;
import top.canyie.dreamland.ipc.IDreamlandManager;
import top.canyie.dreamland.ipc.DreamlandManagerService;
import top.canyie.dreamland.utils.RuntimeUtils;
import top.canyie.dreamland.utils.reflect.Reflection;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;

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
    //private static int sWebViewZygoteUid = -1;
    private static int sTranslationCode = IBinder.LAST_CALL_TRANSACTION;
    private static boolean mainZygote;

    public static int init() {
        try {
            ClassLoader classLoader = Main.class.getClassLoader();

            // Try initialize resources hook first.
            // This needs to be performed as early as possible to avoid trying to load XResources when XResourcesSuperClass is not loaded.
            try {
                initXResources(classLoader);
            } catch (Throwable e) {
                Log.e(TAG, "Resources hook initialize failed", e);
                Dreamland.disableResourcesHook = true;
            }

            final String[] preloadClasses = {
                    "mirror.android.app.ActivityThread",
                    "mirror.android.app.ActivityThread$AppBindData",
                    "mirror.android.os.ServiceManager",
                    "top.canyie.dreamland.core.Dreamland",
                    "top.canyie.dreamland.ipc.IDreamlandManager$Stub",
                    "top.canyie.dreamland.ipc.IDreamlandManager$Stub$Proxy"
            };

            for (String className : preloadClasses) {
                try {
                    Class.forName(className, true, classLoader);
                } catch (Exception e) {
                    Log.e(TAG, "Preload class failed for " + className, e);
                }
            }

            PineConfig.debug = true;
            PineConfig.debuggable = false;
            PineConfig.libLoader = null;
            Pine.ensureInitialized();

            /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try {
                    Field field = Process.class.getDeclaredField("WEBVIEW_ZYGOTE_UID");
                    field.setAccessible(true);
                    sWebViewZygoteUid = field.getInt(null);
                } catch (Exception e) {
                    Log.e(TAG, "Could not get the WebView zygote uid", e);
                    switch (Build.VERSION.SDK_INT) {
                        case Build.VERSION_CODES.Q:
                        case Build.VERSION_CODES.P:
                            sWebViewZygoteUid = 1053;
                            break;
                        case Build.VERSION_CODES.O_MR1:
                        case Build.VERSION_CODES.O:
                            sWebViewZygoteUid = 1051;
                            break;
                    }
                }
            }*/

            sTranslationCode = getAvailableTranslationCode();
            String realApi = SystemProperties.get( "ro.product.cpu.abi", "");
            if (TextUtils.isEmpty(realApi)) {
                Log.e(TAG, "System property 'ro.product.cpu.abi' is missing on the device");
                mainZygote = false;
            } else {
                // For 32-bit process running on 64-bit device, Build.CPU_ABI is 32-bit api
                mainZygote = Build.CPU_ABI.equalsIgnoreCase(realApi);
            }
            return 0;
        } catch (Throwable e) {
            try {
                Log.e(TAG, "Dreamland init error", e);
            } catch (Throwable ignored) {
            }
        }
        return 1;
    }

    private static void initXResources(ClassLoader myCL) throws Exception {
        Resources res = Resources.getSystem();
        String dexForXResources = ensureSuperDexFor("XResources", res.getClass(), Resources.class);

        Class<?> taClass = TypedArray.class;
        try {
            TypedArray ta = res.obtainTypedArray(res.getIdentifier("preloaded_drawables", "array", "android"));
            taClass = ta.getClass();
            ta.recycle();
        } catch (Resources.NotFoundException e) {
            XposedBridge.log(e);
        }

        String dexForXTypedArray = ensureSuperDexFor("XTypedArray", taClass, TypedArray.class);

        // Inject a ClassLoader for the created classes as parent of XposedBridge's ClassLoader.
        RuntimeUtils.injectDex(myCL, dexForXResources + File.pathSeparator + dexForXTypedArray);

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

    private static int getAvailableTranslationCode() throws Exception {
        @SuppressLint("PrivateApi") Class<?> c = Class.forName("android.content.IClipboard$Stub");
        SparseBooleanArray maybeUsing = new SparseBooleanArray();
        for (Field field : c.getDeclaredFields()) {
            if (field.getType() == int.class && Modifier.isStatic(field.getModifiers())) {
                field.setAccessible(true);
                maybeUsing.put(field.getInt(null), false);
            }
        }

        // AIDL transaction code increases monotonically from FIRST_CALL_TRANSACTION
        // Search forward from the LAST_CALL_TRANSACTION
        // (But if LAST_CALL_TRANSACTION is used,
        // remaining call transaction codes are also very likely to have been used.)
        for (int i = IBinder.LAST_CALL_TRANSACTION; i >= IBinder.FIRST_CALL_TRANSACTION; i--) {
            if (maybeUsing.get(i, true))
                return i; // Not found in maybeUsing
        }
        // WTF?!
        throw new RuntimeException("No call transaction code available");
    }

    public static void onSystemServerStart() {
        try {
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
                    Object activityThread = callFrame.getResult();
                    Context context = ActivityThread.REF.method("getSystemContext").call(activityThread);
                    dms.initContext(context);

                    String[] modules = dms.getEnabledModulesFor();
                    if (modules != null && modules.length != 0) {
                        ClassLoader cl = Thread.currentThread().getContextClassLoader();
                        Dreamland.packageName = "android";
                        Dreamland.processName = "android"; // it's actually system_server, but other functions return this as well
                        Dreamland.appInfo = null;
                        Dreamland.classLoader = cl;
                        assert cl != null;
                        Pine.hook(cl.loadClass("com.android.server.SystemServer")
                                        .getDeclaredMethod("startBootstrapServices"),
                                new MethodHook() {
                                    @Override public void beforeCall(Pine.CallFrame callFrame) {
                                        Dreamland.loadXposedModules(modules, true);
                                    }
                                });
                    }
                }
            });

            final int translationCode = sTranslationCode;
            Object base = ServiceManager.getIServiceManager.callStatic();
            ServiceManager.sServiceManager.setStaticValue(Reflection.on("android.os.IServiceManager")
                    .proxy((proxy, method, args) -> {
                        if ("addService".equals(method.getName())) {
                            if (TARGET_BINDER_SERVICE_NAME.equals(args[0])) {
                                Log.i(TAG, "Replacing clipboard service");
                                args[1] = new BinderServiceProxy((Binder) args[1], translationCode,
                                        TARGET_BINDER_SERVICE_DESCRIPTOR, dms);
                                //args[2] = true; // Do not supports isolated processes yet
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

    public static void onAppProcessStart() {
        try {
            /*if (sWebViewZygoteUid == Process.myUid())
                return; // Disable framework for WebView zygote*/

            Dreamland.isSystem = false;

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

            final IDreamlandManager dm;
            try {
                IBinder dmsBinder = BinderServiceProxy.transactRemote(clipboard,
                        TARGET_BINDER_SERVICE_DESCRIPTOR, sTranslationCode);
                dm = IDreamlandManager.Stub.asInterface(dmsBinder);
                if (!dm.isEnabledFor()) return;
            } catch (Exception e) {
                Log.e(TAG, "Couldn't check whether the current process is needs to hook", e);
                return;
            }

            String[] modules;
            try {
                modules = dm.getEnabledModulesFor();
            } catch (RemoteException e) {
                Log.e(TAG, "Failure from remote dreamland service", e);
                return;
            }

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
                                Dreamland.ready(dm);
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

            try {
                Dreamland.startResourcesHook(dm);
            } catch (Throwable e) {
                Log.e(TAG, "Start resources hook failed", e);
                Dreamland.disableResourcesHook = true;
            }

            Dreamland.loadXposedModules(modules, mainZygote);
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