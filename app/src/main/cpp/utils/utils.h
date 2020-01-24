//
// Created by canyie on 2019/12/1.
//

#ifndef DREAMLAND_UTILS_H
#define DREAMLAND_UTILS_H

#ifdef __arm__
#define SYSTEM_LIB_BASE_PATH "/system/lib/"
#elif defined(__aarch64__)
#define SYSTEM_LIB_BASE_PATH "/system/lib64/"
#else
#error "Unknown ABI, not suppport now"
#endif


#endif //DREAMLAND_UTILS_H
