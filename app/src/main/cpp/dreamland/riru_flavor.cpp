//
// Created by canyie on 2022/2/28.
//

#include "dreamland.h"
#include "flavor.h"
#include "../riru.h"

using namespace dreamland;

static int riru_api_version = 0;
static int* riru_allow_unload_ = nullptr;
static bool requested_start_system_server_ = false;

// Skip incomplete fork (post fork happens before pre fork)
static bool skip_ = true;

static void AllowUnload() {
    if (riru_allow_unload_) *riru_allow_unload_ = 1;
}

EXPORT void onModuleLoaded() {
    Flavor::OnModuleLoaded(true);
}

static void PostForkApp(JNIEnv* env) {
    bool allow_unload = skip_ || !Flavor::PostForkApp(env, requested_start_system_server_);
    if (allow_unload)
        AllowUnload();
}

EXPORT int shouldSkipUid(int uid) {
    return Dreamland::ShouldSkipUid(uid) ? 1 : 0;
}

EXPORT void nativeForkAndSpecializePre(JNIEnv* env, jclass, jint* uid_ptr, jint*,
                                       jintArray*, jint*, jobjectArray*, jint*, jstring*,
                                       jstring*, jintArray*, jintArray*,
                                       jboolean* is_child_zygote, jstring*, jstring*, jboolean*,
                                       jobjectArray*) {
    if (skip_ = Flavor::ShouldSkip(*is_child_zygote, *uid_ptr); !skip_) {
        Flavor::PreFork(env, true);
    }
}

EXPORT int nativeForkAndSpecializePost(JNIEnv* env, jclass, jint result) {
    if (result == 0) PostForkApp(env);
    else skip_ = true;
    return 0;
}

EXPORT void nativeForkSystemServerPre(JNIEnv* env, jclass, uid_t*, gid_t*,
                                      jintArray*, jint*, jobjectArray*, jlong*, jlong*) {
    requested_start_system_server_ = true;
    // Only skip system server when we are disabled
    if (LIKELY(!Flavor::IsDisabled())) Flavor::PreFork(env, true);
}

EXPORT int nativeForkSystemServerPost(JNIEnv* env, jclass, jint result) {
    if (result == 0 && !Flavor::IsDisabled()) Flavor::PostForkSystemServer(env);
    return 0;
}

// ----------- Riru V22+ API -----------

static void forkAndSpecializePre(
        JNIEnv *env, jclass, jint *_uid, jint *gid, jintArray *gids, jint *runtimeFlags,
        jobjectArray *rlimits, jint *mountExternal, jstring *seInfo, jstring *niceName,
        jintArray *fdsToClose, jintArray *fdsToIgnore, jboolean *is_child_zygote,
        jstring *instructionSet, jstring *appDataDir, jboolean *isTopApp, jobjectArray *pkgDataInfoList,
        jobjectArray *whitelistedDataInfoList, jboolean *bindMountAppDataDirs, jboolean *bindMountAppStorageDirs) {
    if (skip_ = Flavor::ShouldSkip(*is_child_zygote, *_uid); !skip_) {
        Flavor::PreFork(env, true);
    }
}

static void forkAndSpecializePost(JNIEnv *env, jclass, jint res) {
    if (res == 0) PostForkApp(env);
    else skip_ = true;
}

static void specializeAppProcessPre(
        JNIEnv *env, jclass, jint *_uid, jint *gid, jintArray *gids, jint *runtimeFlags,
        jobjectArray *rlimits, jint *mountExternal, jstring *seInfo, jstring *niceName,
        jboolean *startChildZygote, jstring *instructionSet, jstring *appDataDir,
        jboolean *isTopApp, jobjectArray *pkgDataInfoList, jobjectArray *whitelistedDataInfoList,
        jboolean *bindMountAppDataDirs, jboolean *bindMountAppStorageDirs) {
    if (skip_ = Flavor::ShouldSkip(*startChildZygote, *_uid); !skip_) {
        Flavor::PreFork(env, false);
    }
}

static void specializeAppProcessPost(JNIEnv *env, jclass) {
    PostForkApp(env);
}

static void forkSystemServerPre(
        JNIEnv *env, jclass, uid_t *uid, gid_t *gid, jintArray *gids, jint *runtimeFlags,
        jobjectArray *rlimits, jlong *permittedCapabilities, jlong *effectiveCapabilities) {
    requested_start_system_server_ = true;
    // Only skip system server when we are disabled
    if (LIKELY(!Flavor::IsDisabled())) Flavor::PreFork(env, true);
}

static void forkSystemServerPost(JNIEnv *env, jclass, jint res) {
    if (res == 0 && !Flavor::IsDisabled()) Flavor::PostForkSystemServer(env);
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
