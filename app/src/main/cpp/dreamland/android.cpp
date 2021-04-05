//
// Created by canyie on 2020/2/4.
//

#include <sys/system_properties.h>
#include <cstdlib>
#include <unistd.h>
#include <string>
#include <dlfcn.h>
#include "android.h"
#include "../pine.h"
#include "../utils/log.h"
#include "../utils/scoped_local_ref.h"
#include "../utils/jni_helper.h"

using namespace dreamland;

int Android::version = 0;

void Android::Initialize() {
    char android_level_str[8];
    __system_property_get("ro.build.version.sdk", android_level_str);
    Android::version = atoi(android_level_str);
}
