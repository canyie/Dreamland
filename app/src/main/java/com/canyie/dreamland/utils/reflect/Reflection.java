package com.canyie.dreamland.utils.reflect;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Field;
import java.lang.reflect.Constructor;

import com.canyie.dreamland.utils.Preconditions;

/**
 * Created by canyie on 2019/10/24.
 */
@SuppressWarnings({"unchecked", "unused"})
public final class Reflection<T> {
    @NonNull
    private Class<T> klass;

    private Reflection(@NonNull Class<T> klass) {
        this.klass = klass;
    }

    public static Reflection on(@NonNull String name) {
        return new Reflection(findClass(name));
    }

    public static Reflection on(@NonNull String name, @NonNull ClassLoader loader) {
        return new Reflection(findClass(name, loader));
    }

    public static Reflection on(@NonNull Class<?> klass) {
        Preconditions.checkNotNull(klass, "klass == null");
        return new Reflection(klass);
    }

    @NonNull
    public Class<?> unwrap() {
        return klass;
    }

    public static WrappedMethod wrap(@NonNull Method method) {
        Preconditions.checkNotNull(method, "method == null");
        return new WrappedMethod(method);
    }

    public WrappedMethod method(@NonNull String name, @NonNull Class<?>... parameterTypes) {
        return method(klass, name, parameterTypes);
    }

    public static WrappedMethod method(@NonNull Class<?> klass, @NonNull String name, @Nullable Class<?>... parameterTypes) {
        return wrap(getMethod(klass, name, parameterTypes));
    }

    public static WrappedField wrap(@NonNull Field field) {
        Preconditions.checkNotNull(field, "field == null");
        return new WrappedField(field);
    }

    public WrappedField field(@NonNull String name) {
        return field(klass, name);
    }

    public static WrappedField field(@NonNull Class<?> klass, @NonNull String name) {
        return wrap(getField(klass, name));
    }

    public static <T> WrappedConstructor<T> wrap(@NonNull Constructor<T> constructor) {
        Preconditions.checkNotNull(constructor, "constructor == null");
        return new WrappedConstructor<>(constructor);
    }

    public WrappedConstructor<T> constructor(@Nullable Class<?>... parameterTypes) {
        return wrap(getConstructor(klass, parameterTypes));
    }

