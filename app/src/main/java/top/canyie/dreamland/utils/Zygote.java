package top.canyie.dreamland.utils;

import android.annotation.SuppressLint;
import android.util.Log;

import java.lang.reflect.Method;

import top.canyie.dreamland.core.Dreamland;
import top.canyie.dreamland.utils.reflect.Reflection;

/**
 * @author canyie
 */
public final class Zygote {
    private static Method allowFileAcrossFork;
    private Zygote() {}

    @SuppressLint("BlockedPrivateApi") public static void allowFileAcrossFork(String path) {
        try {
            if (allowFileAcrossFork == null) {
                allowFileAcrossFork = Class.forName("com.android.internal.os.Zygote")
                        .getDeclaredMethod("nativeAllowFileAcrossFork", String.class);
                allowFileAcrossFork.setAccessible(true);
            }
            allowFileAcrossFork.invoke(null, path);
        } catch (Throwable e) {
            Log.e(Dreamland.TAG, "Error in nativeAllowFileAcrossFork", e);
        }
    }
}
