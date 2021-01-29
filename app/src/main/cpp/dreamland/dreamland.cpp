//
// Created by canyie on 2019/11/12.
//

#include <dlfcn.h>
#include <unistd.h>
#include <string>
#include "dreamland.h"
#include "../utils/log.h"
#include "../utils/scoped_local_ref.h"
#include "../utils/well_known_classes.h"
#include "../utils/jni_helper.h"
#include "../pine.h"
#include "resources_hook.h"
#include "binder.h"

using namespace dreamland;

Dreamland* Dreamland::instance = nullptr;

static jboolean Main_initXResourcesNative(JNIEnv* env, jclass, jobject classLoader) {
    return static_cast<jboolean>(ResourcesHook::Init(env, classLoader));
}

static const JNINativeMethod gMainNativeMethods[] = {
        {"initXResourcesNative", "(Ljava/lang/ClassLoader;)Z", (void*) Main_initXResourcesNative}
};

bool Dreamland::ShouldDisable() {
    if (UNLIKELY(access(kBaseDir, F_OK) != 0)) {
        LOGE("Dreamland framework is broken: base directory is not exist!");
        return true;
    }
    if (UNLIKELY(access(kCoreJarFile, F_OK) != 0)) {
        LOGE("Dreamland framework is broken: core jar is not exist!");
        return true;
    }
    if (UNLIKELY(access(kDisableFile, F_OK) == 0)) {
        LOGW("Dreamland is disabled: disable flag file is exist.");
        return true;
    }
    return false;
}

bool Dreamland::javaInit(JNIEnv* env) {
    ScopedLocalRef<jclass> main_class(env);
    // 1. load core jar
    {
        ScopedLocalRef<jobject> class_loader(env);
        {
            ScopedLocalRef<jstring> dex_path(env, Dreamland::kCoreJarFile);
            ScopedLocalRef<jobject> system_class_loader(env, env->CallStaticObjectMethod(
                    WellKnownClasses::java_lang_ClassLoader,
                    WellKnownClasses::java_lang_ClassLoader_getSystemClassLoader));

            jmethodID constructor = env->GetMethodID(
                    WellKnownClasses::dalvik_system_PathClassLoader,
                    "<init>",
                    "(Ljava/lang/String;Ljava/lang/ClassLoader;)V");
            CHECK_FOR_JNI(constructor != nullptr,
                          "dalvik.system.PathClassLoader.<init>(java.lang.String, java.lang.ClassLoader) not found");
            class_loader.Reset(env->NewObject(WellKnownClasses::dalvik_system_PathClassLoader, constructor,
                                   dex_path.Get(), system_class_loader.Get()));
            if (JNIHelper::ExceptionCheck(env)) {
                LOGE("can't load the core jar");
                return false;
            }
        }

        // 2. Register native methods
        {
            main_class.Reset(JNIHelper::FindClassFromClassLoader(env, "top.canyie.dreamland.Main", class_loader.Get()));
            if (JNIHelper::ExceptionCheck(env)) {
                LOGE("main_class not found");
                return false;
            }

            env->RegisterNatives(main_class.Get(), gMainNativeMethods, NELEM(gMainNativeMethods));

            ScopedLocalRef<jclass> pine_class(env,
                    JNIHelper::FindClassFromClassLoader(env, "top.canyie.pine.Pine", class_loader.Get()));
            if (UNLIKELY(JNIHelper::ExceptionCheck(env))) {
                LOGE("Failed to load Pine class.");
                return false;
            }
            ScopedLocalRef<jclass> ruler_class(env,
                    JNIHelper::FindClassFromClassLoader(env, "top.canyie.pine.Ruler", class_loader.Get()));
            if (UNLIKELY(JNIHelper::ExceptionCheck(env))) {
                LOGE("Failed to load Ruler class.");
                return false;
            }
            if (UNLIKELY(!(register_Pine(env, pine_class.Get()) &&
                           register_Ruler(env, ruler_class.Get())))) {
                LOGE("Failed to register native methods.");
                return false;
            }
        }
    }
    // 3. call java main()
    jmethodID main = env->GetStaticMethodID(main_class.Get(), "zygoteInit", "()I");
    onAppProcessStart = env->GetStaticMethodID(main_class.Get(), "onAppProcessStart", "(Landroid/os/IBinder;)V");
    if (UNLIKELY(onAppProcessStart == nullptr)) {
        LOGE("Method onAppProcessStart() not found.");
        JNIHelper::AssertAndClearPendingException(env);
        return false;
    }
    onSystemServerStart = env->GetStaticMethodID(main_class.Get(), "onSystemServerStart", "()V");
    if (UNLIKELY(onSystemServerStart == nullptr)) {
        LOGE("Method onSystemServerStart() not found.");
        JNIHelper::AssertAndClearPendingException(env);
        return false;
    }
    jint status = env->CallStaticIntMethod(main_class.Get(), main);
    if (UNLIKELY(JNIHelper::ExceptionCheck(env))) {
        return false;
    }
    if (UNLIKELY(status != 0)) {
        LOGE("java zygoteInit() returned error %d", status);
        return false;
    }
    java_main_class = reinterpret_cast<jclass>(env->NewGlobalRef(main_class.Get()));
    return true;
}

