package top.canyie.dreamland.core;

import androidx.annotation.Keep;
import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

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
    @Nullable @SerializedName(value = "scope", alternate = "enabledFor") private Set<String> scope;

    public ModuleInfo() {
    }

    public ModuleInfo(String path) {
        this.path = path;
    }

    public boolean isEnabledFor(String packageName) {
        return scope == null || scope.contains(packageName);
    }

    public String[] getScope() {
        if (scope == null) return null;
        return scope.toArray(new String[scope.size()]);
    }

    public void setScope(String[] packages) {
        if (packages == null) {
            // Disable module activation scope
            scope = null;
            return;
        }
        if (scope == null)
            scope = new HashSet<>(4);
        else
            scope.clear();
        Collections.addAll(scope, packages);
    }

//    public ModuleInfo(String name, String path) {
//        this.name = name;
//        this.path = path;
//    }
}
