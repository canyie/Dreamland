package com.canyie.dreamland.core;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author canyie
 */
public final class ModuleManager extends GsonBasedManager<ConcurrentHashMap<String, ModuleInfo>> {
    ModuleManager() {
        super("modules.json");
    }

    public Map<String, ModuleInfo> getEnabledModules() {
        return new HashMap<>(getRawObject());
    }

    public List<ModuleInfo> getEnabledModuleList() {
        Map<String, ModuleInfo> map = getRawObject();
        return new ArrayList<>(map.values());
    }

    public boolean isModuleEnabled(String packageName) {
        return getRawObject().containsKey(packageName);
    }

    @NonNull @Override protected ConcurrentHashMap<String, ModuleInfo> createEmptyObject() {
        return new ConcurrentHashMap<>();
    }
}