package mirror.android.app;

import com.canyie.dreamland.utils.reflect.Reflection;

/**
 * Mirror class of {@link android.app.ActivityThread}
 * @author canyie
 * @date 2019/11/24
 */
@SuppressWarnings({"unused", "unchecked"})
public final class ActivityThread {
    public static final String NAME = "android.app.ActivityThread";
    public static final Reflection REF = Reflection.on(NAME);

    public static final Reflection.WrappedMethod currentActivityThread = REF.method("currentActivityThread");
    public static final Reflection.WrappedMethod getApplication = REF.method("getApplication");

    private ActivityThread() {
        throw new InstantiationError("Mirror class android.app.ActivityThread");
    }

    /**
     * Mirror class of {@link android.app.ActivityThread.AppBindData}
     */
    public static final class AppBindData {
        public static final String NAME = "android.app.ActivityThread$AppBindData";
        public static final Reflection REF = Reflection.on(NAME);

        public static final Reflection.WrappedField processName = REF.field("processName");
        public static final Reflection.WrappedField appInfo = REF.field("appInfo");

        private AppBindData() {
            throw new InstantiationError("Mirror class android.app.ActivityThread$AppBindData");
        }
    }
}
