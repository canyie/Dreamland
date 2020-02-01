package com.canyie.dreamland;

import android.util.Log;

import androidx.annotation.Keep;

import com.canyie.dreamland.core.Dreamland;
import com.canyie.dreamland.hookers.ActivityThreadHooker;
import com.canyie.dreamland.hookers.ContextImplHooker;
import com.canyie.dreamland.hookers.LoadedApkHooker;
import com.canyie.dreamland.utils.HiddenApis;
import com.swift.sandhook.SandHook;
import com.swift.sandhook.SandHookConfig;

/**
 * Created by canyie on 2019/11/12.
 */
@SuppressWarnings("unused") @Keep public final class Main {
    public static int init() {
        try {
            SandHookConfig.DEBUG = true;
            SandHook.init();
            return 0;
        } catch (Throwable e) {
            try {
                Log.e("Dreamland", "Dreamland init error", e);
            } catch (Throwable ignored) {
            }
        }
        return 1;
    }


    public static void onSystemServerStart() {
        try {
//            Dreamland.isSystem = true;
//            Dreamland.processName = "android"; // it's actually system_server, but other functions return this as well
//            Dreamland.packageName = "android";
//            SandHook.addHookClass(ActivityThreadHooker.class);
            /*
             * SELinux会阻止我们在system_server里hook，所以暂时不hook
             * type=1400 audit(0.0:5): avc: denied { execmem } for scontext=u:r:system_server:s0 tcontext=u:r:system_server:s0 tclass=process permissive=0
             */
        } catch(Throwable e) {
            try {
                Log.e(Dreamland.TAG, "Dreamland error in system server", e);
            } catch(Throwable ignored) {
            }
        }
    }

    public static void onAppProcessStart() {
        try {
            HiddenApis.exemptAll();
            // TODO: Only white-listed applications are allowed to access the hidden APIs.
            Dreamland.isSystem = false;
            Dreamland.getInstance().initProperties();
            SandHook.addHookClass(ActivityThreadHooker.class, ContextImplHooker.class, LoadedApkHooker.class);
        } catch(Throwable e) {
            try {
                Log.e(Dreamland.TAG, "Dreamland error in app process", e);
            } catch(Throwable ignored) {
            }
        }
    }
}