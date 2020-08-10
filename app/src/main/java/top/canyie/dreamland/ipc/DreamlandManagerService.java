package top.canyie.dreamland.ipc;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.RemoteException;
import android.util.Log;

import top.canyie.dreamland.core.AppManager;
import top.canyie.dreamland.core.Dreamland;
import top.canyie.dreamland.core.ModuleInfo;
import top.canyie.dreamland.core.ModuleManager;
import top.canyie.dreamland.utils.DLog;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * @author canyie
 */
public final class DreamlandManagerService extends IDreamlandManager.Stub {
    private static final String TAG = "DreamlandManagerService";
    private static final String SAFE_MODE_FILENAME = "safemode";
    private static final String ENABLE_RESOURCES_FILENAME = "enable_resources";
    private static final int ROOT_UID = 0;
    private static final int SYSTEM_UID = 1000;
    private static final int SHELL_UID = 2000;

    private boolean mSafeModeEnabled;
    private boolean mResourcesHookEnabled;

    private Context mContext;
    private final AppManager mAppManager;
    private final ModuleManager mModuleManager;

    private volatile String[] mEnabledAppCache;
    private volatile String[] mEnabledModuleCache;

    private DreamlandManagerService() {
        mModuleManager = new ModuleManager();
        mModuleManager.startLoad();
        mAppManager = new AppManager();
        mAppManager.startLoad();
        mSafeModeEnabled = new File(Dreamland.BASE_DIR, SAFE_MODE_FILENAME).exists();
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
        try {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(Intent.ACTION_PACKAGE_REPLACED);
            intentFilter.addDataScheme("package");
            mContext.registerReceiver(new BroadcastReceiver() {
                @Override public void onReceive(Context context, Intent intent) {
                    String packageName = intent.getData().getSchemeSpecificPart();
                    Log.i(TAG, "Package " + packageName + " Replaced");
                }
            }, intentFilter);
        } catch (Throwable e) {
            Log.e(Dreamland.TAG, "Cannot register package replaced receiver", e);
        }
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
        return mAppManager.isEnabled(calling);
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
        if (!isCallingAppEnabled()) return null;
        String[] enabledFor = mEnabledModuleCache;
        if (enabledFor == null) {
            synchronized (mModuleManager) {
                if (mEnabledModuleCache == null) {
                    Collection<ModuleInfo> allEnabled = mModuleManager.getEnabledModuleList();
                    mEnabledModuleCache = enabledFor = new String[allEnabled.size()];
                    Iterator<ModuleInfo> iterator = allEnabled.iterator();
                    int i = 0;
                    while (iterator.hasNext()) {
                        ModuleInfo current = iterator.next();
                        enabledFor[i] = current.path;
                        i++;
                    }
                } else {
                    enabledFor = mEnabledModuleCache;
                }
            }
        }
        return enabledFor;
    }

    @Override public String[] getAllEnabledModules() {
        enforceManagerOrEnabledModule("getAllEnabledModules");
        Map<String, ModuleInfo> map = mModuleManager.getEnabledModules();
        return map.keySet().toArray(new String[map.size()]);
    }

    @Override
    public void setModuleEnabled(String packageName, boolean enabled) {
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
                mModuleManager.enable(packageName, new ModuleInfo(apkPath));
            } catch (PackageManager.NameNotFoundException e) {
                DLog.e(TAG, "Attempting to enable a non-existing module " + packageName, e);
                return;
            }
        } else {
            mModuleManager.disable(packageName);
        }
        mEnabledModuleCache = null;
    }

    @Override public boolean isSafeModeEnabled() {
        enforceManagerOrEnabledModule("isSafeModeEnabled");
        return mSafeModeEnabled;
    }

    @Override public void setSafeModeEnabled(boolean enabled) {
        enforceManager("setSafeModeEnabled");
        mSafeModeEnabled = enabled;
        File safeModeFile = new File(Dreamland.BASE_DIR, SAFE_MODE_FILENAME);
        try {
            boolean success = enabled ? safeModeFile.createNewFile() : safeModeFile.delete();
            if (!success) {
                String action = enabled ? "create " : "delete ";
                Log.e(TAG, "Failed to " + action + safeModeFile.getAbsolutePath());
            }
        } catch (IOException e) {
            Log.e(TAG, "Failed to create " + safeModeFile.getAbsolutePath(), e);
        }
    }

    @Override public void reload() {
        enforceManagerOrShell("reload");
        mModuleManager.startLoad();
        mAppManager.startLoad();
        mSafeModeEnabled = new File(Dreamland.BASE_DIR, SAFE_MODE_FILENAME).exists();
        mResourcesHookEnabled = new File(Dreamland.BASE_DIR, ENABLE_RESOURCES_FILENAME).exists();
    }

    @Override public boolean isResourcesHookEnabled() {
        return mResourcesHookEnabled;
    }

    @Override public void setResourcesHookEnabled(boolean enabled) {
        enforceManager("setResourcesHookEnabled");
        mResourcesHookEnabled = enabled;
        File enableFile = new File(Dreamland.BASE_DIR, ENABLE_RESOURCES_FILENAME);
        try {
            boolean success = enabled ? enableFile.createNewFile() : enableFile.delete();
            if (!success) {
                String action = enabled ? "create " : "delete ";
                Log.e(TAG, "Failed to " + action + enableFile.getAbsolutePath());
            }
        } catch (IOException e) {
            Log.e(TAG, "Failed to create " + enableFile.getAbsolutePath(), e);
        }
    }

    private String getCallingPackage() {
        int callingUid = Binder.getCallingUid();
        return callingUid == SYSTEM_UID
                ? "android" : mContext.getPackageManager().getNameForUid(callingUid);
    }

    private boolean isCallingAppEnabled() {
        return mAppManager.isEnabled(getCallingPackage());
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
        if (callingUid == ROOT_UID || callingUid == SYSTEM_UID || callingUid == SHELL_UID) return;
        String callingPackage = mContext.getPackageManager().getNameForUid(callingUid);
        if (!Dreamland.MANAGER_PACKAGE_NAME.equals(callingPackage))
            throw new SecurityException("Only root/system/shell/manager process can " + op);
    }
}
