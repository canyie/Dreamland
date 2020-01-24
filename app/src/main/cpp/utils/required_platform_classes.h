//
// Created by canyie on 2019/11/28.
//

#ifndef DREAMLAND_REQUIRED_PLATFORM_CLASSES_H
#define DREAMLAND_REQUIRED_PLATFORM_CLASSES_H

#include "jni.h"

namespace dreamland {
    class RequiredPlatformClasses {
    public:
        static bool onRegisterNativeMethods(JNINativeMethod method) {

        }
/*
    private:
        static bool registered_android_app_ActivityThread = false;
        static bool registered_android_util_Log = false;
        static bool registered_android_os_Process = false;
        static bool registered_android_os_SystemProperties = false;
        static bool registered_android_os_Binder = false;
        static bool registered_android_os_Parcel = false;
        static bool registered_com_android_internal_os_Zygote = false;
        static bool registered_com_android_internal_os_ZygoteInit = false;
        */
    };
}

#endif //DREAMLAND_REQUIRED_PLATFORM_CLASSES_H
