package com.liziyi0914.mci;

import com.liziyi0914.mci.bean.InstallContext;

import java.util.*;

public class Identifier<T> {

    long n;

    Class<T> clz;

    private static final Random rnd = new Random();

    public static <S> Identifier<S> of(Class<S> clz) {
        return new Identifier<>(clz);
    }

    public static <S> Identifier<List<S>> list(Class<S> clz) {
        List<S> l = new ArrayList<>();
        return new Identifier<>((Class<List<S>>)l.getClass());
    }

    public static <K,V> Identifier<Map<K,V>> map(Class<K> clzKey, Class<V> clzValue) {
        Map<K,V> m = new HashMap<>();
        return new Identifier<>((Class<Map<K,V>>)m.getClass());
    }

    public Identifier(Class<T> clz) {
        this.n = rnd.nextLong();
        this.clz = clz;
    }

    public long getN() {
        return n;
    }

    public T cast(Object o) {
        return clz.cast(o);
    }

    public void putIntoContext(InstallContext ctx, Object value) {
        ctx.put(this, (T)value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Identifier<?> that = (Identifier<?>) o;
        return n == that.n && clz.equals(that.clz);
    }

    @Override
    public int hashCode() {
        return Objects.hash(n, clz);
    }

    @Override
    public String toString() {
        return "{"+clz.getSimpleName()+"#"+n+"}";
    }
}
