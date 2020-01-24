#include <jni.h>
#include <unistd.h>
#include <dlfcn.h>
#include <cerrno>
#include <cstring>

#include "utils/log.h"
#include "utils/macros.h"
#include "dreamland/dreamland.h"
#include "dreamland/override_jni_methods.h"
#include "utils/well_known_classes.h"
#include "utils/selinux.h"
#include "utils/selinux_helper.h"
#include "external/xhook/xhook.h"

using namespace dreamland;

static int
(*orig_jniRegisterNativeMethods)(JNIEnv *, const char *, const JNINativeMethod *, int) = nullptr;

static constexpr const char *requiredPlatformClasses[] = {
    "android/app/ActivityThread",
    "android/util/Log",
    "android/os/Process",
    "android/os/SELinux",
    "android/os/SystemProperties",
    "android/os/SystemClock",
    "android/os/Binder",
    "android/os/Parcel",
    "com/android/internal/os/Zygote",
    // "com/android/internal/os/ZygoteInit"
};
static int rc = 0;

extern "C" JNIEXPORT void JNICALL
Java_com_canyie_dreamland_MainActivity_testNativeHook(
        JNIEnv *env,
        jobject) {
}

void onRuntimeReady(JNIEnv *env) {
    LOGI("The runtime is already.");
    if (LIKELY(Dreamland::Initialize(env))) {
        LOGI("Dreamland framework is initialized.");
    } else {
        LOGE("Dreamland framework initialize failed. Do nothing.");
    }
}


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

void onAppProcessStarted(JNIEnv *env, int uid, jstring java_nice_name) {
    LOGI("App process started; pid %d uid %d", getpid(), uid);
    clearHook();
    Dreamland::OnAppProcessStart(java_nice_name);
}

void onSystemServerStarted(JNIEnv *env) {
    LOGI("System server started; pid %d", getpid());
    clearHook();
    Dreamland::OnSystemServerStart();
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
    if(rc >= 0) {
        for (int i = 0; i < NELEM(requiredPlatformClasses); ++i) {
            if (strcmp(className, requiredPlatformClasses[i]) == 0) {
                LOGI("Registering native methods for class %s (required platform class) ", className);
                rc++;
                if (strcmp(className, "com/android/internal/os/Zygote") == 0) {
                    newMethods = onRegisterZygote(env, methods, numMethods);
                }
                break;
            }
        }
    }

    int result = orig_jniRegisterNativeMethods(env, className, newMethods != nullptr ? newMethods : methods, numMethods);
    delete newMethods;
    if(rc == NELEM(requiredPlatformClasses) && result >= 0) {
        rc = -1;
        onRuntimeReady(env);
    }
    return result;
}

extern __attribute__((constructor))  void onLibraryLoad() {
    LOGI("libdreamland.so loaded in process: uid = %d, pid = %d", getuid(), getpid());
    LOGI("Dreamland version %d", Dreamland::VERSION);
    bool is_selinux_enforced = false;
    bool changed_selinux_status = false;

    // 1. try set SELinux mode to permissive

/*
    if(SELinuxHelper::InitOnce()) {
        bool is_selinux_enabled = SELinuxHelper::IsEnabled();
        if (is_selinux_enabled) {
            LOGI("SELinux is enabled.");
            is_selinux_enforced = SELinuxHelper::IsEnforced();
            if (is_selinux_enforced) {
                LOGI("SELinux is enforcing. try set mode to permissive...");
                bool success = SELinuxHelper::SetEnforce(false);
                if (success) {
                    if (!SELinuxHelper::IsEnforced()) {
                        LOGE("security_setenforce(0) returned success but SELinux is enforcing.");
                    } else {
                        LOGI("Changed SELinux mode to permissive!");
                        changed_selinux_status = true;
                    }
                } else {
                    LOGE("security_setenforce(0) failed...");
                }
            } else {
                LOGI("SELinux is permissive.");
            }
        } else {
            LOGI("SELinux is disabled.");
        }
    }
*/

    /*if(is_selinux_enabled()) {
        LOGI("SELinux is enabled.");
        is_selinux_enforced = security_getenforce() == 1;
        if(is_selinux_enforced) {
            LOGI("SELinux mode: enforcing. try set to permissive");
            if(security_setenforce(0) == 0) {
                if(security_getenforce() == 0) {
                    LOGI("changed selinux mode to permissive!");
                    changed_selinux_status = true;
                } else {
                    LOGE("security_setenforce returned success but SELinux is enforcing. ");
                }
            } else {
                LOGE("security_setenforce failed...");
            }
        } else {
            LOGI("SELinux is permissive.");
        }
    } else {
        LOGI("SELinux is disabled.");
    }*/

    if (UNLIKELY(Dreamland::ShouldDisable())) {
/*        if(changed_selinux_status) {
            // Reset SELinux mode if framework is disabled.
            if(!SELinuxHelper::SetEnforce(is_selinux_enforced)) {
                LOGE("Reset SELinux mode failed...");
            }
        }
*/
        return;
    }


    setOnAppProcessStartListener(onAppProcessStarted);
    setOnSystemServerStartListener(onSystemServerStarted);
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
        LOGD("hooked jniRegisterNativeMethods(); orig_jniRegisterNativeMethods = 0x%x", orig_jniRegisterNativeMethods);
    } else {
        LOGE("Failed to hook jniRegisterNativeMethods");
    }
}












