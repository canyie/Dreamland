#include <cstdlib>
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
#include "dreamland/android.h"
#include "utils/scoped_local_ref.h"

using namespace dreamland;

int riru_api_version = 0;
bool disabled = false;
bool starting_child_zygote = false;
int uid_ = -1;
int* riru_allow_unload_ = nullptr;

void AllowUnload() {
    if (riru_allow_unload_) *riru_allow_unload_ = 1;
}

EXPORT void onModuleLoaded() {
    LOGI("Welcome to Dreamland %s (%d)!", Dreamland::VERSION_NAME, Dreamland::VERSION);
    disabled = Dreamland::ShouldDisable();
    if (UNLIKELY(disabled)) {
        LOGW("Dreamland framework should be disabled, do nothing.");
        return;
    }
    Android::Initialize();
    int api_level = Android::version;
    LOGI("Android Api Level %d", api_level);
    PineSetAndroidVersion(api_level);
    Dreamland::Prepare();
}

bool SkipThis() {
    return disabled || (uid_ != -1 && Dreamland::ShouldSkipUid(uid_));
}

EXPORT int shouldSkipUid(int uid) {
    return Dreamland::ShouldSkipUid(uid) ? 1 : 0;
}

static inline void Prepare(JNIEnv* env) {
    if (disabled || SkipThis()) return;
    Dreamland::ZygoteInit(env);
}

static inline void PostForkApp(JNIEnv* env, jint result) {
    if (result == 0) {
        bool allow_unload = true;
        if (!SkipThis()) {
            if (UNLIKELY(starting_child_zygote))  {
                // child zygote not allowed to do binder transaction
                LOGW("Skipping inject this process because it is child zygote");
            } else if (Dreamland::OnAppProcessStart(env)) {
                allow_unload = false;
            }
        }
        if (allow_unload) AllowUnload();
    }
}

static inline void PostForkSystemServer(JNIEnv* env, jint result) {
    if (result == 0 && !disabled) {
        Dreamland::OnSystemServerStart(env);
    }
}

EXPORT void nativeForkAndSpecializePre(JNIEnv* env, jclass, jint* uid_ptr, jint* gid_ptr,
                                       jintArray*, jint*, jobjectArray*, jint*, jstring*,
                                       jstring*, jintArray*, jintArray*,
                                       jboolean* is_child_zygote, jstring*, jstring*, jboolean*,
                                       jobjectArray*) {
    Prepare(env);
    starting_child_zygote = *is_child_zygote;
}

EXPORT int nativeForkAndSpecializePost(JNIEnv* env, jclass, jint result) {
    PostForkApp(env, result);
    return 0;
}

EXPORT void nativeForkSystemServerPre(JNIEnv* env, jclass, uid_t*, gid_t*,
                                      jintArray*, jint*, jobjectArray*, jlong*, jlong*) {
    Prepare(env);
}

EXPORT int nativeForkSystemServerPost(JNIEnv* env, jclass, jint result) {
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
    uid_ = *_uid;
}

static void forkAndSpecializePost(JNIEnv *env, jclass, jint res) {
    PostForkApp(env, res);
    uid_ = -1;
}

static void specializeAppProcessPre(
        JNIEnv *env, jclass, jint *_uid, jint *gid, jintArray *gids, jint *runtimeFlags,
        jobjectArray *rlimits, jint *mountExternal, jstring *seInfo, jstring *niceName,
        jboolean *startChildZygote, jstring *instructionSet, jstring *appDataDir,
        jboolean *isTopApp, jobjectArray *pkgDataInfoList, jobjectArray *whitelistedDataInfoList,
        jboolean *bindMountAppDataDirs, jboolean *bindMountAppStorageDirs) {
    Prepare(env);
    starting_child_zygote = *startChildZygote;
    uid_ = *_uid;
}

static void specializeAppProcessPost(JNIEnv *env, jclass) {
    PostForkApp(env, 0);
    uid_ = -1;
}

static void forkSystemServerPre(
        JNIEnv *env, jclass, uid_t *uid, gid_t *gid, jintArray *gids, jint *runtimeFlags,
        jobjectArray *rlimits, jlong *permittedCapabilities, jlong *effectiveCapabilities) {
    Prepare(env);
}

static void forkSystemServerPost(JNIEnv *env, jclass, jint res) {
    PostForkSystemServer(env, res);
}

static auto module = RiruVersionedModuleInfo {
        .moduleApiVersion = RIRU_NEW_MODULE_API_VERSION,
        .moduleInfo = RiruModuleInfo {
                .supportHide = true,
                .version = Dreamland::VERSION,
                .versionName = Dreamland::VERSION_NAME,
                .onModuleLoaded = onModuleLoaded,
                .shouldSkipUid = shouldSkipUid,
                .forkAndSpecializePre = forkAndSpecializePre,
                .forkAndSpecializePost = forkAndSpecializePost,
                .forkSystemServerPre = forkSystemServerPre,
                .forkSystemServerPost = forkSystemServerPost,
                .specializeAppProcessPre = specializeAppProcessPre,
                .specializeAppProcessPost = specializeAppProcessPost
        }
};

static int step = 0;
EXPORT void* init(Riru* arg) {
    step++;

    switch (step) {
        case 1: {
            int core_max_api_version = arg->riruApiVersion;
            riru_api_version = core_max_api_version <= RIRU_NEW_MODULE_API_VERSION ? core_max_api_version : RIRU_NEW_MODULE_API_VERSION;
            if (riru_api_version > 10 && riru_api_version < 25) {
                // V24 is pre-release version, not supported
                riru_api_version = 10;
            }
            if (riru_api_version >= 25) {
                module.moduleApiVersion = riru_api_version;
                riru_allow_unload_ = arg->allowUnload;
                return &module;
            } else {
                return &riru_api_version;
            }
        }
        case 2: {
            return &module.moduleInfo;
        }
        case 3:
        default:
            return nullptr;
    }
}
