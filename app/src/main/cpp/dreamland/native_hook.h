//
// Created by canyie on 2024/2/5.
//

#ifndef DREAMLAND_NATIVE_HOOK_H
#define DREAMLAND_NATIVE_HOOK_H

#include <jni.h>

namespace dreamland {
    class NativeHook {
    public:
        static void RegisterNatives(JNIEnv* env, jclass main);
    };
} // dreamland

#endif //DREAMLAND_NATIVE_HOOK_H
