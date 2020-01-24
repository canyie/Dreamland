//
// Created by canyie on 2019/11/12.
//

#include <dlfcn.h>
#include <unistd.h>
#include "dreamland.h"
#include "../sandhook/sandhook_jni.h"
#include "../utils/log.h"
#include "../utils/scoped_local_ref.h"
#include "../utils/well_known_classes.h"
#include "../utils/jni_helper.h"
#include "../utils/required_platform_classes.h"

using namespace dreamland;

Dreamland *Dreamland::instance = nullptr;

bool Dreamland::ShouldDisable() {
    if (access(DREAMLAND_BASE, F_OK) != 0) {
        LOGE("Dreamland framework is broken: base directory is not exist!");
        return true;
    }
    if (access(DREAMLAND_CORE_JAR, F_OK) != 0) {
        LOGE("Dreamland framework is broken: core jar is not exist!");
        return true;
    }
    if (access(DREAMLAND_FLAG_DISABLE_FILE, F_OK) == 0) {
        LOGW("Dreamland is disabled: disable flag is exist.");
        return true;
    }
    return false;
}

bool Dreamland::javaInit(JNIEnv *env) {
    // 1. load core jar
    ScopedLocalRef<jstring> dex_path(env, Dreamland::DREAMLAND_CORE_JAR);
    ScopedLocalRef<jobject> system_class_loader(env, env->CallStaticObjectMethod(
            WellKnownClasses::java_lang_ClassLoader,
            WellKnownClasses::java_lang_ClassLoader_getSystemClassLoader));

    jmethodID constructor = env->GetMethodID(WellKnownClasses::dalvik_system_PathClassLoader,
                                             "<init>",
                                             "(Ljava/lang/String;Ljava/lang/ClassLoader;)V");
    CHECK_FOR_JNI(constructor != nullptr,
                  "dalvik.system.PathClassLoader.<init>(java.lang.String, java.lang.ClassLoader) not found");
    ScopedLocalRef<jobject> class_loader(env, env->NewObject(
            WellKnownClasses::dalvik_system_PathClassLoader, constructor,
            dex_path.Get(), system_class_loader.Get()));
    if (JNIHelper::ExceptionCheck(env)) {
        LOGE("can't load the core jar");
        return false;
    }
    dex_path.Reset();
    system_class_loader.Reset();

    //2. register native methods
    ScopedLocalRef<jstring> sand_hook_class_name(env, "com.swift.sandhook.SandHook");
    ScopedLocalRef<jstring> never_call_class_name(env, "com.swift.sandhook.ClassNeverCall");

    ScopedLocalRef<jclass> sand_hook_class(env,
                                           reinterpret_cast<jclass> (env->CallStaticObjectMethod(
                                                   WellKnownClasses::java_lang_Class,
                                                   WellKnownClasses::java_lang_Class_forName_String_boolean_ClassLoader,
                                                   sand_hook_class_name.Get(), JNI_TRUE,
                                                   class_loader.Get())));

    ScopedLocalRef<jclass> never_call_class(env,
                                            reinterpret_cast<jclass> (env->CallStaticObjectMethod(
                                                    WellKnownClasses::java_lang_Class,
                                                    WellKnownClasses::java_lang_Class_forName_String_boolean_ClassLoader,

                                                    never_call_class_name.Get(), JNI_TRUE,
                                                    class_loader.Get())));

    sand_hook_class_name.Reset();
    never_call_class_name.Reset();
    if (!register_SandHook(env, sand_hook_class.Get(), never_call_class.Get())) {
        LOGE("Can't register native methods of SandHook");
        return false;
    }
    sand_hook_class.Reset();
    never_call_class.Reset();
    // 3. call java main()
    ScopedLocalRef<jstring> main_class_name(env, "com.canyie.dreamland.Main");
    ScopedLocalRef<jclass> main_class(env,
                                      reinterpret_cast<jclass>(env->CallStaticObjectMethod(
                                              WellKnownClasses::java_lang_Class,
                                              WellKnownClasses::java_lang_Class_forName_String_boolean_ClassLoader,
                                              main_class_name.Get(), JNI_TRUE,
                                              class_loader.Get())));
    if (JNIHelper::ExceptionCheck(env)) {
        LOGE("main_class not found");
        return false;
    }

    main_class_name.Reset();
    system_class_loader.Reset();
    class_loader.Reset();
    jmethodID main = env->GetStaticMethodID(main_class.Get(), "init", "()I");
    onAppProcessStart = env->GetStaticMethodID(main_class.Get(), "onAppProcessStart",
                                               "(Ljava/lang/String;)V");
    if (UNLIKELY(onAppProcessStart == nullptr)) {
        LOGE("Method Lcom/canyie/dreamland/Main;->onAppProcessStart(Ljava/lang/String;)V not found.");
        JNIHelper::AssertAndClearPendingException(env);
        return false;
    }
    onSystemServerStart = env->GetStaticMethodID(main_class.Get(), "onSystemServerStart", "()V");
    if (UNLIKELY(onSystemServerStart == nullptr)) {
        LOGE("Method Lcom/canyie/dreamland/Main;->onSystemServerStart()V not found.");
        JNIHelper::AssertAndClearPendingException(env);
        return false;
    }
    jint status = env->CallStaticIntMethod(main_class.Get(), main);
    if (UNLIKELY(JNIHelper::ExceptionCheck(env))) {
        return false;
    }
    if (UNLIKELY(status != 0)) {
        LOGE("onZygoteStart() returned error %d", status);
        return false;
    }
    java_main_class = reinterpret_cast<jclass> (env->NewGlobalRef(main_class.Get()));
    main_class.Reset();
    return true;
}

