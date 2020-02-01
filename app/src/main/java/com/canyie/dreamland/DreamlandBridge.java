package com.canyie.dreamland;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;

import com.canyie.dreamland.core.Dreamland;
import com.canyie.dreamland.utils.Preconditions;
import com.swift.sandhook.SandHook;
import com.swift.sandhook.wrapper.HookErrorException;
import com.swift.sandhook.wrapper.HookWrapper;

import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Created by canyie on 2019/11/25.
 */
@Keep @SuppressWarnings("unused") public final class DreamlandBridge {
    private DreamlandBridge() {
    }

    public static int getDreamlandVersion() {
        return Dreamland.VERSION;
    }

    public static void addHookClass(@NonNull Class<?>... hookers) throws HookErrorException {
        SandHook.addHookClass(hookers);
    }

    public static void hookMethod(@NonNull HookWrapper.HookEntity entity) throws HookErrorException {
        SandHook.hook(entity);
    }

    public static boolean is64Bit() {
        return SandHook.is64Bit();
    }

    public static boolean compileMethod(@NonNull Member method) {
        int mod = method.getModifiers();
        Preconditions.checkArgument(!Modifier.isAbstract(mod), "Can't compile a abstract method");
        Preconditions.checkArgument(!Modifier.isNative(mod), "Can't compile a native method");
        Preconditions.checkState(!Dreamland.isSystem, "Can't compile a method in system processes");
        return SandHook.compileMethod(method);
    }

    public static boolean decompileMethod(@NonNull Member method, boolean disableJIT) {
        return SandHook.deCompileMethod(method, disableJIT);
    }

    public static boolean disableVMInline() {
        return SandHook.disableVMInline();
    }
}
