//
// Created by canyie on 2019/11/12.
//

#ifndef DREAMLAND_DREAMLAND_H
#define DREAMLAND_DREAMLAND_H

#include <jni.h>
#include "../utils/log.h"
#include "../utils/macros.h"

namespace dreamland {
    class Dreamland {
    public:
        static constexpr int VERSION = 1;
        static bool ShouldDisable();
        static bool Initialize(JNIEnv *env);
        static Dreamland* GetInstance() {
            return instance;
        }
        static Dreamland* GetOrCreateInstance(JNIEnv *env) {
            if(instance == nullptr) {
                Initialize(env);
            }
            return instance;
        }

        static bool OnAppProcessStart();
        static bool OnSystemServerStart();

        JavaVM *GetJavaVM() {
            return java_vm;
        }

        JNIEnv *GetJNIEnv();

    private:
        bool javaInit(JNIEnv *env);
        bool InitializeImpl(JNIEnv *env);
        Dreamland();
        ~Dreamland();

        static Dreamland *instance;
        JavaVM *java_vm;
        jclass java_main_class;
        jmethodID onSystemServerStart;
        jmethodID onAppProcessStart;

        static constexpr const char *DREAMLAND_BASE = "/data/dreamland/";
        static constexpr const char *DREAMLAND_CORE_JAR = "/system/framework/dreamland.jar";
        static constexpr const char *DREAMLAND_FLAG_DISABLE_FILE = "/data/dreamland/disable";
        DISALLOW_COPY_AND_ASSIGN(Dreamland);
    };
}

#endif //DREAMLAND_DREAMLAND_H
