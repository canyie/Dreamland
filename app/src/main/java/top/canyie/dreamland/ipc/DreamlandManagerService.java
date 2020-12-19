package top.canyie.dreamland.ipc;

import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.Process;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.Log;
import android.util.LruCache;

import top.canyie.dreamland.core.AppManager;
import top.canyie.dreamland.core.Dreamland;
import top.canyie.dreamland.core.ModuleManager;
import top.canyie.dreamland.utils.AppConstants;
import top.canyie.dreamland.utils.DLog;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * @author canyie
 */
public final class DreamlandManagerService extends IDreamlandManager.Stub {
    private static final String TAG = "DreamlandManagerService";
    private static final String SAFE_MODE_FILENAME = "safemode";
    private static final String GLOBAL_MODE_FILENAME = "global_mode";
    private static final String ENABLE_RESOURCES_FILENAME = "enable_resources";
    private static final int ROOT_UID = 0;
    private static final int SYSTEM_UID = Process.SYSTEM_UID;
    private static final int SHELL_UID = 2000;

    private boolean mSafeModeEnabled;
    private boolean mGlobalModeEnabled;
    private boolean mResourcesHookEnabled;

    private IPackageManager pm;
    private final AppManager mAppManager;
    private final ModuleManager mModuleManager;

    private volatile String[] mEnabledAppCache;
    private final LruCache<String[], String[]> mEnabledModuleCache = new LruCache<String[], String[]>(128) {
        @Override protected String[] create(String[] key) {
            if (key.length == 0) return AppConstants.EMPTY_STRING_ARRAY;
            HashSet<String> set = new HashSet<>();
            for (String p : key) {
                mModuleManager.getEnabledFor(p, set);;
            }
            return set.toArray(new String[set.size()]);
        }
    };
    private volatile String[] mAllEnabledModuleCache;

    private final LruCache<Integer, String[]> mAppUidMap = new LruCache<Integer, String[]>(128) {
        @Override protected String[] create(Integer key) {
            try {
                return pm.getPackagesForUid(key);
            } catch (RemoteException e) {
                // should never happen, we run in the same process as PMS
                Log.e(TAG, "getPackagesForUid failed", e);
                return null;
            }
        }
    };

    private DreamlandManagerService() {
        mModuleManager = new ModuleManager();
        mModuleManager.startLoad();
        mAppManager = new AppManager();
        mAppManager.startLoad();
        mSafeModeEnabled = new File(Dreamland.BASE_DIR, SAFE_MODE_FILENAME).exists();
        mGlobalModeEnabled = new File(Dreamland.BASE_DIR, GLOBAL_MODE_FILENAME).exists();
        mResourcesHookEnabled = new File(Dreamland.BASE_DIR, ENABLE_RESOURCES_FILENAME).exists();

        mAppUidMap.put(SYSTEM_UID, AppConstants.ARRAY_ANDROID);
    }

    public static DreamlandManagerService start() {
        return new DreamlandManagerService();
    }

    public void setPackageManager(IBinder service) {
        pm = IPackageManager.Stub.asInterface(service);
    }

//    public void registerPackageReplacedReceiver() {
//        // FIXME Receiver will not triggered
//        try {
//            IntentFilter intentFilter = new IntentFilter();
//            intentFilter.addAction(Intent.ACTION_PACKAGE_REPLACED);
//            intentFilter.addDataScheme("package");
//            mContext.registerReceiver(new BroadcastReceiver() {
//                @Override public void onReceive(Context context, Intent intent) {
//                    String packageName = intent.getData().getSchemeSpecificPart();
//                    Log.i(TAG, "Package " + packageName + " Replaced");
//                }
//            }, intentFilter);
//        } catch (Throwable e) {
//            Log.e(Dreamland.TAG, "Cannot register package replaced receiver", e);
//        }
//    }
    @Override public int getVersion() {
        return Dreamland.VERSION;
    }

    @Override public boolean isEnabledFor() throws RemoteException {
        String[] packages = getPackagesForCallingUid();
        if (packages != null && packages.length == 1) {
            // Dreamland manager should never use sharedUserId.
            String calling = packages[0];
            if (Dreamland.MANAGER_PACKAGE_NAME.equals(calling)
                    || Dreamland.OLD_MANAGER_PACKAGE_NAME.equals(calling)) {
                return true;
            }
        }

        if (mSafeModeEnabled) return false;
        if (mGlobalModeEnabled) return true;
        if (packages == null) return false;
        for (String app : packages) {
            if (mAppManager.isEnabled(app))
                return true;
        }
        return false;
    }

    @Override public String[] getEnabledApps() throws RemoteException {
        enforceManagerOrEnabledModule("getEnabledApps");
        String[] enabled = mEnabledAppCache;
        if (enabled == null) {
            synchronized (mAppManager) {
                if (mEnabledAppCache == null) {
                    Set<String> enabledSet = mAppManager.getAllEnabled();
                    mEnabledAppCache = enabled = enabledSet.toArray(new String[enabledSet.size()]);
                } else {
                    enabled = mEnabledAppCache;
                }
            }
        }
        return enabled;
    }

    @Override public void setAppEnabled(String packageName, boolean enabled) throws RemoteException {
        enforceManager("setAppEnabled");
        mAppManager.setEnabled(packageName, enabled);
        mEnabledAppCache = null;
    }

