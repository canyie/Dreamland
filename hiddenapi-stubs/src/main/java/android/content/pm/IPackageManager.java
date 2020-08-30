package android.content.pm;

import android.annotation.TargetApi;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.RemoteException;

/**
 * @author canyie
 */
public interface IPackageManager extends IInterface {
    ApplicationInfo getApplicationInfo(String packageName, int flags, int userId) throws RemoteException;

    PackageInfo getPackageInfo(String packageName, int flags, int userId) throws RemoteException;

    int getPackageUid(String packageName, int userId) throws RemoteException;

    int getPackageUid(String packageName, int flags, int userId) throws RemoteException;

    String[] getPackagesForUid(int uid) throws RemoteException;

    String getNameForUid(int uid) throws RemoteException;

    abstract class Stub extends Binder implements IPackageManager {
        public static IPackageManager asInterface(IBinder service) {
            throw new UnsupportedOperationException("Stub!");
        }
    }
}
