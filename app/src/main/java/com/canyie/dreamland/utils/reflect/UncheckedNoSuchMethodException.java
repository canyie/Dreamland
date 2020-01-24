package com.canyie.dreamland.utils.reflect;

/**
 * Created by canyie on 2019/10/24.
 */
public class UncheckedNoSuchMethodException extends ReflectiveException {
    public UncheckedNoSuchMethodException() {
    }

    public UncheckedNoSuchMethodException(String message) {
        super(message);
    }

    public UncheckedNoSuchMethodException(Throwable cause) {
        super(cause);
    }

    public UncheckedNoSuchMethodException(String message, Throwable cause) {
        super(message, cause);
    }
}
