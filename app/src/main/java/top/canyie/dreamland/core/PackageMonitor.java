package top.canyie.dreamland.core;

import android.annotation.SuppressLint;
import android.app.ActivityThread;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserHandleHidden;
import android.util.Log;
import java.lang.reflect.Method;

import top.canyie.dreamland.ipc.DreamlandManagerService;
import top.canyie.dreamland.utils.BuildUtils;

/**
 * @author canyie
 */
public class PackageMonitor extends BroadcastReceiver {
    PackageMonitor() {
    }

    public static void startRegister() {
        try {
            ActivityThread activityThread = ActivityThread.currentActivityThread();
            Context context = mirror.android.app.ActivityThread.REF.method("getSystemContext").call(activityThread);
            if (context == null) {
                Log.e(Dreamland.TAG, "No context for register package monitor");
                return;
            }
            Handler h = new Handler(Looper.getMainLooper());
            h.post(() -> new T(context).start());
        } catch (Throwable e) {
            Log.e(Dreamland.TAG, "Cannot schedule register action", e);
        }
    }

    @Override public void onReceive(Context context, Intent intent) {
        String packageName = intent.getData().getSchemeSpecificPart();
        String action = intent.getAction();
        assert action != null;
        Log.i(Dreamland.TAG, "Received " + action + " for package " + packageName);
        DreamlandManagerService dm = DreamlandManagerService.getInstance();
        ModuleManager moduleManager = dm.getModuleManager();
        switch (action) {
            case Intent.ACTION_PACKAGE_REPLACED:
                if (!moduleManager.isModuleEnabled(packageName)) {
                    // Not a module, or disabled. For disabled module, apk path will be updated in enable()
                    return;
                }

                ApplicationInfo appInfo;
                String modulePath;
                try {
                    appInfo = context.getPackageManager().getApplicationInfo(packageName, 0);
                    modulePath = dm.getModulePath(appInfo);
                } catch (PackageManager.NameNotFoundException|RemoteException e) {
                    Log.e(Dreamland.TAG, "getModulePath", e);
                    return;
                }
                if (modulePath == null) {
                    Log.e(Dreamland.TAG, "No valid apk found for module " + packageName);
                    return;
                }
                moduleManager.updateModulePath(packageName, modulePath, appInfo.nativeLibraryDir);
                dm.clearModuleCache();
                Log.i(Dreamland.TAG, "Updated module info for " + packageName);
                break;
            case Intent.ACTION_PACKAGE_FULLY_REMOVED:
                if (moduleManager.remove(packageName)) {
                    dm.clearModuleCache();
                    Log.i(Dreamland.TAG, "Module " + packageName + " has been removed, clean up.");
                }
                break;
        }
    }

    static final class T extends Thread {
        private Context context;

        T (Context context) {
            super("Dreamland-PackageMonitor");
            this.context = context;
            setDaemon(true);
        }

        @Override public void run() {
            Looper.prepare();
            try {
                PackageMonitor monitor = new PackageMonitor();
                Handler h = new Handler(Looper.myLooper());

                IntentFilter intentFilter = new IntentFilter();
                intentFilter.addAction(Intent.ACTION_PACKAGE_REPLACED);
                intentFilter.addAction(Intent.ACTION_PACKAGE_FULLY_REMOVED);
                intentFilter.addDataScheme("package");

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Method registerReceiverAsUser = Context.class.getDeclaredMethod("registerReceiverAsUser",
                            BroadcastReceiver.class, UserHandle.class, IntentFilter.class,
                            String.class, Handler.class, int.class);
                    registerReceiverAsUser.setAccessible(true);
                    registerReceiverAsUser.invoke(context, monitor, UserHandleHidden.ALL,
                            intentFilter, null, h, Context.RECEIVER_NOT_EXPORTED);
                } else {
                    @SuppressLint("DiscouragedPrivateApi")
                    Method registerReceiverAsUser = Context.class.getDeclaredMethod("registerReceiverAsUser",
                            BroadcastReceiver.class, UserHandle.class, IntentFilter.class,
                            String.class, Handler.class);
                    registerReceiverAsUser.setAccessible(true);
                    registerReceiverAsUser.invoke(context, monitor, UserHandleHidden.ALL,
                            intentFilter, null, h);
                }

                context = null;
                Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
            } catch (Throwable e) {
                Log.e(Dreamland.TAG, "Cannot register package monitor", e);
                return;
            }
            Looper.loop();
        }
    }
}
