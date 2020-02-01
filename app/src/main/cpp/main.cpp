#include <jni.h>
#include <unistd.h>
#include <dlfcn.h>
#include <cerrno>
#include <cstring>

#include "utils/log.h"
#include "utils/macros.h"
#include "dreamland/dreamland.h"
#include "utils/well_known_classes.h"
#include "utils/selinux.h"
#include "utils/selinux_helper.h"
#include "external/xhook/xhook.h"

using namespace dreamland;

static int
(*orig_jniRegisterNativeMethods)(JNIEnv *, const char *, const JNINativeMethod *, int) = nullptr;

static void (*orig_nativeZygoteInit)(JNIEnv *, jobject) = nullptr;

void clearHook() {
    xhook_register(".*\\libandroid_runtime.so$", "jniRegisterNativeMethods",
                   reinterpret_cast<void *> (orig_jniRegisterNativeMethods),
                   nullptr);
    if(xhook_refresh(0) == 0) {
        xhook_clear();
        LOGD("Reset function jniRegisterNativeMethods() success.");
    } else {
        LOGE("Reset function jniRegisterNativeMethods() failed...");
    }
}

extern void fake_nativeZygoteInit(JNIEnv *env, jobject clazz) {
    orig_nativeZygoteInit(env, clazz);
    clearHook();
    if (UNLIKELY(Dreamland::ShouldDisable())) {
        return;
    }
    if (UNLIKELY(!Dreamland::Initialize(env))) {
        LOGE("Dreamland framework init failed.");
        return;
    }
    Dreamland::OnAppProcessStart();
}

int hook_jniRegisterNativeMethods(JNIEnv *env, const char *className,
                                  JNINativeMethod *methods, int numMethods) {
    /*
    JNINativeMethod *newMethods = nullptr;
    if(strcmp(className, "com/android/internal/os/Zygote") == 0) {
        LOGD("Registering native methods of class com.android.internal.os.Zygote");
        newMethods = onRegisterZygote(env, methods, numMethods);
    }
    int result = orig_jniRegisterNativeMethods(env, className, newMethods != nullptr ? newMethods : methods, numMethods);
    delete newMethods;
    return result;
     */
    JNINativeMethod *newMethods = nullptr;

    if (strcmp(className, "com/android/internal/os/RuntimeInit") == 0) {
        // may 7.x
        newMethods = new JNINativeMethod[numMethods];
        memcpy(newMethods, methods, sizeof(JNINativeMethod) * numMethods);

        for (int i = 0;i < numMethods;i++) {
            JNINativeMethod method = methods[i];
            if (strcmp(method.name, "nativeZygoteInit") == 0) {
                if (strcmp(method.signature, "()V") == 0) {
                    LOGI("Replaced RuntimeInit.nativeZygoteInit");
                    orig_nativeZygoteInit =
                            reinterpret_cast<void (*)(JNIEnv *, jobject)>(method.fnPtr);
                    newMethods[i].fnPtr = reinterpret_cast<void *>(fake_nativeZygoteInit);
                    break;
                } else {
                    LOGW("Found RuntimeInit.nativeZygoteInit but signature %s mismatch", method.signature);
                }
            }
        }
    } else if (strcmp(className, "com/android/internal/os/ZygoteInit") == 0) {
        // may 8.0+
        newMethods = new JNINativeMethod[numMethods];
        memcpy(newMethods, methods, sizeof(JNINativeMethod) * numMethods);

        for (int i = 0;i < numMethods;i++) {
            JNINativeMethod method = methods[i];
            if (strcmp(method.name, "nativeZygoteInit") == 0) {
                if (strcmp(method.signature, "()V") == 0) {
                    LOGI("Replaced ZygoteInit.nativeZygoteInit");
                    orig_nativeZygoteInit =
                            reinterpret_cast<void (*)(JNIEnv *, jobject)>(method.fnPtr);
                    newMethods[i].fnPtr = reinterpret_cast<void *>(fake_nativeZygoteInit);
                    break;
                } else {
                    LOGW("Found ZygoteInit.nativeZygoteInit but signature %s mismatch", method.signature);
                }
            }
        }
    }

    int result = orig_jniRegisterNativeMethods(env, className, newMethods != nullptr ? newMethods : methods, numMethods);
    delete newMethods;
    return result;
}

extern __attribute__((constructor))  void onLibraryLoad() {
    LOGI("libdreamland.so loaded in process: uid = %d, pid = %d", getuid(), getpid());
    LOGI("Dreamland version %d", Dreamland::VERSION);

    /*if (UNLIKELY(Dreamland::ShouldDisable())) {
        return;
    }*/

    xhook_enable_debug(1);
    xhook_enable_sigsegv_protection(0);
    bool success = xhook_register(".*\\libandroid_runtime.so$", "jniRegisterNativeMethods",
                                  reinterpret_cast<void *> (hook_jniRegisterNativeMethods),
                                  reinterpret_cast<void **> (&orig_jniRegisterNativeMethods)) == 0;
    if (success) {
        success = xhook_refresh(0) == 0;
    }
    if (success) {
        xhook_clear();
        LOGD("hooked jniRegisterNativeMethods(); orig_jniRegisterNativeMethods = %#x", orig_jniRegisterNativeMethods);
    } else {
        LOGE("Failed to hook jniRegisterNativeMethods");
    }
}












