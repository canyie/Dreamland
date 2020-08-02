#include <jni.h>
#include <unistd.h>
#include <dlfcn.h>
#include <cerrno>
#include <cstring>
#include <asm/fcntl.h>
#include <fcntl.h>
#include "pine.h"
#include "utils/log.h"
#include "utils/macros.h"
#include "dreamland/dreamland.h"
#include "utils/well_known_classes.h"
#include "utils/selinux.h"
#include "utils/selinux_helper.h"
#include "external/xhook/xhook.h"
#include "dreamland/android.h"
#include "utils/scoped_local_ref.h"

using namespace dreamland;

bool disabled = false;
jint (*orig_JNI_CreateJavaVM)(JavaVM**, JNIEnv**, void*) = nullptr;

jint hook_JNI_CreateJavaVM(JavaVM** p_vm, JNIEnv** p_env, void* vm_args) {
    Android::DisableOnlyUseSystemOatFiles();
    bool success = xhook_register(".*\\libandroid_runtime.so$", "JNI_CreateJavaVM",
                                  reinterpret_cast<void*>(orig_JNI_CreateJavaVM),
                                  nullptr) == 0 && xhook_refresh(0) == 0;
    if (LIKELY(success)) {
        xhook_clear();
    } else {
        LOGE("Failed to clear hook.");
    }
    // After JNI_CreateJavaVM returns, we already have a valid JNIEnv and can call some Java APIs
    // But many important APIs are not yet ready (such as JNI function is not registered).
    return orig_JNI_CreateJavaVM(p_vm, p_env, vm_args);
}

#define EXPORT_C extern "C" __attribute__ ((visibility ("default"))) __attribute__((used))

EXPORT_C void onModuleLoaded() {
    LOGI("Welcome to Dreamland v%d!", Dreamland::VERSION);
    disabled = Dreamland::ShouldDisable();
    if (UNLIKELY(disabled)) {
        LOGW("Dreamland framework should disable, do nothing.");
        return;
    }
    Android::Initialize();
    int api_level = Android::version;
    LOGI("Android Api Level %d", api_level);
    PineSetAndroidVersion(api_level);

    // At this time, libart.so has not been loaded yet (it will be dlopened in JniInvocation::Init)
    xhook_enable_debug(1);
    xhook_enable_sigsegv_protection(0);
    bool success = xhook_register(".*\\libandroid_runtime.so$", "JNI_CreateJavaVM",
                                  reinterpret_cast<void*> (hook_JNI_CreateJavaVM),
                                  reinterpret_cast<void**> (&orig_JNI_CreateJavaVM)) == 0 &&
                   xhook_refresh(0) == 0;

    if (LIKELY(success)) {
        xhook_clear();
    } else {
        LOGE("Failed to hook JNI_CreateJavaVM");
    }
}

EXPORT_C void nativeForkAndSpecializePre(JNIEnv* env, jclass, jint* uid_ptr, jint* gid_ptr,
                                         jintArray*, jint*, jobjectArray*, jint*, jstring*,
                                         jstring*, jintArray*, jintArray*,
                                         jboolean*, jstring*, jstring*, jboolean*, jobjectArray*) {
    if (disabled) return;
    Dreamland::Prepare(env);
}

EXPORT_C int nativeForkAndSpecializePost(JNIEnv* env, jclass, jint result) {
    if (result == 0 && !disabled) {
        Dreamland::OnAppProcessStart(env);
    }
    return 0;
}

EXPORT_C void nativeForkSystemServerPre(JNIEnv* env, jclass, uid_t*, gid_t*,
                                        jintArray*, jint*, jobjectArray*, jlong*, jlong*) {
    if (disabled) return;
    Dreamland::Prepare(env);
}

EXPORT_C int nativeForkSystemServerPost(JNIEnv* env, jclass, jint result) {
    if (result == 0 && !disabled) {
        Dreamland::OnSystemServerStart(env);
    }
    return 0;
}