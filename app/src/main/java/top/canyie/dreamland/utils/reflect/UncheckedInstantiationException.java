package top.canyie.dreamland.utils.reflect;

/**
 * Created by canyie on 2019/10/24.
 */
public final class UncheckedInstantiationException extends ReflectiveException {
    public UncheckedInstantiationException() {
    }

    public UncheckedInstantiationException(String message) {
        super(message);
    }

    public UncheckedInstantiationException(Throwable cause) {
        super(cause);
    }

    public UncheckedInstantiationException(String message, Throwable cause) {
        super(message, cause);
    }
}
