package top.canyie.dreamland;

import java.lang.reflect.Modifier;

import top.canyie.dreamland.utils.reflect.Reflection;

/**
 * @author canyie
 */
public final class RuntimeUtils {
    private static final Reflection.FieldWrapper superclass = Reflection.field(Class.class, "superClass");
    private static final Reflection.FieldWrapper accessFlags = Reflection.field(Class.class, "accessFlags");
    private RuntimeUtils() {}

    public static void makeExtendable(Class<?> c) {
        int modifiers = c.getModifiers();
        if (Modifier.isFinal(modifiers) || !Modifier.isPublic(modifiers)) {
            int flags = accessFlags.getValue(c);
            flags &= ~Modifier.FINAL;
            flags &= ~(Modifier.PRIVATE | Modifier.PROTECTED);
            flags |= Modifier.PUBLIC;
            accessFlags.setValue(c, flags);
        }
    }

    public static void setSuperclass(Class<?> target, Class<?> newSuperclass) {
        makeExtendable(newSuperclass);
        superclass.setValue(target, newSuperclass);
    }
}
