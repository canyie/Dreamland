package top.canyie.dreamland.utils.reflect;

/**
 * Created by canyie on 2019/10/24.
 */
public class UncheckedNoSuchFieldException extends ReflectiveException {
    public UncheckedNoSuchFieldException() {
    }

    public UncheckedNoSuchFieldException(String message) {
        super(message);
    }

    public UncheckedNoSuchFieldException(Throwable cause) {
        super(cause);
    }

    public UncheckedNoSuchFieldException(String message, Throwable cause) {
        super(message, cause);
    }
}
