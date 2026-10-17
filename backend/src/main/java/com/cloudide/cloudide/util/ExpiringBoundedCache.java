package com.cloudide.cloudide.util;

import java.time.Duration;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Thread-safe, size-bounded, time-expiring cache.
 * When the cache reaches maxCapacity, the oldest entries are evicted.
 * Entries older than ttlMillis expire and return null upon access or cleanup.
 *
 * @param <K> Key type
 * @param <V> Value type
 */
public class ExpiringBoundedCache<K, V> {

    private final int maxCapacity;
    private final long ttlMillis;
    private final ConcurrentHashMap<K, CacheEntry<V>> map = new ConcurrentHashMap<>();
    private final ConcurrentLinkedDeque<K> orderQueue = new ConcurrentLinkedDeque<>();

    public record CacheEntry<V>(V value, long createdAt) {
        public boolean isExpired(long now, long ttl) {
            return (now - createdAt) > ttl;
        }
    }

    public ExpiringBoundedCache(int maxCapacity, Duration ttl) {
        if (maxCapacity <= 0) {
            throw new IllegalArgumentException("maxCapacity must be > 0");
        }
        if (ttl == null || ttl.isNegative() || ttl.isZero()) {
            throw new IllegalArgumentException("TTL must be positive");
        }
        this.maxCapacity = maxCapacity;
        this.ttlMillis = ttl.toMillis();
    }

    public V get(K key) {
        if (key == null) return null;
        CacheEntry<V> entry = map.get(key);
        if (entry == null) return null;

        long now = System.currentTimeMillis();
        if (entry.isExpired(now, ttlMillis)) {
            remove(key);
            return null;
        }
        return entry.value();
    }

    public void put(K key, V value) {
        if (key == null || value == null) return;
        long now = System.currentTimeMillis();
        CacheEntry<V> newEntry = new CacheEntry<>(value, now);

        if (map.containsKey(key)) {
            map.put(key, newEntry);
            return;
        }

        // Evict expired entries if approaching max capacity
        if (map.size() >= maxCapacity) {
            cleanUp(now);
        }

        // If still at capacity, evict oldest entry to enforce hard bound
        while (map.size() >= maxCapacity) {
            K oldest = orderQueue.poll();
            if (oldest == null) break;
            map.remove(oldest);
        }

        map.put(key, newEntry);
        orderQueue.offer(key);
    }

    public V remove(K key) {
        if (key == null) return null;
        CacheEntry<V> removed = map.remove(key);
        if (removed != null) {
            orderQueue.remove(key);
            return removed.value();
        }
        return null;
    }

    public boolean containsKey(K key) {
        return get(key) != null;
    }

    public int size() {
        cleanUp(System.currentTimeMillis());
        return map.size();
    }

    public void cleanUp() {
        cleanUp(System.currentTimeMillis());
    }

    public void cleanUp(long now) {
        Iterator<Map.Entry<K, CacheEntry<V>>> it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<K, CacheEntry<V>> entry = it.next();
            if (entry.getValue().isExpired(now, ttlMillis)) {
                it.remove();
                orderQueue.remove(entry.getKey());
            }
        }
    }

    public void clear() {
        map.clear();
        orderQueue.clear();
    }

    public int getMaxCapacity() {
        return maxCapacity;
    }

    public long getTtlMillis() {
        return ttlMillis;
    }
}
