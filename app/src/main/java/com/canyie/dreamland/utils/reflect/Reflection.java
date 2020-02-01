package com.canyie.dreamland.utils.reflect;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.canyie.dreamland.utils.Preconditions;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Created by canyie on 2019/10/24.
 */
@SuppressWarnings({"unchecked", "unused", "WeakerAccess"})
public final class Reflection<T> {
    @NonNull
    private Class<T> clazz;

    private Reflection(@NonNull Class<T> clazz) {
        this.clazz = clazz;
    }

    public static Reflection<?> on(@NonNull String name) {
        return new Reflection(findClass(name));
    }

    public static Reflection<?> on(@NonNull String name, @NonNull ClassLoader loader) {
        return new Reflection(findClass(name, loader));
    }

    public static <T> Reflection<T> on(@NonNull Class<T> clazz) {
        Preconditions.checkNotNull(clazz, "clazz == null");
        return new Reflection(clazz);
    }

    @NonNull
    public Class<T> unwrap() {
        return clazz;
    }

    public static MethodWrapper wrap(@NonNull Method method) {
        Preconditions.checkNotNull(method, "method == null");
        return new MethodWrapper(method);
    }

    public MethodWrapper method(@NonNull String name, @NonNull Class<?>... parameterTypes) {
        return method(clazz, name, parameterTypes);
    }

    public static MethodWrapper method(@NonNull Class<?> clazz, @NonNull String name, @Nullable Class<?>... parameterTypes) {
        return wrap(getMethod(clazz, name, parameterTypes));
    }

    public static FieldWrapper wrap(@NonNull Field field) {
        Preconditions.checkNotNull(field, "field == null");
        return new FieldWrapper(field);
    }

    public FieldWrapper field(@NonNull String name) {
        return field(clazz, name);
    }

    public static FieldWrapper field(@NonNull Class<?> clazz, @NonNull String name) {
        return wrap(getField(clazz, name));
    }

    public static <T> ConstructorWrapper<T> wrap(@NonNull Constructor<T> constructor) {
        Preconditions.checkNotNull(constructor, "constructor == null");
        return new ConstructorWrapper<>(constructor);
    }

