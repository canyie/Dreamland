package com.canyie.dreamland.core;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.ApplicationInfo;
import android.nfc.Tag;
import android.os.Process;
import android.util.Log;

import androidx.annotation.NonNull;

import com.canyie.dreamland.utils.IOUtils;
import com.canyie.dreamland.utils.Preconditions;
import com.swift.sandhook.xposedcompat.XposedCompat;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Created by canyie on 2019/11/12.
 */
public final class Dreamland {
    public static final String TAG = "Dreamland";
    public static final int VERSION = 1;
    public static String processName = "";
    public static String packageName = "";
    public static boolean isSystem;
    private ClassLoader classLoader;
    private Context context;
    private boolean hooked;

    private static final Dreamland INSTANCE = new Dreamland();
    private static XposedBridge.CopyOnWriteSortedSet<XC_LoadPackage> sLoadedPackageCallbacks = new XposedBridge.CopyOnWriteSortedSet<>();
    private Dreamland() {
    }

    public static Dreamland getInstance() {
        return INSTANCE;
    }

    public boolean isEnabled() {
        // TODO: implement this method
        return true;
    }

    public void initClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    public void initContext(@NonNull Context context) {
        Preconditions.checkNotNull(context, "context == null");
        if(context instanceof ContextWrapper) {
            Preconditions.checkNotNull(((ContextWrapper) context).getBaseContext(), "context.getBaseContext() == null");
        }
        if (this.context != null) {
            return;
        }
        this.context = context;

        XposedCompat.context = context;
        if(isSystem) {
            XposedCompat.cacheDir = new File("/data/dreamland/system-cache/");
        } else {
            XposedCompat.cacheDir = IOUtils.ensureDirectoryExisting(new File(context.getCacheDir(), "dreamland"));
        }
        // TODO: Reimplement xposed-bridge method generator with java proxy (See https://blog.csdn.net/TuringTechnician/article/details/88925234)
    }

    public boolean canLoadXposedModules() {
        return context != null && classLoader != null && !hooked;
    }

    public void loadXposedModules() {
        Preconditions.checkState(classLoader != null, "No ClassLoader");
        if(!isEnabled()) {
            Log.i(TAG, "Dreamland is disabled.");
            return;
        }

        if(hooked) return;
        hooked = true;

        Log.i(TAG, "Loading xposed-style modules for process " + processName);

        XC_LoadPackage.LoadPackageParam param = new XC_LoadPackage.LoadPackageParam(XposedBridge.sLoadedPackageCallbacks);
        param.packageName = packageName;
        param.processName = processName;
        param.classLoader = classLoader;
        param.isFirstApplication = true;
        if (!isSystem) {
            param.appInfo = context.getApplicationInfo();
        }

        List<ModuleInfo> moduleInfos = getEnabledModules();
        List<String> callbacks = new ArrayList<>();
        for(ModuleInfo module : moduleInfos) {
            Log.i(TAG, "Loading xposed module: " + module.name + " for process " + processName);
            callbacks.clear();
            try {
                module.parseCallbacks(callbacks);
            } catch (IOException e) {
                Log.e(TAG, "Module " + module.name + "  load skipped: parsing failed", e);
            }
            ClassLoader moduleClassLoader = module.getClassLoader();
            for(String callbackClassName : callbacks) {
                Log.d(TAG, "Loading class: " + callbackClassName + " from module " + module.name);
                Class<?> callbackClass;
                try {
                    callbackClass = Class.forName(callbackClassName, true, moduleClassLoader);
                } catch (ClassNotFoundException e) {
                    Log.e(TAG, "callback class " + callbackClassName + " not found.");
                    continue;
                }
                if(IXposedHookLoadPackage.class.isAssignableFrom(callbackClass)) {
                    Log.i(TAG, "     This class is IXposedHookLoadPackage.");
                }
                if(IXposedHookInitPackageResources.class.isAssignableFrom(callbackClass)) {
                    Log.e(TAG, "    This class requires resource-related hooks (not support now), skipping it.");
                } else if(!IXposedHookLoadPackage.class.isAssignableFrom(callbackClass)) {
                    Log.e(TAG, "    This class doesn't implement IXposedHookLoadPackage, skipping it");
                    // TODO: support it
                    continue;
                }
                Object callback;
                try {
                    callback = callbackClass.newInstance();
                } catch(Exception e) {
                    Log.e(TAG, "Failed to create instance for class " + callbackClassName, e);
                    continue;
                }
                if(callback instanceof IXposedHookLoadPackage) {
                    try {
                        callHandleLoadPackage((IXposedHookLoadPackage) callback, param);
                    } catch(Throwable e) {
                        Log.e(TAG, callbackClassName + ".handleLoadPackage() thrown exception", e);
                    }
                }
            }
        }
    }

    private void callHandleLoadPackage(IXposedHookLoadPackage callback, XC_LoadPackage.LoadPackageParam param) throws Throwable {
        Log.i(TAG, "Calling callback.handleLoadPackage for callback " + callback + " (a " + callback.getClass().getName() + ") on process " + param.processName);
        XC_LoadPackage wrapper = new IXposedHookLoadPackage.Wrapper(callback);
        XposedBridge.hookLoadPackage(wrapper);
        wrapper.handleLoadPackage(param.clone());
    }

    @NonNull public List<ModuleInfo> getEnabledModules() {
        /*ModuleInfo gravityBox = new ModuleInfo();
        gravityBox.name = "重力工具箱";
        gravityBox.path = "/data/app/com.ceco.nougat.gravitybox-1/base.apk";
        gravityBox.nativeLibraryDir = "";
        // Gravity Box is not support now.

        ModuleInfo colorQQ = new ModuleInfo();
        colorQQ.name = "ColorQQ";
        colorQQ.path = "/data/app/me.qiwu.colorqq-1/base.apk";
        // colorQQ.nativeLibraryDir = "/data/user/0/me.qiwu.colorqq/lib/";
        // Oh, QQ and WeChat is not working on Android Virtual Device... so we can't test it.
        */
        // TODO: implement this method

        ModuleInfo fullFake = new ModuleInfo();
        fullFake.name = "FullFake";
        fullFake.path = "/data/app/com.canyie.fullfake-1/base.apk";
        return Arrays.asList(fullFake);
    }
}
