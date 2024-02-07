package top.canyie.dreamland.ipc;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by canyie on 2019/11/12.
 */
@Keep public final class ModuleInfo implements Parcelable {
//  public String name;
    public String path;
    public String nativePath;
    public boolean enabled = true;

    /**
     * This module has been enabled for the applications contained in the collection.
     * null: module activation scope not enabled for this module.
     */
    @Nullable @SerializedName(value = "scope", alternate = "enabledFor") private Set<String> scope;

    public ModuleInfo() {
    }

    public ModuleInfo(String path, String nativePath) {
        this.path = path;
        this.nativePath = nativePath;
    }

    public static final Creator<ModuleInfo> CREATOR = new Creator<>() {
        @Override
        public ModuleInfo createFromParcel(Parcel in) {
            String path = in.readString();
            String nativePath = in.readString();
            return new ModuleInfo(path, nativePath);
        }

        @Override
        public ModuleInfo[] newArray(int size) {
            return new ModuleInfo[size];
        }
    };

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

    @Override public int describeContents() {
        return 0;
    }

    @Override public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(path);
        dest.writeString(nativePath);
    }

    @NonNull @Override public String toString() {
        return "ModuleInfo{path=" + path + ", nativeDir=" + nativePath + ", enabled=" + enabled + "}";
    }

    //    public ModuleInfo(String name, String path) {
//        this.name = name;
//        this.path = path;
//    }
}
