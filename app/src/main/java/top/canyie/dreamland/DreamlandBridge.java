package top.canyie.dreamland;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;

import top.canyie.dreamland.core.Dreamland;

import java.lang.reflect.Member;

import top.canyie.pine.Pine;

/**
 * Created by canyie on 2019/11/25.
 */
@Keep @SuppressWarnings("unused") public final class DreamlandBridge {
    private DreamlandBridge() {
    }

    public static int getDreamlandVersion() {
        return Dreamland.VERSION;
    }

    public static boolean compileMethod(@NonNull Member method) {
        return Pine.compile(method);
    }

    public static boolean decompileMethod(@NonNull Member method, boolean disableJit) {
        return Pine.decompile(method, disableJit);
    }

    public static boolean disableJitInline() {
        return Pine.disableJitInline();
    }
}
