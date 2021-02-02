//
// Created by canyie on 2021/2/2.
//

#ifndef DREAMLAND_DEX_LOADER_H
#define DREAMLAND_DEX_LOADER_H

#include <jni.h>

namespace dreamland {
    class DexLoader {
    public:
        static void Prepare(JNIEnv* env);
        static jobject FromMemory(JNIEnv* env, char* dex, const size_t size);
        static jobject FromFile(JNIEnv* env, const char* path);

    private:
        static jclass PathClassLoader; // For Nougat only
        static jmethodID PathClassLoader_init;
        static jclass InMemoryDexClassLoader; // For Oreo+
        static jmethodID InMemoryDexClassLoader_init;
    };
}

#endif //DREAMLAND_DEX_LOADER_H
