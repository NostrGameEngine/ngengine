package com.jme3.util;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;

public final class WeakCollection<T> extends AbstractCollection<T> {

    private final ArrayList<WeakReference<T>> refs = new ArrayList<>();
    private final ReferenceQueue<T> queue = new ReferenceQueue<>();

    public WeakCollection() {
    }

    public WeakCollection(Collection<? extends T> c) {
        addAll(c);
    }

    private void cleanup() {
        boolean hadQueued = false;
        for (Reference<? extends T> r; (r = queue.poll()) != null; ) {
            hadQueued = true;
        }
        if (hadQueued) {
            refs.removeIf(wr -> wr.get() == null);
        }
    }

    @Override
    public boolean add(T element) {
        cleanup();
        refs.add(new WeakReference<>(Objects.requireNonNull(element, "element"), queue));
        return true;
    }

    @Override
    public int size() {
        // online live refs
        int count = 0;
        for (int i = 0; i < refs.size(); i++) {
            if (refs.get(i).get() != null) count++;
        }
        return count;
    }

    @Override
    public boolean contains(Object o) {
        for (int i = 0; i < refs.size(); i++) {
            T t = refs.get(i).get();
            if (t != null && Objects.equals(t, o)) return true;
        }
        return false;
    }

    @Override
    public Iterator<T> iterator() {
        return new Iterator<>() {
            int i = 0;
            T next = null;

            private void advance() {
                next = null;
                while (i < refs.size()) {
                    T t = refs.get(i++).get();
                    if (t != null) {
                        next = t;
                        return;
                    }
                }
            }

            @Override
            public boolean hasNext() {
                if (next == null) advance();
                return next != null;
            }

            @Override
            public T next() {
                if (!hasNext()) throw new NoSuchElementException();
                T r = next;
                next = null;
                return r;
            }
        };
    }
}
