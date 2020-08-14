package top.canyie.dreamland.core;

import androidx.annotation.NonNull;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author canyie
 */
public final class ModuleManager extends GsonBasedManager<ConcurrentHashMap<String, ModuleInfo>> {
    public ModuleManager() {
        super("modules.json");
    }

    public Map<String, ModuleInfo> getModules() {
        // Note: From the principle of encapsulation, the real object should not be returned directly,
        // we should return a unmodifiable object, but this is not a public API
        // and we know that no write operation will be performed on the return value.
        // return new HashMap<>(getRealObject());
        return getRealObject();
    }

    public String[] getEnabledAppsFor(String packageName) {
        ModuleInfo module = getRealObject().get(packageName);
        if (module == null) return null;
        return module.getEnabledFor();
    }

    public void setEnabledFor(String packageName, String[] apps) {
        Map<String, ModuleInfo> map = getRealObject();
        ModuleInfo moduleInfo = map.get(packageName);
        if (moduleInfo == null) {
            moduleInfo = new ModuleInfo();
            moduleInfo.enabled = false;
            // Not found in map, this module must not be enabled.
            // Module path will set in enable().
            map.put(packageName, moduleInfo);
        }
        moduleInfo.setEnabledFor(apps);
        notifyDataChanged();
    }

    public Set<String> getAllEnabled() {
        // Note: From the principle of encapsulation, the real object should not be returned directly,
        // we should return a unmodifiable object, but this is not a public API
        // and we know that no write operation will be performed on the return value.
        Map<String, ModuleInfo> map = getRealObject();
        if (map.isEmpty()) return Collections.emptySet();
        Set<String> result = new HashSet<>(Math.min(4, map.size() / 2));
        for (Map.Entry<String, ModuleInfo> entry : map.entrySet()) {
            if (entry.getValue().enabled)
                result.add(entry.getKey());
        }
        return result;
    }

    public Set<String> getEnabledFor(String packageName) {
        // Note: From the principle of encapsulation, the real object should not be returned directly,
        // we should return a unmodifiable object, but this is not a public API
        // and we know that no write operation will be performed on the return value.
        Map<String, ModuleInfo> map = getRealObject();
        if (map.isEmpty()) return Collections.emptySet();
        Collection<ModuleInfo> modules = map.values();
        Set<String> result = new HashSet<>(Math.min(4, modules.size() / 3));
        for (ModuleInfo module : modules) {
            if (module.enabled && module.isEnabledFor(packageName))
                result.add(module.path);
        }
        return result;
    }

    public boolean isModuleEnabled(String packageName) {
        ModuleInfo moduleInfo = getRealObject().get(packageName);
        return moduleInfo != null && moduleInfo.enabled;
    }

    public void enable(String module, String path) {
        Map<String, ModuleInfo> map = getRealObject();
        ModuleInfo moduleInfo = map.get(module);
        if (moduleInfo == null) {
            map.put(module, new ModuleInfo(path));
        } else {
            moduleInfo.path = path; // Module path maybe changed
            moduleInfo.enabled = true;
        }
        notifyDataChanged();
    }

    public void disable(String packageName) {
        ModuleInfo moduleInfo = getRealObject().get(packageName);
        if (moduleInfo != null) {
            moduleInfo.enabled = false;
            notifyDataChanged();
        }
    }

    @NonNull @Override protected ConcurrentHashMap<String, ModuleInfo> createEmpty() {
        return new ConcurrentHashMap<>();
    }
}