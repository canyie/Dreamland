//
// Created by canyie on 2019/11/12.
//

#include <dlfcn.h>
#include <unistd.h>
#include <string>
#include <iterator>
#include <fcntl.h>
#include <cerrno>
#include <sys/stat.h>
#include <fstream>
#include <sstream>
#include "dreamland.h"
#include "../utils/log.h"
#include "../utils/scoped_local_ref.h"
#include "../utils/well_known_classes.h"
#include "../utils/jni_helper.h"
#include "../pine.h"
#include "resources_hook.h"
#include "binder.h"
#include "dex_loader.h"

using namespace dreamland;

Dreamland* Dreamland::instance = nullptr;
std::vector<char>* Dreamland::dex_data = nullptr;

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

void Dreamland::PreloadDexData() {
    std::string core_jar_file(kCoreJarFile);
    std::ifstream is(core_jar_file, std::ios::binary);
    if (UNLIKELY(!is.good())) {
        LOGE("Cannot open the core dex file: %s", strerror(errno));
        return;
    }
    dex_data = new std::vector<char>{std::istreambuf_iterator<char>(is),
            std::istreambuf_iterator<char>()};
    is.close();
}

void Dreamland::Prepare() {
    if (Android::version >= Android::kO) PreloadDexData();
}

bool Dreamland::ZygoteJavaInit(JNIEnv* env) {
    ScopedLocalRef<jclass> main_class(env);
    // 1. load core jar
    {
        ScopedLocalRef<jobject> class_loader(env, DexLoader::FromFile(env, kCoreJarFile));
        if (UNLIKELY(class_loader.IsNull())) {
            LOGE("Can't load the core jar file!!");
            return false;
        }

        // 2. Register native methods
        {
            main_class.Reset(JNIHelper::FindClassFromClassLoader(env, "top.canyie.dreamland.Main",
                                                                 class_loader.Get()));
            if (JNIHelper::ExceptionCheck(env)) {
                LOGE("main_class not found");
                return false;
            }
            if (UNLIKELY(!RegisterNatives(env, main_class.Get(), class_loader.Get()))) {
                LOGE("Failed to register native methods");
                return false;
            }
        }
    }
    // 3. call java main()
    jmethodID main = env->GetStaticMethodID(main_class.Get(), "zygoteInit", "()I");
    if (UNLIKELY(!FindEntryMethods(env, main_class.Get(), true, true))) {
        LOGE("Failed to find some entry methods");
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

bool Dreamland::ZygoteInitImpl(JNIEnv* env) {
#ifdef DREAMLAND_DISABLE
    LOGW("Dreamland is disabled. Skipped initialize.");
    return false;
#endif
    CHECK_FOR_JNI(env->GetJavaVM(&java_vm) == JNI_OK, "env->GetJavaVM failed");
    WellKnownClasses::Init(env);
    DexLoader::Prepare(env);
    if (Android::version >= Android::kO) {
        return Binder::Prepare(env);
    } else {
        return ZygoteJavaInit(env);
    }
}

bool Dreamland::ZygoteInit(JNIEnv* env) {
    static bool tried_to_prepare = false;
    if (UNLIKELY(instance == nullptr)) {
        if (UNLIKELY(tried_to_prepare)) {
            return false;
        }
        tried_to_prepare = true;
        instance = new Dreamland;
        if (UNLIKELY(!instance->ZygoteInitImpl(env))) {
            LOGE("Dreamland::ZygoteInitImpl() returned false.");
            delete instance;
            instance = nullptr;
            return false;
        }
    }
    return true;
}

bool Dreamland::RegisterNatives(JNIEnv* env, jclass main_class, jobject class_loader) {
    env->RegisterNatives(main_class, gMainNativeMethods, NELEM(gMainNativeMethods));
    {
        ScopedLocalRef<jclass> pine_class(env, JNIHelper::FindClassFromClassLoader(
                env, "top.canyie.pine.Pine", class_loader));
        if (UNLIKELY(JNIHelper::ExceptionCheck(env))) {
            LOGE("Failed to load Pine class.");
            return false;
        }
        ScopedLocalRef<jclass> ruler_class(env, JNIHelper::FindClassFromClassLoader(
                env, "top.canyie.pine.Ruler", class_loader));
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

    ScopedLocalRef<jclass> enhances_class(env, JNIHelper::FindClassFromClassLoader(
            env, "top.canyie.pine.enhances.PineEnhances", class_loader));
    if (UNLIKELY(!init_PineEnhances(java_vm, env, enhances_class.Get()))) {
        LOGE("Failed to init PineEnhances.");
        return false;
    }
    return true;
}


JNIEnv* Dreamland::GetJNIEnv() {
    JNIEnv* env = nullptr;
    CHECK(java_vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) == JNI_OK,
          "java_vm->GetEnv failed");
    CHECK(env != nullptr, "env == nullptr");
    return env;
}

bool Dreamland::EnsureDexLoaded(JNIEnv* env, bool app) {
    if (java_main_class == nullptr) {
        // Dex not loaded
        return LoadDexFromMemory(env, app);
    }
    return true;
}

bool Dreamland::LoadDexFromMemory(JNIEnv* env, bool app) {
    ScopedLocalRef<jclass> main_class(env);
    {
        ScopedLocalRef<jobject> class_loader(env, DexLoader::FromMemory(env, dex_data->data(), dex_data->size()));
        if (UNLIKELY(class_loader.IsNull())) {
            LOGE("Failed to load the core jar from memory");
            return false;
        }

        main_class.Reset(JNIHelper::FindClassFromClassLoader(env, "top.canyie.dreamland.Main",
                class_loader.Get()));
        if (JNIHelper::ExceptionCheck(env)) {
            LOGE("main_class not found");
            return false;
        }
        if (UNLIKELY(!RegisterNatives(env, main_class.Get(), class_loader.Get()))) {
            LOGE("Failed to register native methods");
            return false;
        }
    }
    if (UNLIKELY(!FindEntryMethods(env, main_class.Get(), app, !app))) {
        LOGE("Failed to find entry methods for %s process", app ? "app" : "system_server");
        return false;
    }
    java_main_class = reinterpret_cast<jclass>(env->NewGlobalRef(main_class.Get()));
    return true;
}

bool Dreamland::FindEntryMethods(JNIEnv* env, jclass main_class, bool app, bool system_server) {
    if (app) {
        onAppProcessStart = env->GetStaticMethodID(main_class, "onAppProcessStart", "(Landroid/os/IBinder;)V");
        if (UNLIKELY(onAppProcessStart == nullptr)) {
            LOGE("Method onAppProcessStart() not found.");
            JNIHelper::AssertAndClearPendingException(env);
            return false;
        }
    }
    if (system_server) {
        onSystemServerStart = env->GetStaticMethodID(main_class, "onSystemServerStart", "()V");
        if (UNLIKELY(onSystemServerStart == nullptr)) {
            LOGE("Method onSystemServerStart() not found.");
            JNIHelper::AssertAndClearPendingException(env);
            return false;
        }
    }
    return true;
}

bool Dreamland::OnAppProcessStart(JNIEnv* env) {
    if (UNLIKELY(instance == nullptr)) return false;
    //if (UNLIKELY(!ZygoteInit(env))) return false;

    jobject service = nullptr;
    bool except_null = true;
    if (Android::version >= Android::kO) {
        service = Binder::GetBinder(env);
        Binder::Cleanup(env);
        except_null = false;
    }

    if (UNLIKELY(except_null || service)) {
        if (UNLIKELY(!instance->EnsureDexLoaded(env, true))) {
            LOGE("Failed to load dex data in app process");
            return false;
        }
        env->CallStaticVoidMethod(instance->java_main_class, instance->onAppProcessStart, service);
        if (UNLIKELY(JNIHelper::ExceptionCheck(env))) {
            LOGE("Failed to call java callback method onAppProcessStart");
            return false;
        }
    }
    return true;
}

bool Dreamland::OnSystemServerStart(JNIEnv* env) {
    if (UNLIKELY(instance == nullptr)) return false;
    //if (UNLIKELY(!ZygoteInit(env))) return false;
    if (UNLIKELY(!instance->EnsureDexLoaded(env, false))) {
        LOGE("Failed to load dex data in system_server");
        return false;
    }
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