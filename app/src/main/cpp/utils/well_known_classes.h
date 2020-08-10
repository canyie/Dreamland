//
// Created by canyie on 2019/11/17.
//

#ifndef DREAMLAND_WELL_KNOWN_CLASSES_H
#define DREAMLAND_WELL_KNOWN_CLASSES_H

#include <jni.h>
#include "log.h"
#include "macros.h"

namespace dreamland {
    class WellKnownClasses {
    public:
        //static jclass java_lang_Object;
        //static jclass java_lang_Class;
        static jclass java_lang_ClassLoader;
        static jclass java_lang_String;
        //static jclass java_lang_Thread;
        static jclass dalvik_system_PathClassLoader;
        //static jclass dalvik_system_DexClassLoader;

        static jmethodID java_lang_ClassLoader_loadClass;
        static jmethodID java_lang_ClassLoader_getSystemClassLoader;

        static void Init(JNIEnv *env);

        static void Clear(JNIEnv *env);
    private:
        static bool inited;

        static jclass CacheClass(JNIEnv *env, const char *name);

        static jmethodID
        CacheMethod(JNIEnv *env, jclass klass, const char *name, const char *signature,
                    bool is_static);

        static void ClearGlobalReference(JNIEnv *env, jclass *ptr) {
            env->DeleteGlobalRef(*ptr);
            *ptr = nullptr;
        }

        DISALLOW_IMPLICIT_CONSTRUCTORS(WellKnownClasses);
    };
}

#endif //DREAMLAND_WELL_KNOWN_CLASSES_H
