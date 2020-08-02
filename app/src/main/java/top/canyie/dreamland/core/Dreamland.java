package top.canyie.dreamland.core;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import top.canyie.dreamland.BuildConfig;
import top.canyie.dreamland.ipc.IDreamlandManager;

import java.io.File;
import java.util.Arrays;
import top.canyie.dreamland.utils.reflect.Reflection;
import top.canyie.pine.xposed.PineXposed;

/**
 * Created by canyie on 2019/11/12.
 */
public final class Dreamland {
    public static final String TAG = "Dreamland";
    public static final int VERSION = BuildConfig.VERSION_CODE;
    public static final String MANAGER_PACKAGE_NAME = "top.canyie.dreamland.manager";
    public static final String OLD_MANAGER_PACKAGE_NAME = "com.canyie.dreamland.manager";
    public static final File BASE_DIR = new File("/data/misc/dreamland/");

    public static String processName = "";
    public static String packageName = "";
    public static ApplicationInfo appInfo;
    public static boolean isSystem;
    public static ClassLoader classLoader;
    private static boolean hooked;

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
            String[] modules;
            try {
                modules = manager.getEnabledModulesFor();
            } catch (RemoteException e) {
                Log.e(TAG, "Failure from remote dreamland service", e);
                return;
            }
            loadXposedModules(modules);
        }
    }

    private static boolean canLoadXposedModules() {
        return classLoader != null && !hooked;
    }

    public static void loadXposedModules(String[] modules) {
        if (modules == null || modules.length == 0) {
            //Log.d(TAG, "No module needed to load, skip.");
            return;
        }
        Log.i(TAG, "Loading xposed-style modules for package " + packageName + " process " + processName);
        for (String module : modules) {
            if (TextUtils.isEmpty(module)) {
                Log.e(TAG, "Module list contains empty, skipping");
                Log.e(TAG, "Module list: " + Arrays.toString(modules));
                continue;
            }
            Log.i(TAG, "Loading xposed module " + module);
            PineXposed.loadModule(new File(module));
        }
        PineXposed.onPackageLoad(packageName, processName, appInfo, true, classLoader);
    }
}
