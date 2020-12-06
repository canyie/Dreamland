//
// Created by canyie on 2020/12/6.
//

#ifndef DREAMLAND_RIRU_H
#define DREAMLAND_RIRU_H

#ifdef __cplusplus
extern "C" {
#endif

#define EXPORT_C extern "C" __attribute__ ((visibility ("default"))) __attribute__((used))

typedef void(onModuleLoaded_v9)();

typedef int(shouldSkipUid_v9)(int uid);

typedef void(nativeForkAndSpecializePre_v9)(
        JNIEnv* env, jclass cls, jint* uid, jint* gid, jintArray* gids, jint* runtimeFlags,
        jobjectArray* rlimits, jint* mountExternal, jstring* seInfo, jstring* niceName,
        jintArray* fdsToClose, jintArray* fdsToIgnore, jboolean* is_child_zygote,
        jstring* instructionSet, jstring* appDataDir, jboolean* isTopApp,
        jobjectArray* pkgDataInfoList,
        jobjectArray* whitelistedDataInfoList, jboolean* bindMountAppDataDirs,
        jboolean* bindMountAppStorageDirs);

typedef void(nativeForkAndSpecializePost_v9)(JNIEnv* env, jclass cls, jint res);

typedef void(nativeForkSystemServerPre_v9)(
        JNIEnv* env, jclass cls, uid_t* uid, gid_t* gid, jintArray* gids, jint* runtimeFlags,
        jobjectArray* rlimits, jlong* permittedCapabilities, jlong* effectiveCapabilities);

typedef void(nativeForkSystemServerPost_v9)(JNIEnv* env, jclass cls, jint res);

typedef void(nativeSpecializeAppProcessPre_v9)(
        JNIEnv* env, jclass cls, jint* uid, jint* gid, jintArray* gids, jint* runtimeFlags,
        jobjectArray* rlimits, jint* mountExternal, jstring* seInfo, jstring* niceName,
        jboolean* startChildZygote, jstring* instructionSet, jstring* appDataDir,
        jboolean* isTopApp, jobjectArray* pkgDataInfoList, jobjectArray* whitelistedDataInfoList,
        jboolean* bindMountAppDataDirs, jboolean* bindMountAppStorageDirs);

typedef void(nativeSpecializeAppProcessPost_v9)(JNIEnv* env, jclass cls);

typedef struct {
    int supportHide;
    int version;
    const char* versionName;
    onModuleLoaded_v9* onModuleLoaded;
    shouldSkipUid_v9* shouldSkipUid;
    nativeForkAndSpecializePre_v9* forkAndSpecializePre;
    nativeForkAndSpecializePost_v9* forkAndSpecializePost;
    nativeForkSystemServerPre_v9* forkSystemServerPre;
    nativeForkSystemServerPost_v9* forkSystemServerPost;
    nativeSpecializeAppProcessPre_v9* specializeAppProcessPre;
    nativeSpecializeAppProcessPost_v9* specializeAppProcessPost;
} RiruModuleInfoV9;

typedef RiruModuleInfoV9 RiruModuleInfoV10;

typedef struct {
    // ...
} RiruApiV9;

typedef RiruApiV9 RiruApiV10;

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
 *   Returns the RiruModuleX struct created by the module (X is the return of the first call).
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
EXPORT_C void* init(void* arg);
extern int riru_api_version;
extern RiruApiV9* riru_api_v9;

#ifdef __cplusplus
}


#endif

#endif //DREAMLAND_RIRU_H
