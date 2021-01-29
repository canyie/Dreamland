//
// Created by canyie on 2021/1/28.
//

#ifndef DREAMLAND_BINDER_H
#define DREAMLAND_BINDER_H


#include <jni.h>

namespace dreamland {
    class Binder {
    public:
        static bool Prepare(JNIEnv* env);
        static jobject GetBinder(JNIEnv* env);
        static void Cleanup(JNIEnv* env);

    private:
        static void RecycleParcel(JNIEnv* env, jobject parcel) {
            if (parcel) {
                env->CallVoidMethod(parcel, recycleParcel);
                env->ExceptionClear();
            }
        }

        static jclass ServiceManager;
        static jclass IBinder;
        static jclass Parcel;
        static jmethodID getService;
        static jmethodID transact;
        static jmethodID obtainParcel;
        static jmethodID writeInterfaceToken;
        static jmethodID readException;
        static jmethodID readStrongBinder;
        static jmethodID recycleParcel;
        static jstring serviceName;
        static jstring interfaceToken;

        static constexpr const char* kServiceName = "clipboard";
        static constexpr const char* kInterfaceToken = "android.content.IClipboard";
        static constexpr jint kTransactionCode = ('_'<<24)|('D'<<16)|('M'<<8)|'S';
    };
}


#endif //DREAMLAND_BINDER_H
