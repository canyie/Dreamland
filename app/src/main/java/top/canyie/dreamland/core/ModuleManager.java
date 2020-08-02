package top.canyie.dreamland.core;

import androidx.annotation.NonNull;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author canyie
 */
public final class ModuleManager extends GsonBasedManager<ConcurrentHashMap<String, ModuleInfo>> {
    public ModuleManager() {
        super("modules.json");
    }

    public Map<String, ModuleInfo> getEnabledModules() {
        // Note: From the principle of encapsulation, the real object should not be returned directly,
        // we should return a unmodifiable object, but this is not a public API
        // and we know that no write operation will be performed on the return value.
        // return new HashMap<>(getRealObject());
        return getRealObject();
    }

    public Collection<ModuleInfo> getEnabledModuleList() {
        // Note: From the principle of encapsulation, the real object should not be returned directly,
        // we should return a unmodifiable object, but this is not a public API
        // and we know that no write operation will be performed on the return value.
        return getRealObject().values();
    }

    public boolean isModuleEnabled(String packageName) {
        return getRealObject().containsKey(packageName);
    }

    public void enable(String packageName, ModuleInfo module) {
        getRealObject().put(packageName, module);
        notifyDataChanged();
    }

    public void disable(String packageName) {
        getRealObject().remove(packageName);
        notifyDataChanged();
    }

    @NonNull @Override protected ConcurrentHashMap<String, ModuleInfo> createEmpty() {
        return new ConcurrentHashMap<>();
    }
}