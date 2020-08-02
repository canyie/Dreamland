package mirror.android.app;

import top.canyie.dreamland.utils.reflect.Reflection;

/**
 * Mirror class of android.app.ActivityThread
 * @author canyie
 */
@SuppressWarnings({"unused"})
public final class ActivityThread {
    public static final String NAME = "android.app.ActivityThread";
    public static final Reflection<?> REF = Reflection.on(NAME);

    public static final Reflection.MethodWrapper currentActivityThread = REF.method("currentActivityThread");
    public static final Reflection.MethodWrapper getApplication = REF.method("getApplication");

    private ActivityThread() {
        throw new InstantiationError("Mirror class " + NAME);
    }

    /**
     * Mirror class of android.app.ActivityThread.AppBindData
     */
    public static final class AppBindData {
        public static final String NAME = "android.app.ActivityThread$AppBindData";
        public static final Reflection REF = Reflection.on(NAME);

        public static final Reflection.FieldWrapper processName = REF.field("processName");
        public static final Reflection.FieldWrapper appInfo = REF.field("appInfo");

        private AppBindData() {
            throw new InstantiationError("Mirror class " + NAME);
        }
    }
}
