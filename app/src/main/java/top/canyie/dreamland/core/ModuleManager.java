package top.canyie.dreamland.core;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.ZipFile;

import top.canyie.dreamland.ipc.ModuleInfo;

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

    public String[] getScopeFor(String packageName) {
        ModuleInfo module = getRealObject().get(packageName);
        if (module == null) return null;
        return module.getScope();
    }

    public void setScopeFor(String packageName, String[] apps) {
        Map<String, ModuleInfo> map = getRealObject();
        ModuleInfo moduleInfo = map.get(packageName);
        if (moduleInfo == null) {
            moduleInfo = new ModuleInfo();
            moduleInfo.enabled = false;
            // Not found in map, this module must not be enabled.
            // Module path will set in enable().
            map.put(packageName, moduleInfo);
        }
        moduleInfo.setScope(apps);
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

    public void getScopeFor(String packageName, Set<ModuleInfo> out) {
        Map<String, ModuleInfo> map = getRealObject();
        if (map.isEmpty()) return;
        Collection<ModuleInfo> modules = map.values();
        for (ModuleInfo module : modules) {
            if (module.enabled && module.isEnabledFor(packageName))
                out.add(module);
        }
    }

    public boolean isModuleEnabled(String packageName) {
        ModuleInfo moduleInfo = getRealObject().get(packageName);
        return moduleInfo != null && moduleInfo.enabled;
    }

    public void enable(String module, String path, String nativeDir) {
        Map<String, ModuleInfo> map = getRealObject();
        ModuleInfo moduleInfo = map.get(module);
        if (moduleInfo == null) {
            map.put(module, new ModuleInfo(path, nativeDir));
        } else {
            moduleInfo.path = path; // Module path maybe changed
            moduleInfo.nativePath = nativeDir;
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

    public void updateModulePath(String packageName, String path, String nativeDir) {
        ModuleInfo moduleInfo = getRealObject().get(packageName);
        if (moduleInfo != null) {
            moduleInfo.path = path;
            moduleInfo.nativePath = nativeDir;
            notifyDataChanged();
        }
    }

    public boolean remove(String packageName) {
        ModuleInfo moduleInfo = getRealObject().remove(packageName);
        if (moduleInfo != null) {
            notifyDataChanged();
            return true;
        }
        return false;
    }

    @NonNull @Override protected ConcurrentHashMap<String, ModuleInfo> createEmpty() {
        return new ConcurrentHashMap<>();
    }

    public static boolean isModuleValid(String apk) {
        try (ZipFile zip = new ZipFile(apk)) {
            return zip.getEntry("assets/xposed_init") != null;
        } catch (IOException e) {
            return false;
        }
    }
}
