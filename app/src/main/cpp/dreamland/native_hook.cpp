//
// Created by canyie on 2024/2/5.
//

#include <string>
#include <list>
#include <dobby.h>
#include <dlfcn.h>
#include <unistd.h>
#include <sys/mman.h>
#include "native_hook.h"
#include "../utils/log.h"
#include "../utils/macros.h"
#include "../utils/scoped_elf.h"

// --- Native Hook API definitions ---

typedef int (*HookFunType)(void* func, void* replace, void** backup);

typedef int (*UnhookFunType)(void* func);

typedef void (*NativeOnModuleLoaded)(const char* name, void* handle);

typedef struct {
    uint32_t version;
    HookFunType hook_func;
    UnhookFunType unhook_func;
} NativeAPIEntries;

typedef NativeOnModuleLoaded (*NativeInit)(const NativeAPIEntries* entries);

// -----------------------------------

static const size_t page_size = static_cast<const size_t>(sysconf(_SC_PAGESIZE));
static std::list<std::string> entrypoints;
static std::list<NativeOnModuleLoaded> module_loaded_callbacks;
static void* dlopen_backup;

namespace dreamland {
    static int HookFunction(void* func, void* replace, void** backup) {
        LOGD("Module hooking %p with %p, backup to %p", func, replace, backup);
        // Always re-protect the target page as rwx to bypass a dobby's bug
        size_t alignment = (uintptr_t) func % page_size;
        void* aligned_ptr = (void*) ((uintptr_t) func - alignment);
        mprotect(aligned_ptr, page_size, PROT_READ | PROT_WRITE | PROT_EXEC);
        return DobbyHook(func, replace, backup);
    }

    static int UnhookFunction(void* func) {
        LOGD("Module unhooking %p", func);
        return DobbyDestroy(func);
    }

    static const NativeAPIEntries api_entries {
            .version = 2,
            .hook_func = HookFunction,
            .unhook_func = UnhookFunction,
    };

    void SoLoaded(const char* name, void* handle) {
        if (!handle) [[unlikely]] return;
        std::string_view path(name ? name : "NULL");
        for (std::string_view module : entrypoints) {
            auto l = path.length();
            auto r = module.length();
            if (l >= r && path.compare(l - r, r, module) == 0) {
                // path ends with module, so this is a module library
                LOGD("Loading module library %s: %p", module.data(), handle);
                NativeInit native_init = reinterpret_cast<NativeInit>(dlsym(handle, "native_init"));
                NativeOnModuleLoaded callback;
                if (native_init && (callback = native_init(&api_entries))) {
                    module_loaded_callbacks.emplace_back(callback);
                    return; // return directly to avoid module interaction
                }
            }
        }

        for (auto& callback : module_loaded_callbacks)
            callback(name, handle);
    }

    void* DoDlopenHook(const char* name, int flags, const void* extinfo, const void* caller) {
        void* handle = reinterpret_cast<void* (*)(const char*, int, const void*, const void*)>(dlopen_backup)
                (name, flags, extinfo, caller);
        SoLoaded(name, handle);
        return handle;
    }

    void* DlopenHook(const char* name, int flags) {
        void* handle = reinterpret_cast<void* (*)(const char*, int)>(dlopen_backup)(name, flags);
        SoLoaded(name, handle);
        return handle;
    }

    static void Main_recordNativeEntrypoint(JNIEnv* env, jclass, jstring lib) {
        static bool initialized = []() {
            // Do not directly hook dlopen, changing its caller will change its linker namespace
            // and cause some system libraries fail to load.
            const char* linker_path = LP_SELECT("/apex/com.android.runtime/bin/linker64",
                                                "/apex/com.android.runtime/bin/linker");
            if (access(linker_path, F_OK)) {
                linker_path = LP_SELECT("/system/bin/linker64", "/system/bin/linker");
            }
            ScopedElf linker(linker_path);
            void* target;
            void* hook;

            // do_dlopen on Android 8.0+
            if (linker.GetSymbolAddress("__dl__Z9do_dlopenPKciPK17android_dlextinfoPKv", &target)
                // do_dlopen on Android 7.x
                || linker.GetSymbolAddress("__dl__Z9do_dlopenPKciPK17android_dlextinfoPv", &target)) {
                hook = reinterpret_cast<void*>(DoDlopenHook);
            } else {
                // Android before 7.0 do not have linker namespace, so we can directly hook dlopen
                target = reinterpret_cast<void*>(dlopen);
                hook = reinterpret_cast<void*>(DlopenHook);
            }
            return DobbyHook(target, hook, &dlopen_backup) == RS_SUCCESS;
        }();
        if (!initialized) [[unlikely]] return;
        auto library = env->GetStringUTFChars(lib, nullptr);
        entrypoints.emplace_back(library);
        env->ReleaseStringUTFChars(lib, library);
    }

    static const JNINativeMethod gMainNativeMethods[] = {
            {"recordNativeEntrypoint", "(Ljava/lang/String;)V", (void*) Main_recordNativeEntrypoint}
    };

    void NativeHook::RegisterNatives(JNIEnv* env, jclass main) {
        env->RegisterNatives(main, gMainNativeMethods, NELEM(gMainNativeMethods));
    }
} // dreamland
