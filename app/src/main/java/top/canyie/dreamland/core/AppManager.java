package top.canyie.dreamland.core;

import androidx.annotation.NonNull;

import top.canyie.dreamland.utils.collections.ConcurrentHashSet;

import java.util.Set;
import java.util.StringTokenizer;

/**
 * @author canyie
 */
public final class AppManager extends BaseManager<ConcurrentHashSet<String>> {
    public AppManager() {
        super("apps.list");
    }

    public boolean isEnabled(String packageName) {
        return getRealObject().contains(packageName);
    }

    public void setEnabled(String packageName, boolean enabled) {
        Set<String> set = getRealObject();
        boolean changed = enabled ? set.add(packageName) : set.remove(packageName);
        if (changed) notifyDataChanged();
    }

    public Set<String> getAllEnabled() {
        // Note: From the principle of encapsulation, the real object should not be returned directly,
        // but this is not a public API
        // and we know that no write operation will be performed on the return value.
        return getRealObject();
    }

    @NonNull @Override protected String serialize(ConcurrentHashSet<String> set) {
        StringBuilder sb = new StringBuilder();
        for (String packageName : set) {
            if (packageName == null || (packageName = packageName.trim()).isEmpty()) continue;
            sb.append(packageName).append('\n');
        }
        return sb.toString();
    }

    @Override protected ConcurrentHashSet<String> deserialize(String str) {
        StringTokenizer st = new StringTokenizer(str, "\n");
        ConcurrentHashSet<String> set = new ConcurrentHashSet<>(Math.max((int) (st.countTokens() / .75f) + 1, 16));
        while (st.hasMoreTokens()) {
            String packageName = st.nextToken();
            if (packageName == null || (packageName = packageName.trim()).isEmpty()) continue;
            set.add(packageName);
        }
        return set;
    }

    @NonNull @Override protected ConcurrentHashSet<String> createEmpty() {
        return new ConcurrentHashSet<>();
    }
}
