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
        static constexpr int VERSION = 2;

        static bool ShouldDisable();

        static bool Prepare(JNIEnv* env);

        static Dreamland* GetInstance() {
            return instance;
        }

        static Dreamland* GetOrCreateInstance(JNIEnv* env) {
            if (instance == nullptr) {
                Prepare(env);
            }
            return instance;
        }

        static bool OnAppProcessStart(JNIEnv* env);

        static bool OnSystemServerStart(JNIEnv* env);

        JavaVM* GetJavaVM() {
            return java_vm;
        }

        JNIEnv* GetJNIEnv();

    private:
        bool javaInit(JNIEnv* env);

        bool InitializeImpl(JNIEnv* env);

        Dreamland();

        ~Dreamland();

        static Dreamland* instance;
        JavaVM* java_vm;
        jclass java_main_class;
        jmethodID onSystemServerStart;
        jmethodID onAppProcessStart;

        static constexpr const char* kBaseDir = "/data/misc/dreamland/";
        static constexpr const char* kCoreJarFile = "/system/framework/dreamland.jar";
        static constexpr const char* kDisableFile = "/data/misc/dreamland/disable";
        DISALLOW_COPY_AND_ASSIGN(Dreamland);
    };
}

#endif //DREAMLAND_DREAMLAND_H