    @Nullable
    public static Class<?> findClassOrNull(@NonNull String name) {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException ignored) {
            return null;
        }
    }

    @Nullable
    public static Class<?> findClassOrNull(@NonNull String name, @Nullable ClassLoader loader) {
        try {
            return Class.forName(name, true, loader);
        } catch (ClassNotFoundException ignored) {
            return null;
        }
    }

    @NonNull
    public static Class<?> findClass(@NonNull String name) {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException e) {
            throw new UncheckedClassNotFoundException("Class " + name + " not found", e);
        }
    }

    @NonNull
    public static Class<?> findClass(@NonNull String name, @Nullable ClassLoader loader) {
        try {
            return Class.forName(name, true, loader);
        } catch (ClassNotFoundException e) {
            throw new UncheckedClassNotFoundException("No class " + name + " found in classloader " + loader, e);
        }
    }

    @NonNull
    public static Method getMethod(@NonNull Class<?> klass, @NonNull String name, @Nullable Class<?>... parameterTypes) {
        Method method = findMethod(klass, name, parameterTypes);
        if (method == null) {
            throw new UncheckedNoSuchMethodException("No method '" + name + getParameterTypesMessage(parameterTypes) + "' found in class " + klass.getName());
        }
        return method;
    }

    private static String getParameterTypesMessage(@Nullable Class<?>[] parameterTypes) {
        if (parameterTypes == null || parameterTypes.length == 0) {
            return "()";
        }
        StringBuilder sb = new StringBuilder("(");
        boolean isFirst = true;
        for (Class<?> type : parameterTypes) {
            if (isFirst) {
                isFirst = false;
            } else {
                sb.append(",");
            }
            sb.append(type.getName());
        }
        return sb.append(')').toString();
    }

    @Nullable
    public static Method findMethod(@NonNull Class<?> klass, @NonNull String name, @Nullable Class<?>... parameterTypes) {
        checkForFindMethod(klass, name, parameterTypes);
        return findMethodNoChecks(klass, name, parameterTypes);
    }

    @Nullable
    public static Method findMethodNoChecks(Class<?> klass, String name, Class<?>... parameterTypes) {
        while (klass != null) {
            try {
                Method method = klass.getDeclaredMethod(name, parameterTypes);
                method.setAccessible(true);
                return method;
            } catch (NoSuchMethodException ignored) {
            }
            klass = klass.getSuperclass();
        }
        return null;
    }

    private static void checkForFindMethod(Class<?> klass, String name, Class<?>... parameterTypes) {
        Preconditions.checkNotNull(klass, "klass == null");
        Preconditions.checkNotEmpty(name, "name is null or empty");
        Preconditions.checkNotNull(parameterTypes, "parameterTypes == null");

        for (int i = 0; i < parameterTypes.length; i++) {
            if (parameterTypes[i] == null) {
                throw new NullPointerException("parameterTypes[" + i + "] == null");
            }
        }
    }

    @NonNull
    public static Field getField(@NonNull Class<?> klass, @NonNull String name) {
        Field field = findField(klass, name);
        if (field == null) {
            throw new UncheckedNoSuchFieldException("No field '" + name + "' found in class " + klass.getName());
        }
        return field;
    }

    @Nullable
    public static Field findField(Class<?> klass, String name) {
        checkForFindField(klass, name);
        return findFieldNoChecks(klass, name);
    }

    @Nullable
    public static Field findFieldNoChecks(@NonNull Class<?> klass, @NonNull String name) {
        while (klass != null) {
            try {
                Field field = klass.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException ignored) {
            }
            klass = klass.getSuperclass();
        }
        return null;
    }

    private static void checkForFindField(Class<?> klass, String name) {
        Preconditions.checkNotNull(klass, "klass == null");
        Preconditions.checkNotEmpty(name, "name is null or empty");
    }

    @NonNull
    public static <T> Constructor<T> getConstructor(@NonNull Class<T> klass, @Nullable Class<?>... parameterTypes) {
        Constructor<T> c = findConstructor(klass, parameterTypes);
        if (c == null) {
            throw new UncheckedNoSuchMethodException("No constructor '" + klass.getName() + getParameterTypesMessage(parameterTypes) + "' found in class " + klass.getName());
        }
        return c;
    }

    @Nullable
    public static <T> Constructor<T> findConstructor(@NonNull Class<T> klass, @Nullable Class<?>... parameterTypes) {
        checkForFindConstructor(klass, parameterTypes);
        return findConstructorNoChecks(klass, parameterTypes);
    }

    @Nullable
    public static <T> Constructor<T> findConstructorNoChecks(@NonNull Class<T> klass, @Nullable Class<?>... parameterTypes) {
        try {
            Constructor<T> constructor = klass.getDeclaredConstructor(parameterTypes);
            constructor.setAccessible(true);
            return constructor;
        } catch (NoSuchMethodException ignored) {
        }
        return null;
    }

    private static void checkForFindConstructor(Class<?> klass, Class<?>... parameterTypes) {
        Preconditions.checkNotNull(klass, "klass == null");
        Preconditions.checkNotNull(parameterTypes, "parameterTypes == null");

        for (int i = 0; i < parameterTypes.length; i++) {
            if (parameterTypes[i] == null) {
                throw new NullPointerException("parameterTypes[" + i + "] == null");
            }
        }
    }

    public boolean isInstance(@Nullable Object instance) {
        return klass.isInstance(instance);
    }

    public static class WrappedMethod {
        private Method method;

        WrappedMethod(Method method) {
            method.setAccessible(true);
            this.method = method;
        }

        public <T> T call(Object instance, Object... args) {
            try {
                return (T) method.invoke(instance, args);
            } catch (IllegalAccessException e) {
                throw new UncheckedIllegalAccessException(e);
            } catch (InvocationTargetException e) {
                throw new UncheckedInvocationTargetException(e);
            }
        }

        public <T> T callStatic(Object... args) {
            return call(null, args);
        }

        public Method unwrap() {
            return method;
        }
    }

    public static class WrappedField {
        private Field field;

        WrappedField(Field field) {
            field.setAccessible(true);
            this.field = field;
        }

        public <T> T getValue(Object instance) {
            try {
                return (T) field.get(instance);
            } catch (IllegalAccessException e) {
                throw new UncheckedIllegalAccessException(e);
            }
        }

        public <T> T getStaticValue() {
            return getValue(null);
        }

        public void setValue(Object instance, Object value) {
            try {
                field.set(instance, value);
            } catch (IllegalAccessException e) {
                throw new UncheckedIllegalAccessException(e);
            }
        }

        public void setStaticValue(Object value) {
            setValue(null, value);
        }
    }

    public static class WrappedConstructor<T> {
        private Constructor<T> constructor;

        public WrappedConstructor(Constructor<T> constructor) {
            constructor.setAccessible(true);
            this.constructor = constructor;
        }

        public T newInstance(Object... args) {
            try {
                return constructor.newInstance(args);
            } catch (IllegalAccessException e) {
                throw new UncheckedIllegalAccessException(e);
            } catch (InvocationTargetException e) {
                throw new UncheckedInvocationTargetException(e);
            } catch (InstantiationException e) {
                throw new UncheckedInstantiationException(e);
            }
        }

        public Constructor<T> unwrap() {
            return constructor;
        }
    }
}