bool Dreamland::InitializeImpl(JNIEnv *env) {
#ifdef DREAMLAND_DISABLE
    LOGW("Dreamland is disabled. Skipped initialize.");
    return false;
#endif
    /*
    lib_android_runtime = dlopen("libandroid_runtime.so", RTLD_NOW | RTLD_GLOBAL);
    if(lib_android_runtime == nullptr) {
        LOGE("libandroid_runtime.so is not available: %s",dlerror());
        return false;
    }
    void* java_vm_addr = dlsym(lib_android_runtime, "_ZN7android14AndroidRuntime7mJavaVME");
    if(java_vm_addr == nullptr) {
        LOGE("android::AndroidRuntime::mJavaVM is not available: %s", dlerror());
        return false;
    }
    java_vm = (JavaVM*)(*(void**)java_vm_addr);
    if(java_vm == nullptr) {
        LOGE("android::AndroidRuntime::mJavaVM == nullptr; The android runtime is initialized? ");
        return false;
    }
    JNIEnv *env = GetJNIEnv();
    if(env == nullptr) {
        return false;
    }
     */
    CHECK_FOR_JNI(env->GetJavaVM(&java_vm) == JNI_OK, "env->GetJavaVM failed");
    WellKnownClasses::Init(env);
    return javaInit(env);
}

bool Dreamland::Initialize(JNIEnv *env) {
    if (instance != nullptr) {
        LOGE("The class Dreamland allows only one instance.");
        return false;
    }
    auto instance = new Dreamland;
    if (!instance->InitializeImpl(env)) {
        LOGE("Dreamland::InitializeImpl() returned false.");
        delete instance;
        return false;
    }
    Dreamland::instance = instance;
    return true;
}

JNIEnv *Dreamland::GetJNIEnv() {
    /*
    void* get_jni_env = dlsym(lib_android_runtime, "_ZN7android14AndroidRuntime9getJNIEnvEv");
    if(get_jni_env == nullptr) {
        LOGE("android::AndroidRuntime::getJNIEnv is not available: %s", dlerror());
        return nullptr;
    }
    return ((JNIEnv*(*)()) get_jni_env)();
     */
    JNIEnv *env = nullptr;
    CHECK(java_vm->GetEnv(reinterpret_cast<void **> (&env), JNI_VERSION_1_6) == JNI_OK,
          "java_vm->GetEnv failed");
    CHECK(env != nullptr, "env == nullptr");
    return env;
}

bool Dreamland::OnAppProcessStart(jstring java_nice_name) {
    Dreamland *instance = GetInstance();
    if (instance == nullptr) {
        LOGE("Application process started but the Dreamland framework is not initialized.");
        return false;
    }
    JNIEnv *env = instance->GetJNIEnv();
    env->CallStaticVoidMethod(instance->java_main_class, instance->onAppProcessStart,
                              java_nice_name);
    if (UNLIKELY(JNIHelper::ExceptionCheck(env))) {
        LOGE("Failed to call callback method Lcom/canyie/dreamland/Main;->onAppProcessStart(Ljava/lang/String;)V");
        return false;
    }
    return true;
}

bool Dreamland::OnSystemServerStart() {
    Dreamland *instance = GetInstance();
    if (instance == nullptr) {
        LOGE("System server started but the Dreamland framework is not initialized.");
        return false;
    }
    JNIEnv *env = instance->GetJNIEnv();
    env->CallStaticVoidMethod(instance->java_main_class, instance->onSystemServerStart);
    if (UNLIKELY(JNIHelper::ExceptionCheck(env))) {
        LOGE("Failed to call callback method Lcom/canyie/dreamland/Main;->onSystemServerStart()V");
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
