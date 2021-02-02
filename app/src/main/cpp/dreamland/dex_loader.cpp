//
// Created by canyie on 2021/2/2.
//

#include "dex_loader.h"
#include "../utils/well_known_classes.h"
#include "android.h"
#include "../utils/scoped_local_ref.h"

using namespace dreamland;

jclass DexLoader::PathClassLoader = nullptr;
jmethodID DexLoader::PathClassLoader_init = nullptr;
jclass DexLoader::InMemoryDexClassLoader = nullptr;
jmethodID DexLoader::InMemoryDexClassLoader_init = nullptr;

void DexLoader::Prepare(JNIEnv* env) {
    if (Android::version < Android::kO) {
        PathClassLoader = WellKnownClasses::CacheClass(env, "dalvik/system/PathClassLoader");
        PathClassLoader_init = WellKnownClasses::CacheMethod(env, PathClassLoader, "<init>",
                "(Ljava/lang/String;Ljava/lang/ClassLoader;)V", false);
    } else {
        InMemoryDexClassLoader = WellKnownClasses::CacheClass(env, "dalvik/system/InMemoryDexClassLoader");
        InMemoryDexClassLoader_init = WellKnownClasses::CacheMethod(env, InMemoryDexClassLoader, "<init>",
                "(Ljava/nio/ByteBuffer;Ljava/lang/ClassLoader;)V", false);
    }
}

jobject DexLoader::FromMemory(JNIEnv* env, char* dex, const size_t size) {
    ScopedLocalRef<jobject> system_class_loader(env, env->CallStaticObjectMethod(
            WellKnownClasses::java_lang_ClassLoader,
            WellKnownClasses::java_lang_ClassLoader_getSystemClassLoader));
    ScopedLocalRef<jobject> buffer(env, env->NewDirectByteBuffer(dex, size));
    jobject loader = env->NewObject(InMemoryDexClassLoader, InMemoryDexClassLoader_init,
            buffer.Get(), system_class_loader.Get());
    if (UNLIKELY(env->ExceptionCheck())) {
        LOGE("Failed to load dex data");
        env->ExceptionDescribe();
        env->ExceptionClear();
        return nullptr;
    }
    return loader;
}

jobject DexLoader::FromFile(JNIEnv* env, const char* path) {
    ScopedLocalRef<jstring> dex_path(env, path);
    ScopedLocalRef<jobject> system_class_loader(env, env->CallStaticObjectMethod(
            WellKnownClasses::java_lang_ClassLoader,
            WellKnownClasses::java_lang_ClassLoader_getSystemClassLoader));
    jobject loader = env->NewObject(PathClassLoader, PathClassLoader_init, dex_path.Get(), system_class_loader.Get());
    if (UNLIKELY(env->ExceptionCheck())) {
        LOGE("Failed to load dex %s", path);
        env->ExceptionDescribe();
        env->ExceptionClear();
        return nullptr;
    }
    return loader;
}
