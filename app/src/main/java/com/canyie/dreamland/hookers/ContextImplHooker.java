package com.canyie.dreamland.hookers;

import android.content.Context;
import android.util.Log;

import com.canyie.dreamland.core.Dreamland;
import com.swift.sandhook.SandHook;
import com.swift.sandhook.annotation.HookMethod;
import com.swift.sandhook.annotation.HookMethodBackup;
import com.swift.sandhook.annotation.HookReflectClass;
import com.swift.sandhook.annotation.MethodReflectParams;
import com.swift.sandhook.annotation.Param;

import java.lang.reflect.Method;

/**
 * Created by canyie on 2019/11/28.
 */
@SuppressWarnings("unused")
@HookReflectClass("android.app.ContextImpl")
public final class ContextImplHooker {
    private static boolean contextReady;
    @HookMethodBackup("createAppContext")
    @MethodReflectParams({"android.app.ActivityThread", "android.app.LoadedApk"})
    private static Method createAppContextBackup;

    @HookMethod("createAppContext")
    @MethodReflectParams({"android.app.ActivityThread", "android.app.LoadedApk"})
    public static Object createAppContext(Object mainThread, Object packageInfo) throws Throwable {
        Object result = SandHook.callOriginByBackup(createAppContextBackup, null, mainThread, packageInfo);
        try {
            if (!contextReady) {
                if (result instanceof Context) {
                    Dreamland d = Dreamland.getInstance();
                    d.initContext((Context) result);
                    contextReady = true;
                    d.ready();
                } else {
                    Log.e(Dreamland.TAG, "ContextImpl.createAppContext() returned " + result);
                }
            }
        } catch(Throwable e) {
            try {
                Log.e(Dreamland.TAG, "Can't init app context", e);
            } catch (Throwable ignored) {
            }
        }
        return result;
    }
}
