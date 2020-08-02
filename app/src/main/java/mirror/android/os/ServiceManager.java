package mirror.android.os;

import top.canyie.dreamland.utils.reflect.Reflection;

/**
 * Mirror class of android.os.ServiceManager
 * @author canyie
 */
public final class ServiceManager {
    public static final String NAME = "android.os.ServiceManager";
    public static final Reflection<?> REF = Reflection.on(NAME);

    public static final Reflection.MethodWrapper getIServiceManager = REF.method("getIServiceManager");
    public static final Reflection.MethodWrapper getService = REF.method("getService", String.class);
    public static final Reflection.FieldWrapper sServiceManager = REF.field("sServiceManager");

    private ServiceManager() {
        throw new InstantiationError("Mirror class " + NAME);
    }
}
