//
// Created by canyie on 2022/2/28.
//

#ifndef DREAMLAND_FLAVOR_H
#define DREAMLAND_FLAVOR_H

#include <jni.h>

namespace dreamland {
    class Flavor {
    public:
        static void OnModuleLoaded(bool zygote);

        static bool IsDisabled();

        // Return true if the process should be skipped
        static bool ShouldSkip(bool is_child_zygote, int uid);

        static void PreFork(JNIEnv* env, bool zygote);
        static void PostForkSystemServer(JNIEnv* env);

        // Return true if we cannot unload our library in the current process
        static bool PostForkApp(JNIEnv* env, bool main_zygote);
    };
}

#endif //DREAMLAND_FLAVOR_H
