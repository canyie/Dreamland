#include <jni.h>
#include <unistd.h>
#include <dlfcn.h>
#include <cerrno>
#include <cstring>
#include <asm/fcntl.h>
#include <fcntl.h>
#include "pine.h"
#include "riru.h"
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

extern "C" {
int riru_api_version = 0;
RiruApiV9* riru_api_v9;
}

bool disabled = false;
bool starting_child_zygote = false;
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

EXPORT_C void onModuleLoaded() {
    LOGI("Welcome to Dreamland v%d!", Dreamland::VERSION);
    disabled = Dreamland::ShouldDisable();
    if (UNLIKELY(disabled)) {
        LOGW("Dreamland framework should be disabled, do nothing.");
        return;
    }
    Android::Initialize();
    int api_level = Android::version;
    LOGI("Android Api Level %d", api_level);
    PineSetAndroidVersion(api_level);

    if (riru_api_version >= 9) {
        // After Riru API V9, we're loaded after libart.so be loaded
        Android::DisableOnlyUseSystemOatFiles();
    } else {
        // Before Riru API V9
        // At this time, libart.so has not been loaded yet (it will be dlopen-ed in JniInvocation::Init)
        xhook_enable_debug(1);
        xhook_enable_sigsegv_protection(0);
        bool success = xhook_register(".*\\libandroid_runtime.so$", "JNI_CreateJavaVM",
                                      reinterpret_cast<void*> (hook_JNI_CreateJavaVM),
                                      reinterpret_cast<void**> (&orig_JNI_CreateJavaVM)) == 0
                       && xhook_refresh(0) == 0;

        if (LIKELY(success)) {
            xhook_clear();
        } else {
            LOGE("Failed to hook JNI_CreateJavaVM");
        }
    }

}

EXPORT_C int shouldSkipUid(int uid) {
    if (uid == 1000/*SYSTEM_UID*/) return 0;

    // Skip non-normal app process (e.g. isolated process, relro updater and webview zygote).
    int app_id = uid % 100000;
    return app_id >= 10000 && app_id <= 19999 ? 0 : 1;
}

static inline void Prepare(JNIEnv* env) {
    if (disabled) return;
    Dreamland::Prepare(env);
}

static inline void PostForkApp(JNIEnv* env, jint result) {
    if (result == 0 && !disabled) {
        if (UNLIKELY(starting_child_zygote))
            LOGW("Skipping inject this process because it is child zygote");
        else
            Dreamland::OnAppProcessStart(env);
    }
}

static inline void PostForkSystemServer(JNIEnv* env, jint result) {
    if (result == 0 && !disabled) {
        Dreamland::OnSystemServerStart(env);
    }
}

EXPORT_C void nativeForkAndSpecializePre(JNIEnv* env, jclass, jint* uid_ptr, jint* gid_ptr,
                                         jintArray*, jint*, jobjectArray*, jint*, jstring*,
                                         jstring*, jintArray*, jintArray*,
                                         jboolean* is_child_zygote, jstring*, jstring*, jboolean*,
                                         jobjectArray*) {
    Prepare(env);
    starting_child_zygote = *is_child_zygote;
}

EXPORT_C int nativeForkAndSpecializePost(JNIEnv* env, jclass, jint result) {
    PostForkApp(env, result);
    return 0;
}

EXPORT_C void nativeForkSystemServerPre(JNIEnv* env, jclass, uid_t*, gid_t*,
                                        jintArray*, jint*, jobjectArray*, jlong*, jlong*) {
    Prepare(env);
}

EXPORT_C int nativeForkSystemServerPost(JNIEnv* env, jclass, jint result) {
    PostForkSystemServer(env, result);
    return 0;
}

// ----------- Riru V22+ API -----------

static void forkAndSpecializePre(
        JNIEnv *env, jclass, jint *_uid, jint *gid, jintArray *gids, jint *runtimeFlags,
        jobjectArray *rlimits, jint *mountExternal, jstring *seInfo, jstring *niceName,
        jintArray *fdsToClose, jintArray *fdsToIgnore, jboolean *is_child_zygote,
        jstring *instructionSet, jstring *appDataDir, jboolean *isTopApp, jobjectArray *pkgDataInfoList,
        jobjectArray *whitelistedDataInfoList, jboolean *bindMountAppDataDirs, jboolean *bindMountAppStorageDirs) {
    Prepare(env);
    starting_child_zygote = *is_child_zygote;
}

