//
// Created by canyie on 2020/8/5.
// Code from https://github.com/ElderDrivers/EdXposed/blob/master/edxp-core/src/main/cpp/main/src/resource_hook.cpp
//

#include "resource_types.h"
#include "resources_hook.h"
#include "../utils/byte_order.h"
#include "../utils/scoped_elf.h"
#include "../utils/log.h"
#include "../utils/scoped_local_ref.h"
#include "../utils/jni_helper.h"

using namespace dreamland;

int32_t (*ResourcesHook::ResXMLParser_next)(void*) = nullptr;
int32_t (*ResourcesHook::ResXMLParser_restart)(void*) = nullptr;
int32_t (*ResourcesHook::ResXMLParser_getAttributeNameID)(void*, int) = nullptr;
char16_t* (*ResourcesHook::ResStringPool_stringAt)(const void*, int32_t, size_t*) = nullptr;
jclass ResourcesHook::XResources = nullptr;
jmethodID ResourcesHook::translateResId = nullptr;
jmethodID ResourcesHook::translateAttrId = nullptr;

void ResourcesHook::JNI_rewriteXmlReferencesNative(JNIEnv *env, jclass,
        jlong parserPtr, jobject origRes, jobject repRes) {
    auto parser = (android::ResXMLParser*) parserPtr;

    if (parser == nullptr)
        return;

    const android::ResXMLTree& mTree = parser->mTree;
    auto mResIds = (uint32_t*) mTree.mResIds;
    android::ResXMLTree_attrExt* tag;
    int attrCount;

    do {
        switch (ResXMLParser_next(parser)) {
            case android::ResXMLParser::START_TAG:
                tag = (android::ResXMLTree_attrExt*) parser->mCurExt;
                attrCount = dtohs(tag->attributeCount);
                for (int idx = 0; idx < attrCount; idx++) {
                    auto attr = (android::ResXMLTree_attribute*)
                            (((const uint8_t*) tag)
                             + dtohs(tag->attributeStart)
                             + (dtohs(tag->attributeSize) * idx));

                    // find resource IDs for attribute names
                    int32_t attrNameID = ResXMLParser_getAttributeNameID(parser, idx);
                    // only replace attribute name IDs for app packages
                    if (attrNameID >= 0 && (size_t) attrNameID < mTree.mNumResIds &&
                        dtohl(mResIds[attrNameID]) >= 0x7f000000) {
                        size_t attNameLen;
                        const char16_t* attrName = ResStringPool_stringAt(&(mTree.mStrings),
                                attrNameID, &attNameLen);
                        jint attrResID = env->CallStaticIntMethod(XResources, translateAttrId,
                                env->NewString((const jchar*) attrName, attNameLen), origRes);
                        if (UNLIKELY(env->ExceptionCheck()))
                            goto leave;

                        mResIds[attrNameID] = htodl(attrResID);
                    }

                    // find original resource IDs for reference values (app packages only)
                    if (attr->typedValue.dataType != android::Res_value::TYPE_REFERENCE)
                        continue;

                    jint oldValue = dtohl(attr->typedValue.data);
                    if (oldValue < 0x7f000000)
                        continue;

                    jint newValue = env->CallStaticIntMethod(XResources, translateResId,
                            oldValue, origRes, repRes);
                    if (UNLIKELY(env->ExceptionCheck()))
                        goto leave;

                    if (newValue != oldValue)
                        attr->typedValue.data = htodl(newValue);
                }
                continue;
            case android::ResXMLParser::END_DOCUMENT:
            case android::ResXMLParser::BAD_DOCUMENT:
                goto leave;
            default:
                continue;
        }
    } while (true);

    leave:
    ResXMLParser_restart(parser);
}

static constexpr const JNINativeMethod gMethods[] = {
        {"rewriteXmlReferencesNative", "(JLandroid/content/res/XResources;Landroid/content/res/Resources;)V", (void*) ResourcesHook::JNI_rewriteXmlReferencesNative}
};

bool ResourcesHook::Init(JNIEnv* env, jobject classLoader) {
    ScopedElf handle("libandroidfw.so");

#define FIND_SYMBOL_OR_FAIL(symbol, out) do { \
if (UNLIKELY(!handle.GetSymbolAddress((symbol), reinterpret_cast<void**>(&(out))))) { \
LOGE("Resources hook: could not find symbol %s", (symbol));\
return false;\
}\
} while (false)

    FIND_SYMBOL_OR_FAIL("_ZN7android12ResXMLParser4nextEv", ResXMLParser_next);
    FIND_SYMBOL_OR_FAIL("_ZN7android12ResXMLParser7restartEv", ResXMLParser_restart);
    FIND_SYMBOL_OR_FAIL(LP_SELECT("_ZNK7android12ResXMLParser18getAttributeNameIDEm",
            "_ZNK7android12ResXMLParser18getAttributeNameIDEj"), ResXMLParser_getAttributeNameID);
    FIND_SYMBOL_OR_FAIL(LP_SELECT("_ZNK7android13ResStringPool8stringAtEmPm",
            "_ZNK7android13ResStringPool8stringAtEjPj"), ResStringPool_stringAt);

#undef FIND_SYMBOL_OR_FAIL

    ScopedLocalRef<jclass> localXResources(env, JNIHelper::FindClassFromClassLoader(env,
            kXResourcesClassName, classLoader));
    if (UNLIKELY(localXResources.IsNull())) {
        LOGE("Resources hook: could not find class XResources");
        return false;
    }

    if (UNLIKELY(env->RegisterNatives(localXResources.Get(), gMethods, NELEM(gMethods)) != JNI_OK)) {
        LOGE("Resources hook: could not register native methods for class XResources");
        return false;
    }

    translateAttrId = env->GetStaticMethodID(localXResources.Get(), "translateAttrId",
            "(Ljava/lang/String;Landroid/content/res/XResources;)I");
    if (UNLIKELY(translateAttrId == nullptr)) {
        LOGE("Resources hook: could not find method translateAttrId on class XResources");
        return false;
    }

    translateResId = env->GetStaticMethodID(localXResources.Get(), "translateResId",
            "(ILandroid/content/res/XResources;Landroid/content/res/Resources;)I");
    if (UNLIKELY(translateResId == nullptr)) {
        LOGE("Resources hook: could not find method translateResId on class XResources");
        return false;
    }

    XResources = static_cast<jclass>(env->NewGlobalRef(localXResources.Get()));
    if (UNLIKELY(XResources == nullptr)) {
        LOGE("Resources hook: could not create global reference for class XResources.");
        return false;
    }
    return true;
}
