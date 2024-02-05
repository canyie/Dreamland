package top.canyie.dreamland;

import android.app.LoadedApk;
import android.content.pm.ApplicationInfo;
import android.os.RemoteException;
import android.util.Log;

import java.lang.reflect.Method;

import top.canyie.dreamland.core.Dreamland;
import top.canyie.dreamland.ipc.IDreamlandManager;
import top.canyie.dreamland.ipc.ModuleInfo;
import top.canyie.dreamland.utils.reflect.Reflection;
import top.canyie.pine.Pine;
import top.canyie.pine.callback.MethodHook;

/**
 * @author canyie
 */
class GetClassLoaderHook extends MethodHook {
    private static final Method getClassLoader = Reflection.getMethod(LoadedApk.class, "getClassLoader");
    private IDreamlandManager dm;
    private LoadedApk loadedApk;
    private String packageName;
    private String processName;
    private ApplicationInfo appInfo;
    private boolean isFirstApp;
    private ModuleInfo[] modules;
    private MethodHook.Unhook unhook;

    private GetClassLoaderHook() {
    }

    public static void install(IDreamlandManager dm, LoadedApk loadedApk, String packageName,
                               String processName, ApplicationInfo appInfo, boolean isFirstApp) {
        ModuleInfo[] modules;

        try {
            modules = dm.getEnabledModulesFor(packageName);
        } catch (RemoteException e) {
            Log.e(Dreamland.TAG, "Failure from remote dreamland service", e);
            return;
        }

        if (modules == null || modules.length == 0) {
            Log.i(Dreamland.TAG, "No module needs to hook into package " + packageName);
            return;
        }

        GetClassLoaderHook hook = new GetClassLoaderHook();
        hook.dm = dm;
        hook.loadedApk = loadedApk;
        hook.packageName = packageName;
        hook.processName = processName;
        hook.appInfo = appInfo;
        hook.isFirstApp = isFirstApp;
        hook.modules = modules;
        try {
            hook.unhook = Pine.hook(getClassLoader, hook);
        } catch (Throwable e) {
            Log.e(Dreamland.TAG, "hook getClassLoader failed", e);
        }
    }

    @Override public void afterCall(Pine.CallFrame callFrame) {
        LoadedApk loadedApk = (LoadedApk) callFrame.thisObject;
        if (loadedApk != this.loadedApk) return;

        try {
            ClassLoader classLoader = (ClassLoader) callFrame.getResult();
            Dreamland.packageReady(dm, packageName, processName, appInfo, classLoader, isFirstApp, Main.mainZygote, modules);
        } finally {
            unhook.unhook();
        }
    }
}
