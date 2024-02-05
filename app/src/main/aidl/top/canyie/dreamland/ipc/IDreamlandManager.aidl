// IDreamlandManager.aidl
package top.canyie.dreamland.ipc;

import top.canyie.dreamland.ipc.ModuleInfo;

interface IDreamlandManager {
    int getVersion() = 0;
    boolean isEnabledFor() = 1;
    ModuleInfo[] getEnabledModulesFor(String packageName) = 2;
    String[] getAllEnabledModules() = 3;
    void setModuleEnabled(String packageName, boolean enabled) = 4;
    String[] getEnabledApps() = 5;
    void setAppEnabled(String packageName, boolean enabled) = 6;
    boolean isSafeModeEnabled() = 7;
    void setSafeModeEnabled(boolean enabled) = 8;
    void reload() = 9;
    boolean isResourcesHookEnabled() = 10;
    void setResourcesHookEnabled(boolean enabled) = 11;
    boolean isGlobalModeEnabled() = 12;
    void setGlobalModeEnabled(boolean enabled) = 13;
    String[] getScopeFor(String module) = 14;
    void setScopeFor(String module, in String[] scope) = 15;
    boolean cannotHookSystemServer() = 16;
}