bool Dreamland::InitializeImpl(JNIEnv* env) {
#ifdef DREAMLAND_DISABLE
    LOGW("Dreamland is disabled. Skipped initialize.");
    return false;
#endif
    CHECK_FOR_JNI(env->GetJavaVM(&java_vm) == JNI_OK, "env->GetJavaVM failed");
    WellKnownClasses::Init(env);
    if (Android::version >= Android::kO) Binder::Prepare(env);
    return javaInit(env);
}

bool Dreamland::Prepare(JNIEnv* env) {
    static bool tried_to_init = false;
    if (UNLIKELY(instance == nullptr)) {
        if (UNLIKELY(tried_to_init)) {
            return false;
        }
        tried_to_init = true;
        instance = new Dreamland;
        if (UNLIKELY(!instance->InitializeImpl(env))) {
            LOGE("Dreamland::InitializeImpl() returned false.");
            delete instance;
            instance = nullptr;
            return false;
        }
    }
    return true;
}

JNIEnv* Dreamland::GetJNIEnv() {
    JNIEnv* env = nullptr;
    CHECK(java_vm->GetEnv(reinterpret_cast<void**> (&env), JNI_VERSION_1_6) == JNI_OK,
          "java_vm->GetEnv failed");
    CHECK(env != nullptr, "env == nullptr");
    return env;
}

bool Dreamland::OnAppProcessStart(JNIEnv* env) {
    if (UNLIKELY(!Prepare(env))) return false;
    jobject service = nullptr;
    bool except_null = true;
    if (Android::version >= Android::kO) {
        service = Binder::GetBinder(env);
        Binder::Cleanup(env);
        except_null = false;
    }

    if (UNLIKELY(except_null || service)) {
        env->CallStaticVoidMethod(instance->java_main_class, instance->onAppProcessStart, service);
        if (UNLIKELY(JNIHelper::ExceptionCheck(env))) {
            LOGE("Failed to call java callback method onAppProcessStart");
            return false;
        }
    }
    return true;
}

bool Dreamland::OnSystemServerStart(JNIEnv* env) {
    if (UNLIKELY(!Prepare(env))) return false;
    env->CallStaticVoidMethod(instance->java_main_class, instance->onSystemServerStart);
    if (UNLIKELY(JNIHelper::ExceptionCheck(env))) {
        LOGE("Failed to call java callback method onSystemServerStart");
        return false;
    }
    return true;
}

Dreamland::Dreamland() : java_vm(nullptr), java_main_class(nullptr),
                         onSystemServerStart(nullptr), onAppProcessStart(nullptr) {
}

Dreamland::~Dreamland() {
    if (java_main_class != nullptr) {
        GetJNIEnv()->DeleteGlobalRef(java_main_class);
    }
}