    public ConstructorWrapper<T> constructor(@Nullable Class<?>... parameterTypes) {
        return wrap(getConstructor(clazz, parameterTypes));
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
    public static Method getMethod(@NonNull Class<?> clazz, @NonNull String name, @Nullable Class<?>... parameterTypes) {
        Method method = findMethod(clazz, name, parameterTypes);
        if (method == null) {
            throw new UncheckedNoSuchMethodException("No method '" + name + getParameterTypesMessage(parameterTypes) + "' found in class " + clazz.getName());
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
    public static Method findMethod(@NonNull Class<?> clazz, @NonNull String name, @Nullable Class<?>... parameterTypes) {
        checkForFindMethod(clazz, name, parameterTypes);
        return findMethodNoChecks(clazz, name, parameterTypes);
    }

    @Nullable
    public static Method findMethodNoChecks(Class<?> clazz, String name, Class<?>... parameterTypes) {
        while (clazz != null) {
            try {
                Method method = clazz.getDeclaredMethod(name, parameterTypes);
                method.setAccessible(true);
                return method;
            } catch (NoSuchMethodException ignored) {
            }
            clazz = clazz.getSuperclass();
        }
        return null;
    }

    private static void checkForFindMethod(Class<?> clazz, String name, Class<?>... parameterTypes) {
        Preconditions.checkNotNull(clazz, "clazz == null");
        Preconditions.checkNotEmpty(name, "name is null or empty");
        Preconditions.checkNotNull(parameterTypes, "parameterTypes == null");

        for (int i = 0; i < parameterTypes.length; i++) {
            if (parameterTypes[i] == null) {
                throw new NullPointerException("parameterTypes[" + i + "] == null");
            }
        }
    }

    @NonNull
    public static Field getField(@NonNull Class<?> clazz, @NonNull String name) {
        Field field = findField(clazz, name);
        if (field == null) {
            throw new UncheckedNoSuchFieldException("No field '" + name + "' found in class " + clazz.getName());
        }
        return field;
    }

    @Nullable
    public static Field findField(Class<?> clazz, String name) {
        checkForFindField(clazz, name);
        return findFieldNoChecks(clazz, name);
    }

    @Nullable
    public static Field findFieldNoChecks(@NonNull Class<?> clazz, @NonNull String name) {
        while (clazz != null) {
            try {
                Field field = clazz.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException ignored) {
            }
            clazz = clazz.getSuperclass();
        }
        return null;
    }

    private static void checkForFindField(Class<?> clazz, String name) {
        Preconditions.checkNotNull(clazz, "clazz == null");
        Preconditions.checkNotEmpty(name, "name is null or empty");
    }

    @NonNull
    public static <T> Constructor<T> getConstructor(@NonNull Class<T> clazz, @Nullable Class<?>... parameterTypes) {
        Constructor<T> c = findConstructor(clazz, parameterTypes);
        if (c == null) {
            throw new UncheckedNoSuchMethodException("No constructor '" + clazz.getName() + getParameterTypesMessage(parameterTypes) + "' found in class " + clazz.getName());
        }
        return c;
    }

    @Nullable
    public static <T> Constructor<T> findConstructor(@NonNull Class<T> clazz, @Nullable Class<?>... parameterTypes) {
        checkForFindConstructor(clazz, parameterTypes);
        return findConstructorNoChecks(clazz, parameterTypes);
    }

    @Nullable
    public static <T> Constructor<T> findConstructorNoChecks(@NonNull Class<T> clazz, @Nullable Class<?>... parameterTypes) {
        try {
            Constructor<T> constructor = clazz.getDeclaredConstructor(parameterTypes);
            constructor.setAccessible(true);
            return constructor;
        } catch (NoSuchMethodException ignored) {
        }
        return null;
    }

    private static void checkForFindConstructor(Class<?> clazz, Class<?>... parameterTypes) {
        Preconditions.checkNotNull(clazz, "clazz == null");
        Preconditions.checkNotNull(parameterTypes, "parameterTypes == null");

        for (int i = 0; i < parameterTypes.length; i++) {
            if (parameterTypes[i] == null) {
                throw new NullPointerException("parameterTypes[" + i + "] == null");
            }
        }
    }

    public boolean isInstance(@Nullable Object instance) {
        return clazz.isInstance(instance);
    }

    public int getModifiers() {
        return clazz.getModifiers();
    }

    public boolean isLambdaClass() {
        return isLambdaClass(clazz);
    }

    public boolean isProxyClass() {
        return isProxyClass(clazz);
    }

    public static boolean isLambdaClass(Class<?> clazz) {
        return clazz.getName().contains("$$Lambda$");
    }

    public static boolean isProxyClass(Class<?> clazz) {
        return Proxy.isProxyClass(clazz);
    }

    public static class MemberWrapper<M extends AccessibleObject & Member> {
        M member;
        MemberWrapper(M member) {
            member.setAccessible(true);
            this.member = member;
        }

        @NonNull public final M unwrap() {
            return member;
        }

        public final int getModifiers() {
            return member.getModifiers();
        }

        public final Class<?> getDeclaringClass() {
            return member.getDeclaringClass();
        }
    }

    public static class MethodWrapper extends MemberWrapper<Method> {
        MethodWrapper(Method method) {
            super(method);
        }

        public <T> T call(Object instance, Object... args) {
            try {
                return (T) member.invoke(instance, args);
            } catch (IllegalAccessException e) {
                throw new UncheckedIllegalAccessException(e);
            } catch (InvocationTargetException e) {
                throw new UncheckedInvocationTargetException(e);
            }
        }

        public <T> T callStatic(Object... args) {
            return call(null, args);
        }
    }

    public static class FieldWrapper extends MemberWrapper<Field> {
        FieldWrapper(Field field) {
            super(field);
        }

        public <T> T getValue(Object instance) {
            try {
                return (T) member.get(instance);
            } catch (IllegalAccessException e) {
                throw new UncheckedIllegalAccessException(e);
            }
        }

        public <T> T getStaticValue() {
            return getValue(null);
        }

        public void setValue(Object instance, Object value) {
            try {
                member.set(instance, value);
            } catch (IllegalAccessException e) {
                throw new UncheckedIllegalAccessException(e);
            }
        }

        public void setStaticValue(Object value) {
            setValue(null, value);
        }

        public Class<?> getType() {
            return member.getType();
        }
    }

    public static class ConstructorWrapper<T> extends MemberWrapper<Constructor<T>> {
        ConstructorWrapper(Constructor<T> constructor) {
            super(constructor);
        }

        public T newInstance(Object... args) {
            try {
                return member.newInstance(args);
            } catch (IllegalAccessException e) {
                throw new UncheckedIllegalAccessException(e);
            } catch (InvocationTargetException e) {
                throw new UncheckedInvocationTargetException(e);
            } catch (InstantiationException e) {
                throw new UncheckedInstantiationException(e);
            }
        }
    }
}
