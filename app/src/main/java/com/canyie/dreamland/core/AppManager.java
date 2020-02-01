package com.canyie.dreamland.core;

import androidx.annotation.NonNull;

import com.canyie.dreamland.utils.collections.ConcurrentHashSet;

import java.util.StringTokenizer;

/**
 * @author canyie
 */
public final class AppManager extends BaseManager<ConcurrentHashSet<String>> {
    AppManager() {
        super("apps.list");
    }

    public boolean isEnabled(String packageName) {
        return getRawObject().contains(packageName);
    }

    @Override protected ConcurrentHashSet<String> deserialize(String str) {
        StringTokenizer st = new StringTokenizer(str, "\n");
        ConcurrentHashSet<String> set = new ConcurrentHashSet<>(Math.max((int) (st.countTokens() / .75f) + 1, 16));
        while (st.hasMoreTokens()) {
            String packageName = st.nextToken();
            if (packageName == null || packageName.trim().isEmpty()) continue;
            set.add(packageName);
        }
        return set;
    }

    @NonNull @Override protected ConcurrentHashSet<String> createEmptyObject() {
        return new ConcurrentHashSet<>();
    }
}
