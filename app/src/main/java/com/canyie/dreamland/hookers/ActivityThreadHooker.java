package com.canyie.dreamland.hookers;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.nfc.Tag;
import android.util.Log;

import com.canyie.dreamland.core.Dreamland;
import com.swift.sandhook.SandHook;
import com.swift.sandhook.annotation.HookClass;
import com.swift.sandhook.annotation.HookMethod;
import com.swift.sandhook.annotation.HookMethodBackup;
import com.swift.sandhook.annotation.HookReflectClass;
import com.swift.sandhook.annotation.MethodParams;
import com.swift.sandhook.annotation.MethodReflectParams;
import com.swift.sandhook.annotation.ThisObject;

import java.lang.reflect.Method;

import mirror.android.app.ActivityThread;

/**
 * Created by canyie on 2019/11/12.
 */
@SuppressWarnings("unused")
@HookReflectClass(ActivityThread.NAME)
public final class ActivityThreadHooker {
    /*@HookMethodBackup("systemMain")
    @MethodParams({})
    private static Method systemMainBackup;*/

    @HookMethodBackup("handleBindApplication")
    @MethodReflectParams("android.app.ActivityThread$AppBindData")
    private static Method handleBindApplicationBackup;

    /*@HookMethod("systemMain")
    @MethodParams({})
    public static Object systemMain() throws Throwable {
        Object result = SandHook.callOriginByBackup(systemMainBackup, null);
        try {
            Object mainThread;
            Log.i(Dreamland.TAG, "Loading xposed modules in system_server");
            if (ActivityThread.REF.isInstance(result)) {
                mainThread = result;
            } else {
                Log.e(Dreamland.TAG, "ActivityThread.systemMain() returned " + result + " ; try currentActivityThread()");
                mainThread = ActivityThread.currentActivityThread.callStatic();
            }
            Context context = ActivityThread.getApplication.call(mainThread);
            Dreamland d = Dreamland.getInstance();
            d.initContext(context);
            d.initClassLoader(Dreamland.class.getClassLoader());
            d.loadXposedModules();
        } catch (Throwable e) {
            try {
                Log.e(Dreamland.TAG, "Can't init app class loader.", e);
            } catch (Throwable ignored) {
            }
        }
        return result;
    }*/

    @HookMethod("handleBindApplication")
    @MethodReflectParams("android.app.ActivityThread$AppBindData")
    public static void handleBindApplication(@ThisObject Object activityThread, Object appBindData) throws Throwable {
        try {
            if (!Dreamland.isSystem) {
                Dreamland.processName = ActivityThread.AppBindData.processName.getValue(appBindData);
                ApplicationInfo appInfo = ActivityThread.AppBindData.appInfo.getValue(appBindData);
                Dreamland.packageName = appInfo.packageName;
            }
        } catch(Throwable e) {
            try {
                Log.e(Dreamland.TAG, "Failed to init app infos", e);
            } catch (Throwable ignored) {
            }
        }
        SandHook.callOriginByBackup(handleBindApplicationBackup, activityThread, appBindData);
    }
}
