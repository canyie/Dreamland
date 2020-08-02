package top.canyie.dreamland.utils;

import android.text.TextUtils;

/**
 * Created by canyie on 2019/10/24.
 */
public final class Preconditions {
    private Preconditions() {}

    public static <T> T checkNotNull(T value) {
        if(value == null) {
            throw new NullPointerException();
        }
        return value;
    }

    public static <T> T checkNotNull(T value, String errorMsg) {
        if(value == null) {
            throw new NullPointerException(errorMsg);
        }
        return value;
    }

    public static void checkArgument(boolean valid) {
        if(!valid) {
            throw new IllegalArgumentException();
        }
    }

    public static void checkArgument(boolean valid,String errorMsg) {
        if(!valid) {
            throw new IllegalArgumentException(errorMsg);
        }
    }

    public static void checkState(boolean valid) {
        if(!valid) {
            throw new IllegalStateException();
        }
    }

    public static void checkState(boolean valid,String errorMsg) {
        if(!valid) {
            throw new IllegalStateException(errorMsg);
        }
    }

    public static <T extends CharSequence> T checkNotEmpty(T value) {
        if(TextUtils.isEmpty(value)) {
            throw new IllegalArgumentException();
        }
        return value;
    }

    public static <T extends CharSequence> T checkNotEmpty(T value, String errorMsg) {
        if(TextUtils.isEmpty(value)) {
            throw new IllegalArgumentException(errorMsg);
        }
        return value;
    }
}