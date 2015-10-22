/*
 * Copyright (C) by Courtanet, All Rights Reserved.
 */
package com.github.blemale;

import java.util.*;

public class TTLCacheMap<K, T> {

    public static final long CACHE_TTL = 10 * 1000;

    private final Map<K, TTLData<K, T>> cache = new HashMap<>();

    private final long TTL;
    private final int SIZE;
    private boolean autoClear;
    private Timer timer;

    /**
     * @param TTL       en millisecondes
     * @param autoClear Si le cache doit être automatiquement vidé toutes les TTL millisecondes
     * @param size
     */
    public TTLCacheMap(long TTL, boolean autoClear, int size) {
        this.TTL = TTL;
        this.SIZE = size;
        this.autoClear = autoClear;
        this.timer = null;
        if (this.autoClear) {
            this.timer = new Timer();
            this.timer.schedule(new AutoClear(this), TTL, TTL);
        }
    }

    public synchronized T get(K key) {
        if (cache.size() > SIZE)
            removeOutdatedEntries();

        final TTLData<K, T> ttlData = cache.get(key);
        if (ttlData == null)
            return null;
        if (System.currentTimeMillis() - ttlData.time < TTL)
            return ttlData.data;
        cache.remove(key);
        return null;
    }

    public synchronized T put(K key, T t) {
        if (cache.size() > SIZE)
            removeOutdatedEntries();

        final TTLData<K, T> data = new TTLData<K, T>(key, t);
        cache.put(key, data);
        return t;
    }

    public final synchronized void clear() {
        cache.clear();
    }

    public final synchronized int size() {
        return cache.size();
    }

    /**
     * Supprime du cache les entrées dont le temps de vie est dépassé.
     */
    private final synchronized void removeOutdatedEntries() {
        final List<TTLData<K, T>> toRemove = new ArrayList<TTLData<K, T>>(cache.values());
        Collections.sort(toRemove);
        for (TTLData<K, T> data : toRemove) {
            if (System.currentTimeMillis() - data.time > TTL || cache.size() > SIZE)
                cache.remove(data.key);
        }
    }

    private static final class TTLData<K, T> implements Comparable<TTLData<K, T>> {
        private final K key;
        private final T data;
        private final long time;

        TTLData(K key, T data) {
            this.key = key;
            this.data = data;
            this.time = System.currentTimeMillis();
        }

        @Override
        public int compareTo(TTLData<K, T> o) {
            return this.time < o.time ? -1 : (this.time == o.time ? 0 : 1);
        }

        @Override
        public String toString() {
            return Long.toString(time);
        }
    }

    private static final class AutoClear extends TimerTask {
        private TTLCacheMap<?, ?> cache;

        public AutoClear(TTLCacheMap<?, ?> cache) {
            this.cache = cache;
        }

        @Override
        public void run() {
            this.cache.removeOutdatedEntries();
        }
    }
}