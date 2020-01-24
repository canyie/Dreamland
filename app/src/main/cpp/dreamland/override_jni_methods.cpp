//
// Created by canyie on 2019/11/21.
//

#include <cstring>
#include "override_jni_methods.h"
#include "../utils/log.h"


void *forkAppProcess = nullptr;
void *forkSystemServer = nullptr;
OnAppProcessStartListener onAppProcessStart = nullptr;
OnSystemServerStartListener onSystemServerStart = nullptr;

void setForkAppProcessFunc(void *addr) {
    forkAppProcess = addr;
}

void setForkSystemServerFunc(void *addr) {
    forkSystemServer = addr;
}

void setOnAppProcessStartListener(OnAppProcessStartListener listener) {
    onAppProcessStart = listener;
}

void setOnSystemServerStartListener(OnSystemServerStartListener listener) {
    onSystemServerStart = listener;
}

void beforeForkAppProcess(JNIEnv *env, int uid, jstring java_nice_name) {
}

void afterForkAppProcess(JNIEnv *env, int uid, int pid, jstring java_nice_name) {
    if(pid != 0) {
        // We are in zygote process or fork() failed.
        return;
    }
    onAppProcessStart(env, uid, java_nice_name);
}

void beforeForkSystemServer(JNIEnv *env) {
}

void afterForkSystemServer(JNIEnv *env, int pid) {
    if(pid != 0) {
        // We are in zygote process or fork() failed.
        return;
    }
    onSystemServerStart(env);
}

jint nativeForkAndSpecialize_marshmallow(
        JNIEnv *env, jclass clazz, jint uid, jint gid, jintArray gids, jint debug_flags,
        jobjectArray rlimits, jint mount_external, jstring se_info, jstring se_name,
        jintArray fdsToClose, jstring instructionSet, jstring appDataDir) {
    beforeForkAppProcess(env, uid, se_name);
    jint pid = (reinterpret_cast<nativeForkAndSpecialize_marshmallow_t> (forkAppProcess))(env, clazz, uid, gid, gids, debug_flags, rlimits, mount_external, se_info, se_name,
                                                                                          fdsToClose, instructionSet, appDataDir);
    afterForkAppProcess(env, uid, pid, se_name);
    return pid;
}

jint nativeForkAndSpecialize_oreo(
        JNIEnv *env, jclass clazz, jint uid, jint gid, jintArray gids, jint debug_flags,
        jobjectArray rlimits, jint mount_external, jstring se_info, jstring se_name,
        jintArray fdsToClose, jintArray fdsToIgnore, jstring instructionSet, jstring appDataDir) {
    beforeForkAppProcess(env, uid, se_name);
    jint pid = (reinterpret_cast<nativeForkAndSpecialize_oreo_t> (forkAppProcess))(env, clazz, uid, gid, gids, debug_flags, rlimits, mount_external, se_info, se_name,
                                                                                   fdsToClose, fdsToIgnore, instructionSet, appDataDir);
    afterForkAppProcess(env, uid, pid, se_name);
    return pid;
}

jint nativeForkAndSpecialize_p(
        JNIEnv *env, jclass clazz, jint uid, jint gid, jintArray gids, jint runtime_flags,
        jobjectArray rlimits, jint mount_external, jstring se_info, jstring se_name,
        jintArray fdsToClose, jintArray fdsToIgnore, jboolean is_child_zygote,
        jstring instructionSet, jstring appDataDir) {
    beforeForkAppProcess(env, uid, se_name);
    jint pid = (reinterpret_cast<nativeForkAndSpecialize_p_t> (forkAppProcess)) (env, clazz, uid, gid, gids, runtime_flags, rlimits, mount_external, se_info, se_name,
                                                                                fdsToClose, fdsToIgnore, is_child_zygote, instructionSet, appDataDir);
    afterForkAppProcess(env, uid, pid, se_name);
    return pid;
}

