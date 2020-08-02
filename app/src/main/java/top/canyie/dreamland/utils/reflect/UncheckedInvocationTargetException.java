package top.canyie.dreamland.utils.reflect;

import androidx.annotation.NonNull;

import java.lang.reflect.InvocationTargetException;

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

    @NonNull public Throwable getTargetException() {
        InvocationTargetException origin = (InvocationTargetException) getCause();
        assert origin != null;
        return origin.getTargetException();
    }
}
