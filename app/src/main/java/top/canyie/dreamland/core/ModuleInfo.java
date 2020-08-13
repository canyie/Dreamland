package top.canyie.dreamland.core;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import dalvik.system.PathClassLoader;

/**
 * Created by canyie on 2019/11/12.
 */
@Keep public final class ModuleInfo {
//    public String name;
    public String path;
    //public boolean enabled;
    ///** Collection of this module enabled for */
    //public Set<String> enabledFor;

    // TODO: Support for Module Activation Scope

    public ModuleInfo() {
    }

    public ModuleInfo(String path) {
        this.path = path;
    }

//    public ModuleInfo(String name, String path) {
//        this.name = name;
//        this.path = path;
//    }
}
