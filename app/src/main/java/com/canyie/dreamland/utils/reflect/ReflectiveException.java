package com.canyie.dreamland.utils.reflect;

/**
 * Created by canyie on 2019/10/24.
 */
public class ReflectiveException extends RuntimeException {
    public ReflectiveException() {
    }

    public ReflectiveException(String message) {
        super(message);
    }

    public ReflectiveException(Throwable cause) {
        super(cause);
    }

    public ReflectiveException(String message, Throwable cause) {
        super(message, cause);
    }
}
