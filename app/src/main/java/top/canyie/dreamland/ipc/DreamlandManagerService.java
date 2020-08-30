package top.canyie.dreamland.ipc;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.util.Log;
import android.util.LruCache;

import top.canyie.dreamland.core.AppManager;
import top.canyie.dreamland.core.Dreamland;
import top.canyie.dreamland.core.ModuleManager;
import top.canyie.dreamland.utils.DLog;

import java.io.File;
import java.io.IOException;
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
    private static final int SYSTEM_UID = 1000;
    private static final int SHELL_UID = 2000;

    private boolean mSafeModeEnabled;
    private boolean mGlobalModeEnabled;
    private boolean mResourcesHookEnabled;

    private Context mContext;
    private final AppManager mAppManager;
    private final ModuleManager mModuleManager;

    private volatile String[] mEnabledAppCache;
    private final LruCache<String, String[]> mEnabledModuleCache = new LruCache<String, String[]>(128) {
        @Override protected String[] create(String key) {
            Set<String> set = mModuleManager.getEnabledFor(key);
            return set.toArray(new String[set.size()]);
        }
    };
    private volatile String[] mAllEnabledModuleCache;

    private DreamlandManagerService() {
        mModuleManager = new ModuleManager();
        mModuleManager.startLoad();
        mAppManager = new AppManager();
        mAppManager.startLoad();
        mSafeModeEnabled = new File(Dreamland.BASE_DIR, SAFE_MODE_FILENAME).exists();
        mGlobalModeEnabled = new File(Dreamland.BASE_DIR, GLOBAL_MODE_FILENAME).exists();
        mResourcesHookEnabled = new File(Dreamland.BASE_DIR, ENABLE_RESOURCES_FILENAME).exists();
    }

    public static DreamlandManagerService start() {
        return new DreamlandManagerService();
    }

    public void initContext(Context context) {
        mContext = context;
    }

    public void registerPackageReplacedReceiver() {
        // FIXME Receiver will not triggered
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
    }

    @Override public int getVersion() {
        return Dreamland.VERSION;
    }

    @Override public boolean isEnabledFor() {
        String calling = getCallingPackage();
        if (Dreamland.MANAGER_PACKAGE_NAME.equals(calling)
                || Dreamland.OLD_MANAGER_PACKAGE_NAME.equals(calling)) {
            return true;
        }
        if (mSafeModeEnabled) return false;
        return isAppEnabled(calling);
    }

    @Override public String[] getEnabledApps() {
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

    @Override public void setAppEnabled(String packageName, boolean enabled) {
        enforceManager("setAppEnabled");
        mAppManager.setEnabled(packageName, enabled);
        mEnabledAppCache = null;
    }

    @Override public String[] getEnabledModulesFor() {
        if (mSafeModeEnabled) return null;
        String calling = getCallingPackage();
        if (Dreamland.MANAGER_PACKAGE_NAME.equals(calling)
                || Dreamland.OLD_MANAGER_PACKAGE_NAME.equals(calling)
                || !isAppEnabled(calling)) return null;
        return mEnabledModuleCache.get(calling);
    }

    @Override public String[] getAllEnabledModules() {
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

    @Override public void setModuleEnabled(String packageName, boolean enabled) {
        enforceManager("setModuleEnabled");
        if (enabled) {
            try {
                PackageManager pm = mContext.getPackageManager();
                ApplicationInfo appInfo = pm.getApplicationInfo(packageName, 0);
                String apkPath = appInfo.publicSourceDir;
                if (apkPath == null) {
                    apkPath = appInfo.sourceDir;
                    if (apkPath == null) {
                        DLog.e(TAG, "No valid apk found for module " + packageName);
                        return;
                    }
                }
                mModuleManager.enable(packageName, apkPath);
            } catch (PackageManager.NameNotFoundException e) {
                DLog.e(TAG, "Attempting to enable a non-existing module " + packageName, e);
                return;
            }
        } else {
            mModuleManager.disable(packageName);
        }
        synchronized (mModuleManager) {
            // Invalidate caches.
            mAllEnabledModuleCache = null;
            mEnabledModuleCache.evictAll();
        }
    }

    @Override public boolean isSafeModeEnabled() {
        enforceManagerOrEnabledModule("isSafeModeEnabled");
        return mSafeModeEnabled;
    }

    @Override public void setSafeModeEnabled(boolean enabled) {
        enforceManager("setSafeModeEnabled");
        mSafeModeEnabled = enabled;
        touch(SAFE_MODE_FILENAME, enabled);
    }

    @Override public void reload() {
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

    @Override public void setResourcesHookEnabled(boolean enabled) {
        enforceManager("setResourcesHookEnabled");
        mResourcesHookEnabled = enabled;
        touch(ENABLE_RESOURCES_FILENAME, enabled);
    }

    @Override public boolean isGlobalModeEnabled() {
        return mGlobalModeEnabled;
    }

    @Override public void setGlobalModeEnabled(boolean enabled) {
        enforceManager("setGlobalModeEnabled");
        mGlobalModeEnabled = enabled;
        touch(GLOBAL_MODE_FILENAME, enabled);
    }

    @Override public String[] getEnabledAppsFor(String module) {
        enforceManager("getEnabledAppsFor");
        synchronized (mModuleManager) {
            return mModuleManager.getEnabledAppsFor(module);
        }
    }

    @Override public void setEnabledAppsFor(String module, String[] apps) {
        enforceManager("setEnabledAppsFor");
        synchronized (mModuleManager) {
            mModuleManager.setEnabledFor(module, apps);
            mEnabledModuleCache.evictAll();
        }
    }

    private String getCallingPackage() {
        int callingUid = Binder.getCallingUid();
        return callingUid == SYSTEM_UID
                ? "android" : mContext.getPackageManager().getNameForUid(callingUid);
    }

    private boolean isAppEnabled(String packageName) {
        return mGlobalModeEnabled || mAppManager.isEnabled(packageName);
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

    private void enforceManager(String op) {
        String callingPackage = mContext.getPackageManager().getNameForUid(Binder.getCallingUid());
        if (!Dreamland.MANAGER_PACKAGE_NAME.equals(callingPackage)) {
            DLog.i(TAG, "Rejecting package " + callingPackage + " to do " + op);
            throw new SecurityException("Only dreamland manager can do: " + op);
        }
    }

    private void enforceManagerOrEnabledModule(String op) {
        enforceManager(op);
        // FIXME Implement for enabled module
    }

    private void enforceManagerOrShell(String op) {
        int callingUid = Binder.getCallingUid();
        Binder.getCallingUserHandle(
        if (callingUid == ROOT_UID || callingUid == SYSTEM_UID || callingUid == SHELL_UID) return;
        String callingPackage = mContext.getPackageManager().getNameForUid(callingUid);
        if (!Dreamland.MANAGER_PACKAGE_NAME.equals(callingPackage))
            throw new SecurityException("Only root/system/shell/manager process can " + op);
    }
}