static void forkAndSpecializePost(JNIEnv *env, jclass, jint res) {
    PostForkApp(env, res);
}

static void specializeAppProcessPre(
        JNIEnv *env, jclass, jint *_uid, jint *gid, jintArray *gids, jint *runtimeFlags,
        jobjectArray *rlimits, jint *mountExternal, jstring *seInfo, jstring *niceName,
        jboolean *startChildZygote, jstring *instructionSet, jstring *appDataDir,
        jboolean *isTopApp, jobjectArray *pkgDataInfoList, jobjectArray *whitelistedDataInfoList,
        jboolean *bindMountAppDataDirs, jboolean *bindMountAppStorageDirs) {
    // added from Android 10, but disabled at least in Google Pixel devices
    Prepare(env);
    starting_child_zygote = *startChildZygote;
}

static void specializeAppProcessPost(JNIEnv *env, jclass) {
    // added from Android 10, but disabled at least in Google Pixel devices
    // PostForkApp(env, 0);
}

static void forkSystemServerPre(
        JNIEnv *env, jclass, uid_t *uid, gid_t *gid, jintArray *gids, jint *runtimeFlags,
        jobjectArray *rlimits, jlong *permittedCapabilities, jlong *effectiveCapabilities) {
    Prepare(env);
}

static void forkSystemServerPost(JNIEnv *env, jclass, jint res) {
    PostForkSystemServer(env, res);
}

/*
 * Init will be called three times.
 *
 * The first time:
 *   Returns the highest version number supported by both Riru and the module.
 *
 *   arg: (int *) Riru's API version
 *   returns: (int *) the highest possible API version
 *
 * The second time:
 *   Returns the RiruModuleX struct created by the module.
 *   (X is the return of the first call)
 *
 *   arg: (RiruApiVX *) RiruApi strcut, this pointer can be saved for further use
 *   returns: (RiruModuleX *) RiruModule strcut
 *
 * The second time:
 *   Let the module to cleanup (such as RiruModuleX struct created before).
 *
 *   arg: null
 *   returns: (ignored)
 *
 */
EXPORT_C void* init(void* arg) {
    static int step = 0;
    step++;

    static void *_module;

    switch (step) {
        case 1: {
            int core_max_api_version = *static_cast<int*>(arg);
            riru_api_version = core_max_api_version <= RIRU_NEW_MODULE_API_VERSION ? core_max_api_version : RIRU_NEW_MODULE_API_VERSION;
            return &riru_api_version;
        }
        case 2: {
            switch (riru_api_version) {
                // RiruApiV10 and RiruModuleInfoV10 are equal to V9
                case 10:
                case 9: {
                    riru_api_v9 = (RiruApiV9 *) arg;

                    auto module = (RiruModuleInfoV9 *) malloc(sizeof(RiruModuleInfoV9));
                    memset(module, 0, sizeof(RiruModuleInfoV9));
                    _module = module;

                    module->supportHide = true;

                    module->version = Dreamland::VERSION;
                    module->versionName = RIRU_MODULE_VERSION_NAME;
                    module->onModuleLoaded = onModuleLoaded;
                    module->shouldSkipUid = shouldSkipUid;
                    module->forkAndSpecializePre = forkAndSpecializePre;
                    module->forkAndSpecializePost = forkAndSpecializePost;
                    module->specializeAppProcessPre = specializeAppProcessPre;
                    module->specializeAppProcessPost = specializeAppProcessPost;
                    module->forkSystemServerPre = forkSystemServerPre;
                    module->forkSystemServerPost = forkSystemServerPost;
                    return module;
                }
                default: {
                    return nullptr;
                }
            }
        }
        case 3: {
            free(_module);
            return nullptr;
        }
        default:
            return nullptr;
    }
}