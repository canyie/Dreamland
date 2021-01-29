//
// Created by canyie on 2021/1/28.
//

#include "binder.h"
#include "../utils/scoped_local_ref.h"
#include "../utils/log.h"

using namespace dreamland;

jclass Binder::ServiceManager = nullptr;
jclass Binder::IBinder = nullptr;
jclass Binder::Parcel = nullptr;
jmethodID Binder::getService = nullptr;
jmethodID Binder::transact = nullptr;
jmethodID Binder::obtainParcel = nullptr;
jmethodID Binder::writeInterfaceToken = nullptr;
jmethodID Binder::readException = nullptr;
jmethodID Binder::readStrongBinder = nullptr;
jmethodID Binder::recycleParcel = nullptr;
jstring Binder::serviceName = nullptr;
jstring Binder::interfaceToken = nullptr;

template <typename T>
static T MakeGlobalRef(JNIEnv* env, T local) {
    T global = (T) env->NewGlobalRef(local);
    env->DeleteLocalRef(local);
    return global;
}

template <typename T>
static void FreeGlobalRef(JNIEnv* env, T& ref) {
    env->DeleteGlobalRef(ref);
    ref = nullptr;
}

bool Binder::Prepare(JNIEnv* env) {
    ServiceManager = MakeGlobalRef(env, env->FindClass("android/os/ServiceManager"));
    IBinder = MakeGlobalRef(env, env->FindClass("android/os/IBinder"));
    Parcel = MakeGlobalRef(env, env->FindClass("android/os/Parcel"));

    getService = env->GetStaticMethodID(ServiceManager, "getService", "(Ljava/lang/String;)Landroid/os/IBinder;");
    transact = env->GetMethodID(IBinder, "transact", "(ILandroid/os/Parcel;Landroid/os/Parcel;I)Z");
    obtainParcel = env->GetStaticMethodID(Parcel, "obtain", "()Landroid/os/Parcel;");
    writeInterfaceToken = env->GetMethodID(Parcel, "writeInterfaceToken", "(Ljava/lang/String;)V");
    readException = env->GetMethodID(Parcel, "readException", "()V");
    readStrongBinder = env->GetMethodID(Parcel, "readStrongBinder", "()Landroid/os/IBinder;");
    recycleParcel = env->GetMethodID(Parcel, "recycle", "()V");

    serviceName = MakeGlobalRef(env, env->NewStringUTF(kServiceName));
    interfaceToken = MakeGlobalRef(env, env->NewStringUTF(kInterfaceToken));
    return true;
}

jobject Binder::GetBinder(JNIEnv* env) {
    // TODO: JNI is very slow, use pure-native code instead if we can.
#define FAIL_IF(cond, error_msg) do {\
    if (UNLIKELY(cond)) {\
    LOGE(error_msg);\
    goto fail;\
    }\
    \
} while (0)

#define FAIL_IF_EXCEPTION(error_msg) FAIL_IF(env->ExceptionCheck(), error_msg)

    ScopedLocalRef<jobject> clipboard(env);
    ScopedLocalRef<jobject> data(env);
    ScopedLocalRef<jobject> reply(env);
    jboolean success;
    jobject service = nullptr;

    clipboard.Reset(env->CallStaticObjectMethod(ServiceManager, getService, serviceName));
    FAIL_IF_EXCEPTION("ServiceManager.getService threw exception");

    if (UNLIKELY(clipboard.IsNull())) {
        // Isolated process or google gril service process is not allowed to access clipboard service
        LOGW("Clipboard service is unavailable in current process, skipping");
        return nullptr;
    }

    data.Reset(env->CallStaticObjectMethod(Parcel, obtainParcel));
    FAIL_IF(data.IsNull(), "Failed to obtain data parcel");
    reply.Reset(env->CallStaticObjectMethod(Parcel, obtainParcel));
    FAIL_IF(reply.IsNull(), "Failed to obtain reply parcel");

    env->CallVoidMethod(data.Get(), writeInterfaceToken, interfaceToken);
    FAIL_IF_EXCEPTION("Parcel.writeInterfaceToken threw exception");

    success = env->CallBooleanMethod(clipboard.Get(), transact, kTransactionCode, data.Get(), reply.Get(), 0);
    FAIL_IF_EXCEPTION("Binder.transact threw exception");

    env->CallVoidMethod(reply.Get(), readException);
    FAIL_IF_EXCEPTION("Clipboard service threw exception");

    if (UNLIKELY(success)) {
        service = env->CallObjectMethod(reply.Get(), readStrongBinder);
        FAIL_IF_EXCEPTION("readStrongBinder threw exception");
    }

    RecycleParcel(env, data.Get());
    RecycleParcel(env, reply.Get());

    return service;

fail:
    env->ExceptionDescribe();
    env->ExceptionClear();
    RecycleParcel(env, data.Get());
    RecycleParcel(env, reply.Get());
    return nullptr;
}

void Binder::Cleanup(JNIEnv* env) {
    FreeGlobalRef(env, ServiceManager);
    FreeGlobalRef(env, IBinder);
    FreeGlobalRef(env, Parcel);
    FreeGlobalRef(env, serviceName);
    FreeGlobalRef(env, interfaceToken);
}
