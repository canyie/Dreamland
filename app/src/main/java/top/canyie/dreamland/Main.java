package top.canyie.dreamland;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Binder;
import android.os.IBinder;
import android.os.Process;
import android.util.Log;
import android.util.SparseBooleanArray;

import androidx.annotation.Keep;

import top.canyie.dreamland.core.Dreamland;
import top.canyie.dreamland.ipc.BinderServiceProxy;
import top.canyie.dreamland.ipc.IDreamlandManager;
import top.canyie.dreamland.ipc.DreamlandManagerService;
import top.canyie.dreamland.utils.reflect.Reflection;

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

    public static int init() {
        try {
            ClassLoader classLoader = Main.class.getClassLoader();
            final String[] preloadClasses = {
                    "mirror.android.app.ActivityThread",
                    "mirror.android.app.ActivityThread$AppBindData",
                    "mirror.android.os.ServiceManager",
                    "top.canyie.dreamland.core.Dreamland",
                    "top.canyie.dreamland.ipc.IDreamlandManager$Stub",
                    "top.canyie.dreamland.ipc.IDreamlandManager$Proxy"
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

            return 0;
        } catch (Throwable e) {
            try {
                Log.e(TAG, "Dreamland init error", e);
            } catch (Throwable ignored) {
            }
        }
        return 1;
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

            Pine.hook(ActivityThread.REF.unwrap().getDeclaredMethod("systemMain"), new MethodHook() {
                @Override public void afterCall(Pine.CallFrame callFrame) throws Throwable {
                    Object activityThread = callFrame.getResult();
                    Context context = ActivityThread.REF.method("getSystemContext").call(activityThread);
                    Log.i(TAG, "System Context = " + context);
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
                                        Dreamland.loadXposedModules(modules);
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
                Log.e(TAG, "Couldn't find clipboard service", e);
                return;
            }

            if (clipboard == null) {
                // Isolated process or google gril service process is not allowed to access clipboard serice
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

            Pine.hook(ActivityThread.REF.unwrap().getDeclaredMethod("handleBindApplication",
                    ActivityThread.AppBindData.REF.unwrap()),
                    new MethodHook() {
                        @Override public void beforeCall(Pine.CallFrame callFrame) {
                            Object appBindData = callFrame.args[0];
                            Dreamland.processName = ActivityThread.AppBindData.processName.getValue(appBindData);
                            ApplicationInfo appInfo = ActivityThread.AppBindData.appInfo.getValue(appBindData);
                            Dreamland.appInfo = appInfo;
                            Dreamland.packageName = appInfo.packageName;
                        }
                    });

            Pine.hook(Class.forName("android.app.LoadedApk").getDeclaredMethod("getClassLoader"),
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
                    });
        } catch (Throwable e) {
            try {
                Log.e(TAG, "Dreamland error in app process", e);
            } catch (Throwable ignored) {
            }
        }
    }


}