//
// Created by canyie on 2020/2/4.
//

#ifndef DREAMLAND_ANDROID_H
#define DREAMLAND_ANDROID_H

#include <jni.h>
#include "../utils/macros.h"

namespace dreamland {
    class Android {
    public:
        static int version;
        static void Initialize();
        static constexpr int kN = 24;
        static constexpr int kO = 26;
        static constexpr int kP = 28;
        static constexpr int kQ = 29;

    private:

        DISALLOW_IMPLICIT_CONSTRUCTORS(Android);
    };
}

#endif //DREAMLAND_ANDROID_H
