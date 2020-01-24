//
// Created by canyie on 2019/12/1.
//

#ifndef DREAMLAND_SELINUX_HELPER_H
#define DREAMLAND_SELINUX_HELPER_H

#include "macros.h"
#include "utils.h"

namespace dreamland {
    class SELinuxHelper {
    public:
        static bool InitOnce();
        static bool IsEnabled();
        static bool IsEnforced();
        static bool SetEnforce(bool enforce);
        static char *GetContext();
        static bool SetContext(const char *context);
        static char *GetFileContext(const char *file);
        static bool SetFileContext(const char *file, const char *context);

    private:
        static void *lib_handle;
        /** Return 1 if we are running on a SELinux kernel, or 0 if not or -1 if we get an error. */
        static int (*is_selinux_enabled) ();
        /** Get the enforce flag value. */
        static int (*security_getenforce) ();
        /** Set the enforce flag value. */
        static int (*security_setenforce) (int);
        /** Get current context, and set *con to refer to it. Caller must free via freecon. */
        static int (*getcon) (char **);
        static int (*setcon) (const char *);
        /** Get file context, and set *con to refer to it. Caller must free via freecon. */
        static int (*getfilecon) (const char *, char **);
        /** Set file context. */
        static int (*setfilecon) (const char *, const char *);

        static constexpr const char *LIB_SELINUX_PATH = SYSTEM_LIB_BASE_PATH "libselinux.so";
        DISALLOW_IMPLICIT_CONSTRUCTORS(SELinuxHelper);
    };
}

#endif //DREAMLAND_SELINUX_HELPER_H