jint nativeForkAndSpecialize_q_beta4(
        JNIEnv *env, jclass clazz, jint uid, jint gid, jintArray gids, jint runtime_flags,
        jobjectArray rlimits, jint mount_external, jstring se_info, jstring se_name,
        jintArray fdsToClose, jintArray fdsToIgnore, jboolean is_child_zygote,
        jstring instructionSet, jstring appDataDir, jstring packageName,
        jobjectArray packagesForUID, jstring sandboxId) {
    beforeForkAppProcess(env, uid, se_name);
    jint pid = (reinterpret_cast<nativeForkAndSpecialize_q_beta4_t> (forkAppProcess)) (env, clazz, uid, gid, gids, runtime_flags, rlimits, mount_external, se_info, se_name,
                                                                                          fdsToClose, fdsToIgnore, is_child_zygote, instructionSet, appDataDir, packageName,
                                                                                          packagesForUID, sandboxId);
    afterForkAppProcess(env, uid, pid, se_name);
    return pid;
}

jint nativeForkAndSpecialize_samsung_p(
        JNIEnv *env, jclass clazz, jint uid, jint gid, jintArray gids, jint runtime_flags,
        jobjectArray rlimits, jint mount_external, jstring se_info, jint category, jint accessInfo,
        jstring se_name, jintArray fdsToClose, jintArray fdsToIgnore, jboolean is_child_zygote,
        jstring instructionSet, jstring appDataDir) {
    beforeForkAppProcess(env, uid, se_name);
    jint pid = (reinterpret_cast<nativeForkAndSpecialize_samsung_p_t> (forkAppProcess)) (env, clazz, uid, gid, gids, runtime_flags, rlimits, mount_external, se_info, category,
                                                                                         accessInfo, se_name, fdsToClose, fdsToIgnore, is_child_zygote, instructionSet,
                                                                                         appDataDir);
    afterForkAppProcess(env, uid, pid, se_name);
    return pid;
}

jint nativeForkAndSpecialize_samsung_o(
        JNIEnv *env, jclass clazz, jint uid, jint gid, jintArray gids, jint debug_flags,
        jobjectArray rlimits, jint mount_external, jstring se_info, jint category, jint accessInfo,
        jstring se_name, jintArray fdsToClose, jintArray fdsToIgnore, jstring instructionSet,
        jstring appDataDir) {
    beforeForkAppProcess(env, uid, se_name);
    jint pid = (reinterpret_cast<nativeForkAndSpecialize_samsung_o_t> (forkAppProcess)) (env, clazz, uid, gid, gids, debug_flags, rlimits, mount_external, se_info, category,
                                                                                         accessInfo, se_name, fdsToClose, fdsToIgnore, instructionSet, appDataDir);
    afterForkAppProcess(env, uid, pid, se_name);
    return pid;
}

jint nativeForkAndSpecialize_samsung_n(
        JNIEnv *env, jclass clazz, jint uid, jint gid, jintArray gids, jint debug_flags,
        jobjectArray rlimits, jint mount_external, jstring se_info, jint category, jint accessInfo,
        jstring se_name, jintArray fdsToClose, jstring instructionSet, jstring appDataDir, jint a1) {
    beforeForkAppProcess(env, uid, se_name);
    jint pid = (reinterpret_cast<nativeForkAndSpecialize_samsung_n_t> (forkAppProcess)) (env, clazz, uid, gid, gids, debug_flags, rlimits, mount_external, se_info, category,
                                                                                         accessInfo, se_name, fdsToClose, instructionSet, appDataDir, a1);
    afterForkAppProcess(env, uid, pid, se_name);
    return pid;
}

jint nativeForkAndSpecialize_samsung_m(
        JNIEnv *env, jclass clazz, jint uid, jint gid, jintArray gids, jint debug_flags,
        jobjectArray rlimits, jint mount_external, jstring se_info, jint category, jint accessInfo,
        jstring se_name, jintArray fdsToClose, jstring instructionSet, jstring appDataDir) {
    beforeForkAppProcess(env, uid, se_name);
    jint pid = (reinterpret_cast<nativeForkAndSpecialize_samsung_m_t> (forkAppProcess)) (env, clazz, uid, gid, gids, debug_flags, rlimits, mount_external, se_info, category,
                                                                                         accessInfo, se_name, fdsToClose, instructionSet, appDataDir);
    afterForkAppProcess(env, uid, pid, se_name);
    return pid;
}

