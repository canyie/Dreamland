//
// Created by canyie on 2020/7/3.
//

#ifndef DREAMLAND_PINE_H
#define DREAMLAND_PINE_H

#include <jni.h>

extern "C" {
    bool register_Pine(JNIEnv* env, jclass Pine);
    bool register_Ruler(JNIEnv* env, jclass Ruler);
    void PineSetAndroidVersion(int version);
    void* PineOpenElf(const char* elf);
    void PineCloseElf(void* handle);
    void* PineGetElfSymbolAddress(void* handle, const char* symbol);
    bool PineNativeInlineHookSymbolNoBackup(const char* elf, const char* symbol, void* replace);
    void PineNativeInlineHookFuncNoBackup(void* target, void* replace);
};

#endif //DREAMLAND_PINE_H
