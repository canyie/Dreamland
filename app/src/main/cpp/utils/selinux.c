//
// From The Android Open Source Project
//

#include <stdlib.h>
#include <stdio.h>
#include <unistd.h>
#include <errno.h>
#include <fcntl.h>
#include <string.h>
#include "log.h"
#include "selinux.h"

#define SELINUX_MNT "/sys/fs/selinux"
#define OPEN_FAILED() do {\
LOGE("SELinux: open %s failed. errno %d(%s)", path, errno, strerror(errno));\
} while(0)
#define READ_FAILED() do {\
LOGE("SELinux: read %s failed. errno %d(%s)", path, errno, strerror(errno));\
} while(0)
#define WRITE_FAILED() do {\
LOGE("SELinux: write %s failed. errno %d(%s)", path, errno, strerror(errno));\
} while(0)

int is_selinux_enabled(void) {
    return access(SELINUX_MNT, F_OK) == 0 ? 1 : 0;
}

int security_getenforce(void) {
    int fd, ret, enforce = 0;
    char path[PATH_MAX];
    char buf[20];

    if (!is_selinux_enabled()) {
        errno = ENOENT;
        return -1;
    }

    snprintf(path, sizeof path, "%s/enforce", SELINUX_MNT);
    fd = open(path, O_RDONLY | O_CLOEXEC);
    if (fd < 0) {
        OPEN_FAILED();
        return -1;
    }

    memset(buf, 0, sizeof buf);
    ret = read(fd, buf, sizeof buf - 1);
    close(fd);
    if (ret < 0) {
        READ_FAILED();
        return -1;
    }

    if (sscanf(buf, "%d", &enforce) != 1)
        return -1;

    return enforce;
}

int security_setenforce(int value) {
    int fd, ret;
    char path[PATH_MAX];
    char buf[20];

    if (!is_selinux_enabled()) {
        errno = ENOENT;
        return -1;
    }

    snprintf(path, sizeof path, "%s/enforce", SELINUX_MNT);
    fd = open(path, O_RDWR | O_CLOEXEC);
    if (fd < 0) {
        OPEN_FAILED();
        return -1;
    }

    snprintf(buf, sizeof buf, "%d", value);
    ret = write(fd, buf, strlen(buf));
    close(fd);
    if (ret < 0) {
        WRITE_FAILED();
        return -1;
    }

    return 0;
}
