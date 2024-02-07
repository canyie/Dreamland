package top.canyie.dreamland;

import android.app.LoadedApk;
import android.content.pm.ApplicationInfo;
import android.os.RemoteException;
import android.util.Log;

import java.lang.reflect.Method;
import java.util.List;

import de.robv.android.xposed.XposedHelpers;
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
    private static final Method target;
    private static final boolean hookGet;
    private IDreamlandManager dm;
    private LoadedApk loadedApk;
    private String packageName;
    private String processName;
    private ApplicationInfo appInfo;
    private boolean isFirstApp;
    private ModuleInfo[] modules;
    private MethodHook.Unhook unhook;

    static {
        Method create = Reflection.findMethod(LoadedApk.class, "createOrUpdateClassLoaderLocked", List.class);
        hookGet = create == null;
        target = hookGet ? Reflection.getMethod(LoadedApk.class, "getClassLoader") : create;
    }

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
            hook.unhook = Pine.hook(target, hook);
        } catch (Throwable e) {
            Log.e(Dreamland.TAG, "hook getClassLoader failed", e);
        }
    }

    @Override public void afterCall(Pine.CallFrame callFrame) {
        LoadedApk loadedApk = (LoadedApk) callFrame.thisObject;
        if (loadedApk != this.loadedApk) return;

        try {
            ClassLoader classLoader = (ClassLoader) (hookGet
                    ? callFrame.getResult()
                    : XposedHelpers.getObjectField(loadedApk, "mClassLoader"));
            Dreamland.packageReady(dm, packageName, processName, appInfo, classLoader, isFirstApp, Main.mainZygote, modules);
        } finally {
            unhook.unhook();
        }
    }
}
