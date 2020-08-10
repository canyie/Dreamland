//
// Created by canyie on 2019/11/24.
//

#ifndef DREAMLAND_JNI_HELPER_H
#define DREAMLAND_JNI_HELPER_H

#include <jni.h>
#include "well_known_classes.h"

namespace dreamland {
    class JNIHelper {
    public:
        static bool ExceptionCheck(JNIEnv *env) {
            if(env->ExceptionCheck()) {
                LOGE("JNI Exception: ");
                env->ExceptionDescribe();
                env->ExceptionClear();
                return true;
            }
            return false;
        }

        static bool ThrowException(JNIEnv *env, const char *className, const char *errMsg) {
            if(env->ExceptionCheck()) {
                LOGW("Ignoring JNI exception: ");
                env->ExceptionDescribe();
                env->ExceptionClear();
            }
            ScopedLocalRef<jclass> exceptionClass(env, env->FindClass(className));
            if(exceptionClass == nullptr) {
                return false;
            }
            return env->ThrowNew(exceptionClass.Get(), errMsg) == JNI_OK;
        }

        static bool RegisterNativeMethods(JNIEnv *env, const char *className, JNINativeMethod *methods, int numMethods) {
            ScopedLocalRef<jclass> clazz(env, env->FindClass(className));
            if(clazz == nullptr) {
                return false;
            }
            if(env->RegisterNatives(clazz.Get(), methods, numMethods) < 0) {
                LOGE("Failed to register native methods of class %s", className);
                return false;
            }
            return true;
        }

        static jclass FindClassFromClassLoader(JNIEnv* env, const char* name, jobject loader) {
            ScopedLocalRef<jstring> name_ref(env, name);
            return static_cast<jclass>(env->CallObjectMethod(loader,
                    WellKnownClasses::java_lang_ClassLoader_loadClass, name_ref.Get()));
        }

        static void AssertPendingException(JNIEnv *env) {
            CHECK_FOR_JNI(env->ExceptionCheck(), "Assert pending exception failed");
        }

        static void AssertAndClearPendingException(JNIEnv *env) {
            AssertPendingException(env);
            LOGE("Pending exception: ");
            env->ExceptionDescribe();
            env->ExceptionClear();
        }

        static bool GetMethodID(JNIEnv *env, jclass clazz, const char *name, const char *signature, bool is_static, jmethodID *out) {
            jmethodID method = is_static ? env->GetStaticMethodID(clazz, name, signature) : env->GetMethodID(clazz, name, signature);
            if (method == nullptr) {
                AssertPendingException(env);
                return false;
            }
            *out = method;
            return true;
        }

    private:
        DISALLOW_IMPLICIT_CONSTRUCTORS(JNIHelper);
    };
}

#endif //DREAMLAND_JNI_HELPER_H