    @Override public String[] getEnabledModulesFor() throws RemoteException {
        if (mSafeModeEnabled) return null;
        String[] packages = getPackagesForCallingUid();
        if (packages == null || packages.length == 0) return null;
        if (packages.length == 1) {
            // Dreamland manager should never use sharedUserId.
            String calling = packages[0];
            if (Dreamland.MANAGER_PACKAGE_NAME.equals(calling)
                    || Dreamland.OLD_MANAGER_PACKAGE_NAME.equals(calling)) return null;
        }

        boolean enabled = false;
        if (!mGlobalModeEnabled) {
            for (String packageName : packages) {
                if (mAppManager.isEnabled(packageName)) {
                    enabled = true;
                    break;
                }
            }
        }

        if (!enabled) return null;

        return mEnabledModuleCache.get(packages);
    }

    @Override public String[] getAllEnabledModules() throws RemoteException {
        enforceManagerOrEnabledModule("getAllEnabledModules");
        String[] modules = mAllEnabledModuleCache;
        if (modules == null) {
            synchronized (mModuleManager) {
                if (mAllEnabledModuleCache == null) {
                    Set<String> set = mModuleManager.getAllEnabled();
                    modules = mAllEnabledModuleCache = set.toArray(new String[set.size()]);
                } else {
                    modules = mAllEnabledModuleCache;
                }
            }
        }
        return modules;
    }

    @Override public void setModuleEnabled(String packageName, boolean enabled) throws RemoteException {
        enforceManager("setModuleEnabled");
        if (enabled) {
            ApplicationInfo appInfo = pm.getApplicationInfo(packageName, 0, UserHandle.getCallingUserId());
            if (appInfo == null) {
                DLog.e(TAG, "Attempting to enable a non-existing module " + packageName);
                return;
            }
            String apkPath = appInfo.publicSourceDir;
            if (apkPath == null) {
                apkPath = appInfo.sourceDir;
                if (apkPath == null) {
                    DLog.e(TAG, "No valid apk found for module " + packageName);
                    return;
                }
            }
            mModuleManager.enable(packageName, apkPath);
        } else {
            mModuleManager.disable(packageName);
        }
        synchronized (mModuleManager) {
            // Invalidate caches.
            mAllEnabledModuleCache = null;
            mEnabledModuleCache.evictAll();
        }
    }

    @Override public boolean isSafeModeEnabled() throws RemoteException {
        enforceManagerOrEnabledModule("isSafeModeEnabled");
        return mSafeModeEnabled;
    }

    @Override public void setSafeModeEnabled(boolean enabled) throws RemoteException {
        enforceManager("setSafeModeEnabled");
        mSafeModeEnabled = enabled;
        touch(SAFE_MODE_FILENAME, enabled);
    }

    @Override public void reload() throws RemoteException {
        enforceManagerOrShell("reload");
        mModuleManager.startLoad();
        mAppManager.startLoad();
        mSafeModeEnabled = new File(Dreamland.BASE_DIR, SAFE_MODE_FILENAME).exists();
        mGlobalModeEnabled = new File(Dreamland.BASE_DIR, GLOBAL_MODE_FILENAME).exists();
        mResourcesHookEnabled = new File(Dreamland.BASE_DIR, ENABLE_RESOURCES_FILENAME).exists();
    }

    @Override public boolean isResourcesHookEnabled() {
        return mResourcesHookEnabled;
    }

    @Override public void setResourcesHookEnabled(boolean enabled) throws RemoteException {
        enforceManager("setResourcesHookEnabled");
        mResourcesHookEnabled = enabled;
        touch(ENABLE_RESOURCES_FILENAME, enabled);
    }

    @Override public boolean isGlobalModeEnabled() {
        return mGlobalModeEnabled;
    }

    @Override public void setGlobalModeEnabled(boolean enabled) throws RemoteException {
        enforceManager("setGlobalModeEnabled");
        mGlobalModeEnabled = enabled;
        touch(GLOBAL_MODE_FILENAME, enabled);
    }

    @Override public String[] getEnabledAppsFor(String module) throws RemoteException {
        enforceManager("getEnabledAppsFor");
        synchronized (mModuleManager) {
            return mModuleManager.getEnabledAppsFor(module);
        }
    }

    @Override public void setEnabledAppsFor(String module, String[] apps) throws RemoteException {
        enforceManager("setEnabledAppsFor");
        synchronized (mModuleManager) {
            mModuleManager.setEnabledFor(module, apps);
            mEnabledModuleCache.evictAll();
        }
    }

    private String[] getPackagesForCallingUid() {
        return mAppUidMap.get(Binder.getCallingUid());
    }

    private void touch(String filename, boolean enabled) {
        File file = new File(Dreamland.BASE_DIR, filename);
        try {
            boolean success = enabled ? file.createNewFile() : file.delete();
            if (!success) {
                Log.e(TAG, "Failed to " + (enabled ? "create " : "delete ") + file.getAbsolutePath());
            }
        } catch (IOException e) {
            Log.e(TAG, "Failed to create " + file.getAbsolutePath(), e);
        }
    }

    private void enforceManager(String op) throws RemoteException {
        String callingPackage = pm.getNameForUid(Binder.getCallingUid());
        if (!Dreamland.MANAGER_PACKAGE_NAME.equals(callingPackage)) {
            DLog.i(TAG, "Rejecting package " + callingPackage + " to do " + op);
            throw new SecurityException("Only dreamland manager can do: " + op);
        }
    }

    private void enforceManagerOrEnabledModule(String op) throws RemoteException {
        enforceManager(op);
        // FIXME Implement for enabled module
    }

    private void enforceManagerOrShell(String op) throws RemoteException {
        int callingUid = Binder.getCallingUid();
        if (callingUid == ROOT_UID || callingUid == SYSTEM_UID || callingUid == SHELL_UID) return;
        String callingPackage = pm.getNameForUid(callingUid);
        if (!Dreamland.MANAGER_PACKAGE_NAME.equals(callingPackage))
            throw new SecurityException("Only root/system/shell/manager process can " + op);
    }
}
