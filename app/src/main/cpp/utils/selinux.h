//
// Created by canyie on 2019/12/3.
//

#ifndef DREAMLAND_SELINUX_H
#define DREAMLAND_SELINUX_H

#ifdef __cplusplus
extern "C" {
#endif

/* Return 1 if we are running on a SELinux kernel, or 0 if not or -1 if we get an error. */
extern int is_selinux_enabled(void);

///* Get file context, and set *con to refer to it.
//   Caller must free via freecon. */
//extern int getfilecon(const char *path, char ** con);
//
///* Set file context */
//extern int setfilecon(const char *path, const char *con);

/* Get the enforce flag value. */
extern int security_getenforce(void);

/* Set the enforce flag value. */
extern int security_setenforce(int value);

#ifdef __cplusplus
}
#endif

#endif //DREAMLAND_SELINUX_H
