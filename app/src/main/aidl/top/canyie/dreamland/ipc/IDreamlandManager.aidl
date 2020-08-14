// IDreamlandManager.aidl
package top.canyie.dreamland.ipc;

interface IDreamlandManager {
    int getVersion();
    boolean isEnabledFor();
    String[] getEnabledModulesFor();
    String[] getAllEnabledModules();
    void setModuleEnabled(String packageName, boolean enabled);
    String[] getEnabledApps();
    void setAppEnabled(String packageName, boolean enabled);
    boolean isSafeModeEnabled();
    void setSafeModeEnabled(boolean enabled);
    void reload();
    boolean isResourcesHookEnabled();
    void setResourcesHookEnabled(boolean enabled);
    boolean isGlobalModeEnabled();
    void setGlobalModeEnabled(boolean enabled);
    String[] getEnabledAppsFor(String module);
    void setEnabledAppsFor(String module, in String[] apps);
}
