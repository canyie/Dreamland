//
// Created by canyie on 2022/2/28.
//

#include "flavor.h"
#include "dreamland.h"
#include "../pine.h"

using namespace dreamland;

static bool disabled = false;

void Flavor::OnModuleLoaded(bool preload) {
    // preload == false means the flavor is Zygisk, which will call OnModuleLoaded() from app process
    // rather than zygote, apps can read logs printed by their processes so this can be detected
    if (preload) LOGI("Welcome to Dreamland %s (%d)!", Dreamland::VERSION_NAME, Dreamland::VERSION);
    disabled = Dreamland::ShouldDisable();
    if (UNLIKELY(disabled)) {
        if (preload) LOGW("Dreamland framework should be disabled, do nothing.");
        return;
    }
    Android::Initialize();
    int api_level = Android::version;
    if (preload) LOGI("Android Api Level %d", api_level);
    PineSetAndroidVersion(api_level);
    Dreamland::Prepare(preload);
}

bool Flavor::IsDisabled() {
    return disabled;
}

bool Flavor::ShouldSkip(bool is_child_zygote, int uid) {
    if (UNLIKELY(disabled)) return true;

    if (UNLIKELY(Dreamland::ShouldSkipUid(uid))) {
        LOGW("Skipping this process because it is isolated service, RELTO updater or webview zygote");
        return true;
    }

    if (UNLIKELY(is_child_zygote)) {
        // child zygote is not allowed to do binder transaction, so our binder call will crash it
        LOGW("Skipping this process because it is a child zygote");
        return true;
    }
    return false;
}

void Flavor::PreFork(JNIEnv* env, bool zygote) {
    Dreamland::PrepareJava(env, zygote);
}

void Flavor::PostForkSystemServer(JNIEnv* env) {
    Dreamland::OnSystemServerStart(env);
}

bool Flavor::PostForkApp(JNIEnv* env, bool main_zygote) {
    return Dreamland::OnAppProcessStart(env, main_zygote);
}


