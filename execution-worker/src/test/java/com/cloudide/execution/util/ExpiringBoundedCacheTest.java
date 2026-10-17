package com.cloudide.execution.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class ExpiringBoundedCacheTest {

    @Test
    @DisplayName("Cache enforces maximum capacity by evicting oldest entries")
    void testCapacityBoundedEviction() {
        ExpiringBoundedCache<String, String> cache = new ExpiringBoundedCache<>(3, Duration.ofMinutes(5));

        cache.put("k1", "v1");
        cache.put("k2", "v2");
        cache.put("k3", "v3");

        assertEquals(3, cache.size());
        assertEquals("v1", cache.get("k1"));
        assertEquals("v2", cache.get("k2"));
        assertEquals("v3", cache.get("k3"));

        // Inserting 4th item must evict oldest ("k1")
        cache.put("k4", "v4");

        assertEquals(3, cache.size());
        assertNull(cache.get("k1"), "k1 should have been evicted due to capacity bound");
        assertEquals("v2", cache.get("k2"));
        assertEquals("v3", cache.get("k3"));
        assertEquals("v4", cache.get("k4"));
    }

    @Test
    @DisplayName("Cache expires entries after configured TTL")
    void testTtlExpiration() throws InterruptedException {
        // Cache with 80ms TTL
        ExpiringBoundedCache<String, String> cache = new ExpiringBoundedCache<>(10, Duration.ofMillis(80));

        cache.put("exp1", "val1");
        assertEquals("val1", cache.get("exp1"));

        // Wait for expiration
        Thread.sleep(120);

        assertNull(cache.get("exp1"), "Entry should expire after TTL");
        assertEquals(0, cache.size(), "Expired entry should be cleaned up from size");
    }

    @Test
    @DisplayName("Updating an existing key refreshes value without evicting other keys")
    void testUpdateExistingKey() {
        ExpiringBoundedCache<String, String> cache = new ExpiringBoundedCache<>(2, Duration.ofMinutes(5));

        cache.put("a", "alpha");
        cache.put("b", "bravo");

        // Update 'a'
        cache.put("a", "alpha-updated");

        assertEquals(2, cache.size());
        assertEquals("alpha-updated", cache.get("a"));
        assertEquals("bravo", cache.get("b"));
    }

    @Test
    @DisplayName("Explicit cleanup removes expired items and retains unexpired items")
    void testCleanUpMethod() throws InterruptedException {
        ExpiringBoundedCache<String, String> cache = new ExpiringBoundedCache<>(10, Duration.ofMillis(80));

        cache.put("expired1", "val1");
        cache.put("expired2", "val2");

        Thread.sleep(120);

        cache.cleanUp();

        assertEquals(0, cache.size());
        assertNull(cache.get("expired1"));
        assertNull(cache.get("expired2"));
    }

    @Test
    @DisplayName("Concurrent puts and gets maintain thread-safety and bounded size")
    void testConcurrentThreadSafety() throws InterruptedException {
        int capacity = 50;
        ExpiringBoundedCache<Integer, String> cache = new ExpiringBoundedCache<>(capacity, Duration.ofMinutes(1));

        int threadCount = 8;
        int operationsPerThread = 100;
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            pool.submit(() -> {
                try {
                    for (int i = 0; i < operationsPerThread; i++) {
                        int key = (threadId * operationsPerThread) + i;
                        cache.put(key, "val-" + key);
                        cache.get(key);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        pool.shutdownNow();

        assertTrue(cache.size() <= capacity, "Cache size must never exceed capacity under concurrent puts");
    }
}
