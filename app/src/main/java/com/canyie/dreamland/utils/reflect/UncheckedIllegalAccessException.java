package com.canyie.dreamland.utils.reflect;

/**
 * Created by canyie on 2019/10/24.
 */
public class UncheckedIllegalAccessException extends ReflectiveException {
    public UncheckedIllegalAccessException() {
    }

    public UncheckedIllegalAccessException(String message) {
        super(message);
    }

    public UncheckedIllegalAccessException(Throwable cause) {
        super(cause);
    }

    public UncheckedIllegalAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
