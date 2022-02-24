//
// Created by canyie on 2020/8/5.
//

#ifndef DREAMLAND_RESOURCES_HOOK_H
#define DREAMLAND_RESOURCES_HOOK_H

#include <jni.h>
#include <cstddef>
#include "resource_types.h"

namespace dreamland {
    class ResourcesHook {
    public:
        static bool Init(JNIEnv* env, jobject classLoader);
        static void JNI_rewriteXmlReferencesNative(JNIEnv *env, jclass,
                jlong parserPtr, jobject origRes, jobject repRes);
    private:
        static int32_t (*ResXMLParser_next)(void*);
        static int32_t (*ResXMLParser_restart)(void*);
        static int32_t (*ResXMLParser_getAttributeNameID)(void*, int);
        static char16_t* (*ResStringPool_stringAt)(const void*, int32_t, size_t*);
        static android::expected<android::StringPiece16, android::NullOrIOError> (*ResStringPool_stringAtS)(
                const void*, size_t);
        static jclass XResources;
        static jmethodID translateResId;
        static jmethodID translateAttrId;

        static constexpr const char* kXResourcesClassName = "android.content.res.XResources";
    };
}

#endif //DREAMLAND_RESOURCES_HOOK_H