jint nativeForkSystemServer(
        JNIEnv *env, jclass clazz, uid_t uid, gid_t gid, jintArray gids, jint debug_flags,
        jobjectArray rlimits, jlong permittedCapabilities, jlong effectiveCapabilities) {
    beforeForkSystemServer(env);
    jint pid = (reinterpret_cast<nativeForkSystemServer_t> (forkSystemServer)) (env, clazz, uid, gid, gids, debug_flags, rlimits, permittedCapabilities,
                                                                                effectiveCapabilities);
    afterForkSystemServer(env, pid);
    return pid;
}

JNINativeMethod *onRegisterZygote(JNIEnv *env, JNINativeMethod *methods, int numMethods) {
    LOGI("onRegisterZygote");
    LOGI(" --- BEGIN ZYGOTE NATIVE METHODS --- ");
    JNINativeMethod *newMethods = new JNINativeMethod[numMethods];
    memcpy(newMethods, methods, sizeof(JNINativeMethod) * numMethods);
    bool found_fork_app_process = false;
    bool found_fork_system_server = false;
    for(int i = 0;i < numMethods;i++) {
        JNINativeMethod method = newMethods[i];
        LOGD("detect native method %s%s", method.name, method.signature);
        void *replace_func = nullptr;
        if(strcmp(method.name, "nativeForkAndSpecialize") == 0) {
            if(found_fork_app_process) {
                LOGW("Found %s%s but we've found other method. Ignore it.", method.name, method.signature);
                continue;
            }
            setForkAppProcessFunc(method.fnPtr);
            found_fork_app_process = true;
            if(strcmp(method.signature, nativeForkAndSpecialize_marshmallow_sig) == 0) {
                replace_func = reinterpret_cast<void *> (nativeForkAndSpecialize_marshmallow);
            } else if(strcmp(method.signature, nativeForkAndSpecialize_oreo_sig) == 0) {
                replace_func = reinterpret_cast<void *> (nativeForkAndSpecialize_oreo);
            } else if(strcmp(method.signature, nativeForkAndSpecialize_p_sig) == 0) {
                replace_func = reinterpret_cast<void *> (nativeForkAndSpecialize_p);
            } else if(strcmp(method.signature, nativeForkAndSpecialize_q_beta4_sig) == 0) {
                replace_func = reinterpret_cast<void *> (nativeForkAndSpecialize_q_beta4);
            } else if(strcmp(method.signature, nativeForkAndSpecialize_samsung_m_sig) == 0) {
                replace_func = reinterpret_cast<void *> (nativeForkAndSpecialize_samsung_m);
            } else if(strcmp(method.signature, nativeForkAndSpecialize_samsung_n_sig) == 0) {
                replace_func = reinterpret_cast<void *> (nativeForkAndSpecialize_samsung_n);
            } else if(strcmp(method.signature, nativeForkAndSpecialize_samsung_o_sig) == 0) {
                replace_func = reinterpret_cast<void *> (nativeForkAndSpecialize_samsung_o);
            } else if(strcmp(method.signature, nativeForkAndSpecialize_samsung_p_sig) == 0) {
                replace_func = reinterpret_cast<void *> (nativeForkAndSpecialize_samsung_p);
            } else {
                found_fork_app_process = false;
                setForkAppProcessFunc(nullptr);
                LOGW("found nativeForkAndSpecialize but signature %s mismatch", method.signature);
                continue;
            }
        } else if(strcmp(method.name, "nativeForkSystemServer") == 0) {
            if(found_fork_system_server) {
                LOGW("Found method %s%s but we've found another method. Ignore it.", method.name, method.signature);
                continue;
            }
            found_fork_system_server = true;
            setForkSystemServerFunc(method.fnPtr);
            if(strcmp(method.signature, nativeForkSystemServer_sig) == 0) {
                replace_func = reinterpret_cast<void *> (nativeForkSystemServer);
            } else {
                found_fork_system_server = false;
                setForkSystemServerFunc(nullptr);
                LOGW("found nativeForkSystemServer but signature %s mismatch", method.signature);
                continue;
            }
        } else {
            continue;
        }
        newMethods[i].fnPtr = replace_func;
    }

    if(!found_fork_app_process) {
        LOGE("Method Zygote.nativeForkAndSpecialize not found");
    }
    if(!found_fork_system_server) {
        LOGE("Method Zygote.nativeForkSystemServer not found");
    }
    LOGI(" --- END ZYGOTE NATIVE METHODS --- ");
    return newMethods;
}
