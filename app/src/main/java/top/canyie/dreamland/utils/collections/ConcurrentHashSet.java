package top.canyie.dreamland.utils.collections;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author canyie
 */
public final class ConcurrentHashSet<E> extends AbstractSet<E> implements Cloneable {
    private static final Object PRESENT = new Object();
    private transient ConcurrentHashMap<E, Object> map;

    public ConcurrentHashSet() {
        map = new ConcurrentHashMap<>();
    }

    public ConcurrentHashSet(Collection<? extends E> c) {
        map = new ConcurrentHashMap<>(Math.max((int) (c.size()/.75f) + 1, 16));
        addAll(c);
    }

    public ConcurrentHashSet(int initialCapacity) {
        map = new ConcurrentHashMap<>(initialCapacity);
    }

    public ConcurrentHashSet(int initialCapacity, float loadFactor) {
        map = new ConcurrentHashMap<>(initialCapacity, loadFactor);
    }

    @Override public boolean add(@NonNull E e) {
        return map.put(e, PRESENT) == null;
    }

    @Override public boolean remove(@Nullable Object o) {
        if (o == null) return false; // ConcurrentHashMap does not allow null.
        return map.remove(o) == PRESENT;
    }

    @Override public void clear() {
        map.clear();
    }

    @SuppressWarnings("SuspiciousMethodCalls") @Override public boolean contains(@Nullable Object o) {
        if (o == null) return false; // ConcurrentHashMap does not allow null.
        return map.containsKey(o);
    }

    @NonNull @Override public Iterator<E> iterator() {
        return map.keySet().iterator();
    }

    @Override public int size() {
        return map.size();
    }

    @SuppressWarnings("unchecked") @NonNull @Override protected Object clone() {
        try {
            ConcurrentHashSet<E> newSet = (ConcurrentHashSet<E>) super.clone();
            newSet.map = new ConcurrentHashMap<>(map);
            return newSet;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }
}
