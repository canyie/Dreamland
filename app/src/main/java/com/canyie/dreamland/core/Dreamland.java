package com.canyie.dreamland.core;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.ContextWrapper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.canyie.dreamland.BuildConfig;
import com.canyie.dreamland.utils.IOUtils;
import com.canyie.dreamland.utils.Preconditions;
import com.swift.sandhook.SandHook;
import com.swift.sandhook.annotation.HookMode;
import com.swift.sandhook.xposedcompat.XposedCompat;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import dalvik.system.PathClassLoader;
import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Created by canyie on 2019/11/12.
 */
public final class Dreamland {
    public static final String TAG = "Dreamland";
    public static final int VERSION = BuildConfig.VERSION_CODE;
    private static final String MANAGER_PACKAGE_NAME = "com.canyie.dreamland.manager";
    static final File BASE_DIR = new File("/data/dreamland/");

    public static String processName = "";
    public static String packageName = "";
    public static boolean isSystem;
    private ModuleManager mModuleManager;
    private AppManager mAppManager;
    private ClassLoader classLoader;
    private Context context;
    private boolean hooked;

    @SuppressLint("StaticFieldLeak") private static final Dreamland INSTANCE = new Dreamland();

    private Dreamland() {
    }

    public static Dreamland getInstance() {
        return INSTANCE;
    }

    public void initProperties() {
        mModuleManager = new ModuleManager();
        mModuleManager.startLoad();
        mAppManager = new AppManager();
        mAppManager.startLoad();
    }

    public boolean isEnabled() {
        return mAppManager.isEnabled(packageName);
    }

    public void initClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
        XposedCompat.classLoader = classLoader;
    }

    public void initContext(@NonNull Context context) {
        Preconditions.checkNotNull(context, "context == null");
        if (context instanceof ContextWrapper) {
            Preconditions.checkNotNull(((ContextWrapper) context).getBaseContext(), "context.getBaseContext() == null");
        }
        if (this.context != null) {
            return;
        }
        this.context = context;

        XposedCompat.context = context;
        if (isSystem) {
            // XposedCompat.cacheDir = new File("/data/dreamland/system-cache/");
            // Not support now...
        } else {
            XposedCompat.cacheDir = IOUtils.ensureDirectoryExisting(new File(context.getCacheDir(), "dreamland"));
        }
        // TODO: Reimplement xposed-bridge method generator with java proxy (See https://blog.csdn.net/TuringTechnician/article/details/88925234)
    }

    public void ready() {
        if (canLoadXposedModules()) {
            if (MANAGER_PACKAGE_NAME.equals(packageName)) {
                Log.i(TAG, "This is dreamland manager. hooking manager getVersion()");
                try {
                    // Method getVersionInternal is only called by reflection,
                    // so only the replacement mode is needed.
                    SandHook.setHookMode(HookMode.REPLACE);
                    Method getVersionInternal = Class.forName("com.canyie.dreamland.manager.core.Dreamland", true, classLoader)
                            .getDeclaredMethod("getVersionInternal");
                    getVersionInternal.setAccessible(true);
                    XposedBridge.hookMethod(getVersionInternal, XC_MethodReplacement.returnConstant(Dreamland.VERSION));
                    hooked = true;
                } catch (Throwable e) {
                    // should never happen
                    Log.e(TAG, "Failed to hook manager methods", e);
                }
                return; // Don't load xposed modules in manager process.
            }
            loadXposedModules();
        }
    }

    private boolean canLoadXposedModules() {
        return context != null && classLoader != null && !hooked;
    }

    private void loadXposedModules() {
        Preconditions.checkState(classLoader != null, "No ClassLoader");
        if (!isEnabled()) {
            Log.i(TAG, "Not load xposed modules: dreamland is disabled in the application.");
            return;
        }

        if (hooked) return;
        hooked = true;

        Log.i(TAG, "Loading xposed-style modules for process " + processName);

        XC_LoadPackage.LoadPackageParam param = new XC_LoadPackage.LoadPackageParam(XposedBridge.sLoadedPackageCallbacks);
        param.packageName = packageName;
        param.processName = processName;
        param.classLoader = classLoader;
        param.isFirstApplication = true; // TODO: validate
        if (!isSystem) {
            param.appInfo = context.getApplicationInfo();
        }

        List<ModuleInfo> moduleInfos = mModuleManager.getEnabledModuleList();
        List<String> callbacks = new ArrayList<>();
        ClassLoader baseClassLoader = getClass().getClassLoader();
        for (ModuleInfo module : moduleInfos) {
            Log.i(TAG, "Loading xposed module: " + module.name + " for process " + processName);
            callbacks.clear();
            try {
                module.parseCallbacks(callbacks);
            } catch (IOException e) {
                Log.e(TAG, "Module " + module.name + "  load skipped: parsing failed", e);
            }
            ClassLoader moduleClassLoader = new PathClassLoader(module.path, baseClassLoader);
            for (String callbackClassName : callbacks) {
                Log.d(TAG, "Loading class: " + callbackClassName + " from module " + module.name);
                callHandleLoadPackage(moduleClassLoader, callbackClassName, param);
            }
        }
    }

    private void callHandleLoadPackage(ClassLoader moduleClassLoader, String callbackClassName, XC_LoadPackage.LoadPackageParam param) {
        Class<?> callbackClass;
        try {
            callbackClass = Class.forName(callbackClassName, true, moduleClassLoader);
        } catch (ClassNotFoundException e) {
            Log.e(TAG, "callback class " + callbackClassName + " not found.");
            return;
        }
        if (IXposedHookInitPackageResources.class.isAssignableFrom(callbackClass)) {
            Log.e(TAG, "    This class requires resource-related hooks (not support now), skipping it.");
        }
        if (!IXposedHookLoadPackage.class.isAssignableFrom(callbackClass)) {
            Log.e(TAG, "    This class doesn't implement IXposedHookLoadPackage, skipping it");
            // TODO: support it
            return;
        }
        IXposedHookLoadPackage callback;
        try {
            callback = (IXposedHookLoadPackage) callbackClass.newInstance();
        } catch (Exception e) {
            Log.e(TAG, "Failed to create instance for class " + callbackClassName, e);
            return;
        }

        try {
            Log.i(TAG, "Calling callback.handleLoadPackage for callback " + callback + " (a " + callback.getClass().getName() + ") on process " + param.processName);
            XC_LoadPackage wrapper = new IXposedHookLoadPackage.Wrapper(callback);
            XposedBridge.hookLoadPackage(wrapper);
            wrapper.handleLoadPackage(param.clone());
        } catch (Throwable e) {
            Log.e(TAG, callbackClassName + ".handleLoadPackage() thrown exception", e);
        }
    }
}
