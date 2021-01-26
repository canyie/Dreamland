package top.canyie.dreamland.ipc;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.FileDescriptor;

/**
 * Proxy for binder service.
 * Note: For ServiceManager.addService, service must be a Binder object
 * See function ibinderForJavaObject (in android_util_Binder.cpp)
 */
public class BinderServiceProxy extends Binder {
    private static final int GET_BINDER_TRANSACTION = ('_'<<24)|('D'<<16)|('M'<<8)|'S';
    private Binder base;
    private String descriptor;
    private IBinder service;
    private CallerVerifier verifier;

    public static IBinder getBinderFrom(IBinder service, String descriptor) throws RemoteException {
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            data.writeInterfaceToken(descriptor);
            boolean success = service.transact(GET_BINDER_TRANSACTION, data, reply, 0);
            reply.readException();
            if (!success) {
                // Unknown transaction => remote service doesn't handle it, ignore this process.
                return null;
            }
            return reply.readStrongBinder();
        } finally {
            reply.recycle();
            data.recycle();
        }
    }

    public BinderServiceProxy(Binder base, String descriptor, IBinder service, CallerVerifier verifier) {
        this.base = base;
        this.descriptor = descriptor;
        this.service = service;
        this.verifier = verifier;
    }

    @Nullable @Override public String getInterfaceDescriptor() {
        return base.getInterfaceDescriptor();
    }

    @Override public boolean pingBinder() {
        return base.pingBinder();
    }

    @Override public boolean isBinderAlive() {
        return base.isBinderAlive();
    }

    @Nullable @Override public IInterface queryLocalInterface(@NonNull String descriptor) {
        return base.queryLocalInterface(descriptor);
    }

    @Override public void dump(@NonNull FileDescriptor fd, @Nullable String[] args) {
        base.dump(fd, args);
    }

    @Override public void dumpAsync(@NonNull FileDescriptor fd, @Nullable String[] args) {
        base.dumpAsync(fd, args);
    }

    @Override protected boolean onTransact(int code, @NonNull Parcel data,
                                           @Nullable Parcel reply, int flags) throws RemoteException {
        if (code == GET_BINDER_TRANSACTION && verifier.canAccessService()) {
            assert reply != null;
            data.enforceInterface(descriptor);
            reply.writeNoException();
            reply.writeStrongBinder(service);
            return true;
        }
        return base.transact(code, data, reply, flags);
    }

    @Override public void linkToDeath(@NonNull DeathRecipient recipient, int flags) {
        base.linkToDeath(recipient, flags);
    }

    @Override public boolean unlinkToDeath(@NonNull DeathRecipient recipient, int flags) {
        return base.unlinkToDeath(recipient, flags);
    }

    @FunctionalInterface public interface CallerVerifier {
        boolean canAccessService();
    }
}