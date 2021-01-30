//
// Created by canyie on 2019/11/11.
//

#ifndef DREAMLAND_LOG_H
#define DREAMLAND_LOG_H

#ifdef __cplusplus
extern "C" {
constexpr const char *LOG_TAG = "Dreamland";
#else
#define LOG_TAG "Dreamland"
#endif

#include <android/log.h>


#define LOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGF(...) __android_log_print(ANDROID_LOG_FATAL, LOG_TAG, __VA_ARGS__)

#define FATAL(...) do {\
LOGF("*** Runtime aborting because of fatal error: ");\
LOGF(__VA_ARGS__);\
LOGF("Aborting...");\
abort();\
} while(0)

#define FATAL_FOR_JNI(...) do {\
LOGF("*** Runtime aborting because of fatal error: ");\
LOGF(__VA_ARGS__);\
if(((env)->ExceptionCheck())) {\
LOGF("JNI ERROR: ");\
(env)->ExceptionDescribe();\
}\
env->FatalError("FATAL_FOR_JNI called.");\
} while(0)

#define CHECK(op, ...) do { if((!(op))) { FATAL(__VA_ARGS__); } } while(0)
#define CHECK_FOR_JNI(op, ...) do { if((!(op))) { FATAL_FOR_JNI(__VA_ARGS__); } } while(0)

#ifdef __cplusplus
}
#endif


#endif //DREAMLAND_LOG_H
