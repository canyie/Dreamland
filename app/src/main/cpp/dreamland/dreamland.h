//
// Created by canyie on 2019/11/12.
//

#ifndef DREAMLAND_DREAMLAND_H
#define DREAMLAND_DREAMLAND_H

#include <vector>
#include <jni.h>
#include "../utils/log.h"
#include "../utils/macros.h"
#include "android.h"

namespace dreamland {
    class Dreamland {
    public:
        static constexpr const char* VERSION_NAME = RIRU_MODULE_VERSION_NAME;
        static constexpr int VERSION = DREAMLAND_VERSION_CODE;

        static bool ShouldDisable();

        static void Prepare(bool preload);

        static bool PrepareJava(JNIEnv* env, bool zygote);

        static Dreamland* GetInstance() {
            return instance;
        }

        static bool ShouldSkipUid(int uid) {
            // TODO: Get these uids through java world
            int app_id = uid % 100000;
            if (UNLIKELY(app_id >= 90000)) {
                // Isolated process
                return true;
            }
            if (UNLIKELY(app_id == 1037)) {
                // RELRO updater
                return true;
            }
            if (Android::version >= Android::kO) {
                uid_t kWebViewZygoteUid = Android::version >= Android::kP ? 1053 : 1051;
                if (UNLIKELY(uid == kWebViewZygoteUid)) {
                    // WebView zygote
                    return true;
                }
            }
            return false;
        }

        static bool OnAppProcessStart(JNIEnv* env, bool start_system_server);

        static bool OnSystemServerStart(JNIEnv* env);

        JavaVM* GetJavaVM() {
            return java_vm;
        }

        JNIEnv* GetJNIEnv();

    private:
        bool LoadDexDirectly(JNIEnv* env, bool zygote);

        bool PrepareJavaImpl(JNIEnv* env, bool preload);

        bool RegisterNatives(JNIEnv* env, jclass main_class, jobject class_loader);

        bool FindEntryMethods(JNIEnv* env, jclass main_class, bool app, bool system_server);

        bool EnsureDexLoaded(JNIEnv* env, bool app);

        bool LoadDexFromMemory(JNIEnv* env, bool app);

        static void PreloadDexData();

        Dreamland();

        ~Dreamland();

        static Dreamland* instance;
        static std::vector<char>* dex_data;
        JavaVM* java_vm;
        jclass java_main_class;
        jmethodID onSystemServerStart;
        jmethodID onAppProcessStart;

        static constexpr const char* kBaseDir = "/data/misc/dreamland/";
        static constexpr const char* kCoreJarFile = "/data/misc/dreamland/dreamland.jar";
        static constexpr const char* kDisableFile = "/data/misc/dreamland/disable";
        DISALLOW_COPY_AND_ASSIGN(Dreamland);
    };
}

#endif //DREAMLAND_DREAMLAND_H
