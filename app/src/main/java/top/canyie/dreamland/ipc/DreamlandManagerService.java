package top.canyie.dreamland.ipc;

import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.Process;
import android.os.RemoteException;
import android.os.UserHandleHidden;
import android.util.Log;
import android.util.LruCache;

import top.canyie.dreamland.core.AppManager;
import top.canyie.dreamland.core.Dreamland;
import top.canyie.dreamland.core.ModuleManager;
import top.canyie.dreamland.utils.AppConstants;
import top.canyie.dreamland.utils.BuildUtils;
import top.canyie.dreamland.utils.DLog;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
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

    /** Skip other packages in system server */
    private static final String SKIP_SYSTEM_SERVER = "skip_system_server";
    private static final int ROOT_UID = 0;
    private static final int SYSTEM_UID = Process.SYSTEM_UID;
    private static final int SHELL_UID = 2000;

    private static DreamlandManagerService instance;

    private boolean mSafeModeEnabled;
    private boolean mGlobalModeEnabled;
    private boolean mResourcesHookEnabled;
    private boolean mSkipSystemServer;

    private IPackageManager pm;
    private final AppManager mAppManager;
    private final ModuleManager mModuleManager;

    private volatile String[] mEnabledAppCache;
    private final LruCache<String, String[]> mEnabledModuleCache = new LruCache<String, String[]>(128) {
        @Override protected String[] create(String key) {
            HashSet<String> set = new HashSet<>();
            mModuleManager.getScopeFor(key, set);
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

    private boolean cannotHookSystemServer = true;

    private DreamlandManagerService() {
        instance = this;
        mModuleManager = new ModuleManager();
        mModuleManager.startLoad();
        mAppManager = new AppManager();
        mAppManager.startLoad();
        mSafeModeEnabled = enabled(SAFE_MODE_FILENAME);
        mGlobalModeEnabled = enabled(GLOBAL_MODE_FILENAME);
        mResourcesHookEnabled = enabled(ENABLE_RESOURCES_FILENAME);
        mSkipSystemServer = enabled(SKIP_SYSTEM_SERVER);
    }

    public static DreamlandManagerService start() {
        return new DreamlandManagerService();
    }

    public static DreamlandManagerService getInstance() {
        return instance;
    }

    public ModuleManager getModuleManager() {
        return mModuleManager;
    }

    public void setPackageManager(IBinder service) {
        pm = IPackageManager.Stub.asInterface(service);
    }

    public String getModulePath(String packageName) throws RemoteException {
        ApplicationInfo appInfo = BuildUtils.isAtLeastT()
                ? pm.getApplicationInfo(packageName, 0L, UserHandleHidden.getCallingUserId())
                : pm.getApplicationInfo(packageName, (int) 0, UserHandleHidden.getCallingUserId());
        if (appInfo == null) return null;
        String[] splitSourceDirs = appInfo.splitSourceDirs;
        if (splitSourceDirs == null) return appInfo.sourceDir;

        String[] apks = Arrays.copyOf(splitSourceDirs, splitSourceDirs.length + 1);
        apks[splitSourceDirs.length] = appInfo.sourceDir;
        return Arrays.stream(apks).filter(ModuleManager::isModuleValid).findFirst().orElse(null);
    }

    public void onSystemServerHookCalled() {
        cannotHookSystemServer = false;
    }

    public boolean hookLoadPackageInSystemServer() {
        if (mSafeModeEnabled) return false;
        if (mGlobalModeEnabled) return true;
        if (mSkipSystemServer) return false;
        if (mAppManager.getAllEnabled().isEmpty()) {
            mSkipSystemServer = true;
            touch(SKIP_SYSTEM_SERVER, true);
            return false;
        }
        return true;
    }

    @Override public int getVersion() {
        return Dreamland.VERSION;
    }

    @Override public boolean isEnabledFor() {
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

    public boolean isEnabledForSystemServer() {
        if (mSafeModeEnabled) return false;
        if (mGlobalModeEnabled) return true;
        return mAppManager.isEnabled(AppConstants.ANDROID);
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

    @Override
    public void setAppEnabled(String packageName, boolean enabled) throws RemoteException {
        enforceManager("setAppEnabled");
        mAppManager.setEnabled(packageName, enabled);
        mEnabledAppCache = null;

        if (AppConstants.ANDROID.equals(packageName)) return; // system server itself
        int userId = UserHandleHidden.getCallingUserId();
        int uid = BuildUtils.isAtLeastT()
                ? pm.getPackageUid(packageName, 0L, userId)
                : pm.getPackageUid(packageName, (int) 0, userId);
        if (uid == Process.SYSTEM_UID) {
            if (enabled) {
                if (mSkipSystemServer) {
                    mSkipSystemServer = false;
                    touch(SKIP_SYSTEM_SERVER, false);
                }
            } else {
                if (mSkipSystemServer) return;
                String[] packages = mAppUidMap.get(Process.SYSTEM_UID);
                if (packages == null || packages.length == 0) return;
                for (String pkg : packages) {
                    if (packageName.equals(pkg)) continue;
                    if (AppConstants.ANDROID.equals(pkg)) continue;
                    if (mAppManager.isEnabled(pkg)) return;
                }
                mSkipSystemServer = true;
                touch(SKIP_SYSTEM_SERVER, true);
            }
        }
    }

    @Override public String[] getEnabledModulesFor(String packageName) {
        if (mSafeModeEnabled) return null;

        if (Dreamland.MANAGER_PACKAGE_NAME.equals(packageName)
                || Dreamland.OLD_MANAGER_PACKAGE_NAME.equals(packageName)) return null;

        boolean enabled = mGlobalModeEnabled || mAppManager.isEnabled(packageName);
        if (!enabled) return null;

        return mEnabledModuleCache.get(packageName);
    }

    public String[] getEnabledModulesForSystemServer() {
        if (mSafeModeEnabled) return null;
        if (!(mGlobalModeEnabled || mAppManager.isEnabled(AppConstants.ANDROID))) return null;
        return mEnabledModuleCache.get(AppConstants.ANDROID);
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

    @Override
    public void setModuleEnabled(String packageName, boolean enabled) throws RemoteException {
        enforceManager("setModuleEnabled");
        if (enabled) {
            String apkPath = getModulePath(packageName);
            if (apkPath == null) {
                DLog.e(TAG, "No valid apk found for module " + packageName);
                return;
            }
            mModuleManager.enable(packageName, apkPath);
        } else {
            mModuleManager.disable(packageName);
        }
        clearModuleCache();
    }

    public void clearModuleCache() {
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

    @Override public String[] getScopeFor(String module) throws RemoteException {
        enforceManager("getScopeFor");
        synchronized (mModuleManager) {
            return mModuleManager.getScopeFor(module);
        }
    }

    @Override public void setScopeFor(String module, String[] apps) throws RemoteException {
        enforceManager("setScopeFor");
        synchronized (mModuleManager) {
            mModuleManager.setScopeFor(module, apps);
            mEnabledModuleCache.evictAll();
        }
    }

    @Override public boolean cannotHookSystemServer() throws RemoteException {
        enforceManager("cannotHookSystemServer");
        return cannotHookSystemServer;
    }

    private String[] getPackagesForCallingUid() {
        return mAppUidMap.get(Binder.getCallingUid());
    }

    private boolean enabled(String config) {
        return new File(Dreamland.BASE_DIR, config).exists();
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
