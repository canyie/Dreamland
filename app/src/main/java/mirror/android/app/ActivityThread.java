package mirror.android.app;

import top.canyie.dreamland.utils.reflect.Reflection;
import top.canyie.dreamland.utils.reflect.Reflection.MethodWrapper;
import top.canyie.dreamland.utils.reflect.Reflection.FieldWrapper;

/**
 * Mirror class of android.app.ActivityThread
 * @author canyie
 */
@SuppressWarnings({"unused"})
public final class ActivityThread {
    public static final String NAME = "android.app.ActivityThread";
    public static final Reflection<?> REF = Reflection.on(NAME);

    //public static final MethodWrapper currentActivityThread = REF.method("currentActivityThread");
    //public static final MethodWrapper getApplication = REF.method("getApplication");
    public static final FieldWrapper mBoundApplication = REF.field("mBoundApplication");

    private ActivityThread() {
        throw new InstantiationError("Mirror class " + NAME);
    }

    /**
     * Mirror class of android.app.ActivityThread.AppBindData
     */
    public static final class AppBindData {
        public static final String NAME = "android.app.ActivityThread$AppBindData";
        public static final Reflection REF = Reflection.on(NAME);

        public static final FieldWrapper appInfo = REF.field("appInfo");
        public static final FieldWrapper processName = REF.field("processName");
        public static final FieldWrapper compatInfo = REF.field("compatInfo");

        private AppBindData() {
            throw new InstantiationError("Mirror class " + NAME);
        }
    }
}
