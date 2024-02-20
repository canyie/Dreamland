//
// Created by canyie on 2022/2/28.
//

#include "flavor.h"
#include "../zygisk.hpp"
#include "../utils/macros.h"

using namespace dreamland;

using zygisk::Api;
using zygisk::AppSpecializeArgs;
using zygisk::ServerSpecializeArgs;

class DreamlandZygiskFlavor : public zygisk::ModuleBase {
public:
    void onLoad(Api* api, JNIEnv* env) override {
        this->api_ = api;
        this->env_ = env;
        skip_ = true;
        Flavor::OnModuleLoaded(false);
    }

    void preAppSpecialize(AppSpecializeArgs* args) override {
        skip_ = Flavor::ShouldSkip(args->is_child_zygote && *args->is_child_zygote, args->uid);
        if (!skip_)
            Flavor::PreFork(env_, true);
    }

    void postAppSpecialize(const AppSpecializeArgs* args) override {
        /** FIXME: check if the zygote that created the child process is main zygote */
        if (skip_ || !Flavor::PostForkApp(env_, false))
            api_->setOption(zygisk::Option::DLCLOSE_MODULE_LIBRARY);
    }

    void preServerSpecialize(ServerSpecializeArgs* args) override {
        if (LIKELY(!Flavor::IsDisabled())) Flavor::PreFork(env_, true);
    }

    void postServerSpecialize(const ServerSpecializeArgs* args) override {
        if (!Flavor::IsDisabled()) Flavor::PostForkSystemServer(env_);
    }

private:
    Api *api_;
    JNIEnv *env_;
    bool skip_;
};

REGISTER_ZYGISK_MODULE(DreamlandZygiskFlavor)
