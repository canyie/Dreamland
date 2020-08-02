package top.canyie.dreamland.utils.reflect;

/**
 * Created by canyie on 2019/10/24.
 */
public class UncheckedClassNotFoundException extends ReflectiveException {
    public UncheckedClassNotFoundException() {
    }

    public UncheckedClassNotFoundException(String message) {
        super(message);
    }

    public UncheckedClassNotFoundException(Throwable cause) {
        super(cause);
    }

    public UncheckedClassNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
