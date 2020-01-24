package com.canyie.dreamland.core;

import android.text.TextUtils;

import androidx.annotation.NonNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import dalvik.system.PathClassLoader;

/**
 * Created by canyie on 2019/11/12.
 */
public final class ModuleInfo {
    public String name;
    public String path;

    public void parseCallbacks(@NonNull List<String> out) throws IOException {
        try(ZipFile zip = new ZipFile(path)) {
            ZipEntry entry = zip.getEntry("assets/xposed_init");
            if(entry == null) {
                throw new IOException("xposed_init not found in module " + name + ". Please check the configuration of your module.");
            }

            try(BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(zip.getInputStream(entry), StandardCharsets.UTF_8))) {
                String line;
                while((line = bufferedReader.readLine()) != null) {
                    line = line.trim();
                    if(line.isEmpty() || line.startsWith("#")) continue;
                    out.add(line);
                }
            }
        }
    }

    @NonNull public ClassLoader getClassLoader() {
        return new PathClassLoader(path, getClass().getClassLoader());
    }
}
