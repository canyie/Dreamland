package com.canyie.dreamland.utils.reflect;

/**
 * Created by canyie on 2019/10/24.
 */
public final class UncheckedInvocationTargetException extends ReflectiveException {
    public UncheckedInvocationTargetException() {
    }

    public UncheckedInvocationTargetException(String message) {
        super(message);
    }

    public UncheckedInvocationTargetException(Throwable cause) {
        super(cause);
    }

    public UncheckedInvocationTargetException(String message, Throwable cause) {
        super(message, cause);
    }
}
