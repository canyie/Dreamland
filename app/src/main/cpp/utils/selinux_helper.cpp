//
// Created by canyie on 2019/12/1.
//

#include <dlfcn.h>
#include "selinux_helper.h"
#include "log.h"

using namespace dreamland;

void *SELinuxHelper::lib_handle = nullptr;
int (*SELinuxHelper::is_selinux_enabled) () = nullptr;
int (*SELinuxHelper::security_getenforce) () = nullptr;
int (*SELinuxHelper::security_setenforce) (int) = nullptr;
int (*SELinuxHelper::setcon) (const char *) = nullptr;
int (*SELinuxHelper::getcon) (char **) = nullptr;
 int (*SELinuxHelper::setfilecon) (const char *, const char *) = nullptr;
int (*SELinuxHelper::getfilecon) (const char *, char **) = nullptr;

inline bool GetSymbol(void *handle, const char *symbol, void **out) {
    *out = dlsym(handle, symbol);
    if(*out == nullptr) {
        LOGE("SELinuxHelper: Failed to find symbol \"%s\" in libselinux.so: %s", symbol, dlerror());
        return false;
    }
    return true;
}

inline bool EnsureHasSymbol(void *handle, const char *symbol, void **ptr) {
    return *ptr != nullptr ? true : GetSymbol(handle, symbol, ptr);
}

bool SELinuxHelper::InitOnce() {
    lib_handle = dlopen(LIB_SELINUX_PATH, RTLD_NOW | RTLD_GLOBAL);
    if(lib_handle == nullptr) {
        LOGE("SELinuxHelper: Failed to open %s: %s", LIB_SELINUX_PATH, dlerror());
        return false;
    }
    return true;
}

bool SELinuxHelper::IsEnabled() {
    if(!EnsureHasSymbol(lib_handle, "is_selinux_enabled",
                        reinterpret_cast<void **> (&is_selinux_enabled))) return false;
    int result = is_selinux_enabled();
    if(result < 0) {
        LOGE("SELinuxHelper: is_selinux_enabled returned %d", result);
        return false;
    }
    return result > 0;
}

bool SELinuxHelper::IsEnforced() {
    if(!IsEnabled()) return false;
    if(!EnsureHasSymbol(lib_handle, "security_getenforce",
                        reinterpret_cast<void **> (&security_getenforce))) return false;
    return security_getenforce() == 1;
}

bool SELinuxHelper::SetEnforce(bool enforce) {
    if(!EnsureHasSymbol(lib_handle, "security_setenforce",
                        reinterpret_cast<void **> (&security_setenforce))) return false;
    return security_setenforce(enforce ? 1 : 0) == 0;
}

char* SELinuxHelper::GetContext() {
    if(!EnsureHasSymbol(lib_handle, "getcon",
                        reinterpret_cast<void **> (&getcon))) return nullptr;
    char *context;
    if(getcon(&context) != 0) {
        LOGE("SELinuxHelper: getcon failed...");
        return nullptr;
    }
    return context;
}

bool SELinuxHelper::SetContext(const char *context) {
    if(!EnsureHasSymbol(lib_handle, "setcon",
                        reinterpret_cast<void **> (&setcon))) return false;
    return setcon(context) == 0;
}

char* SELinuxHelper::GetFileContext(const char *file) {
    if(!EnsureHasSymbol(lib_handle, "getfilecon",
                        reinterpret_cast<void **> (&getfilecon))) return nullptr;
    char *context;
    if(getfilecon(file, &context) != 0) {
        LOGE("SELinuxHelper: getfilecon failed...");
        return nullptr;
    }
    return context;
}

bool SELinuxHelper::SetFileContext(const char *file, const char *context) {
    if(!EnsureHasSymbol(lib_handle, "setfilecon",
                        reinterpret_cast<void **> (&setfilecon))) return false;
    return setfilecon(file, context) == 0;
}