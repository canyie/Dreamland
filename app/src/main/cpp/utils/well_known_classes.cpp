//
// Created by canyie on 2019/11/18.
//
#include "well_known_classes.h"

using namespace dreamland;

bool WellKnownClasses::inited = false;

//jclass WellKnownClasses::java_lang_Object = nullptr;
//jclass WellKnownClasses::java_lang_Class = nullptr;
jclass WellKnownClasses::java_lang_ClassLoader = nullptr;
jclass WellKnownClasses::java_lang_String = nullptr;
//jclass WellKnownClasses::java_lang_Thread = nullptr;
//jclass WellKnownClasses::dalvik_system_DexClassLoader = nullptr;

jmethodID WellKnownClasses::java_lang_ClassLoader_loadClass = nullptr;
jmethodID WellKnownClasses::java_lang_ClassLoader_getSystemClassLoader = nullptr;

jclass WellKnownClasses::CacheClass(JNIEnv *env, const char *name) {
    jclass localClassRef = env->FindClass(name);
    CHECK_FOR_JNI(localClassRef != nullptr, "Didn't find class '%s'", name);
    jclass globalClassRef = reinterpret_cast<jclass> (env->NewGlobalRef(localClassRef));
    env->DeleteLocalRef(localClassRef);
    CHECK_FOR_JNI(globalClassRef != nullptr, "globalClassRef == nullptr; out of memory? ");
    return globalClassRef;
}

jmethodID WellKnownClasses::CacheMethod(JNIEnv *env, jclass klass, const char *name,
                                        const char *signature, bool is_static) {
    jmethodID method = is_static ? env->GetStaticMethodID(klass, name, signature)
                                 : env->GetMethodID(klass, name, signature);
    CHECK_FOR_JNI(method != nullptr, "No match method %s%s.", name, signature);
    return method;
}

void WellKnownClasses::Init(JNIEnv *env) {
    if (inited) return;
    //java_lang_Object = CacheClass(env, "java/lang/Object");
    //java_lang_Class = CacheClass(env, "java/lang/Class");
    java_lang_ClassLoader = CacheClass(env, "java/lang/ClassLoader");
    java_lang_String = CacheClass(env, "java/lang/String");
    //java_lang_Thread = CacheClass(env, "java/lang/Thread");
    //dalvik_system_DexClassLoader = CacheClass(env, "dalvik/system/DexClassLoader");

    java_lang_ClassLoader_loadClass = CacheMethod(env, java_lang_ClassLoader, "loadClass",
            "(Ljava/lang/String;)Ljava/lang/Class;", false);

    java_lang_ClassLoader_getSystemClassLoader = CacheMethod(env, java_lang_ClassLoader,
            "getSystemClassLoader", "()Ljava/lang/ClassLoader;", true);
    inited = true;
}

void WellKnownClasses::Clear(JNIEnv *env) {
    //ClearGlobalReference(env, &java_lang_Object);
    //ClearGlobalReference(env, &java_lang_Class);
    ClearGlobalReference(env, &java_lang_ClassLoader);
    ClearGlobalReference(env, &java_lang_String);
    //ClearGlobalReference(env, &java_lang_Thread);
    //ClearGlobalReference(env, &dalvik_system_DexClassLoader);

    java_lang_ClassLoader_loadClass = nullptr;
    java_lang_ClassLoader_getSystemClassLoader = nullptr;

    inited = false;
}