package top.canyie.dreamland.core;

import androidx.annotation.Keep;
import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by canyie on 2019/11/12.
 */
@Keep public final class ModuleInfo {
//  public String name;
    public String path;
    public boolean enabled = true;

    /**
     * This module has been enabled for the applications contained in the collection.
     * null: module activation scope not enabled for this module.
     */
    @Nullable private Set<String> enabledFor;

    public ModuleInfo() {
    }

    public ModuleInfo(String path) {
        this.path = path;
    }

    public boolean isEnabledFor(String packageName) {
        return enabledFor == null || enabledFor.contains(packageName);
    }

    public String[] getEnabledFor() {
        if (enabledFor == null) return null;
        return enabledFor.toArray(new String[enabledFor.size()]);
    }

    public void setEnabledFor(String[] packages) {
        if (packages == null) {
            // Disable module activation scope
            enabledFor = null;
            return;
        }
        if (enabledFor == null)
            enabledFor = new HashSet<>(4);
        else
            enabledFor.clear();
        Collections.addAll(enabledFor, packages);
    }



//    public ModuleInfo(String name, String path) {
//        this.name = name;
//        this.path = path;
//    }
}
