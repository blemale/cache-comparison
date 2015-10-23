/*
 * Copyright (C) by Courtanet, All Rights Reserved.
 */
package com.github.blemale;

import static java.util.Objects.hash;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import org.junit.Test;
import com.google.common.base.Ticker;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.ListenableFuture;

public class GuavaCacheTest {

    public static class HashServiceWithGuavaCache {
        private final Cache<String, Integer> cache =
            CacheBuilder.newBuilder().expireAfterWrite(1, SECONDS).maximumSize(10).build();

        private final LongAdder counter = new LongAdder();

        public Integer computeHash(String string) {
            try {
                return cache.get(string, () -> computeComplicatedHash(string));
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            }
        }

        public long callsCount() {
            return counter.sum();
        }

        private int computeComplicatedHash(String string) {
            counter.increment();
            sleep(100);
            return hash(string);
        }

        private void sleep(int millis) {
            try {
                Thread.sleep(millis);
            } catch (InterruptedException e) {
                System.out.println("Interrupted");
            }
        }
    }

    @Test
    public void should_cache_result_with_cache() throws Exception {
        // Given
        HashServiceWithGuavaCache service = new HashServiceWithGuavaCache();

        // When
        service.computeHash("toto");
        service.computeHash("toto");

        // Then
        assertThat(service.callsCount()).isEqualTo(1);
    }

    public static class HashServiceWithGuavaLoadingCache {
        private final LoadingCache<String, Integer> cache;

        private final LongAdder counter = new LongAdder();
        private final LongAdder refreshCounter = new LongAdder();

        public HashServiceWithGuavaLoadingCache() {
            cache =
                CacheBuilder.newBuilder().refreshAfterWrite(1, SECONDS).maximumSize(10).recordStats().build(
                    new CacheLoader<String, Integer>() {
                        @Override
                        public Integer load(String key) throws Exception {
                            return computeComplicatedHash(key);
                        }

                        @Override
                        public ListenableFuture<Integer> reload(String key, Integer oldValue) throws Exception {
                            refreshCounter.increment();
                            throw new RuntimeException("Refresh failed for " + key);
                        }
                    }
                );
        }

        public Integer computeHash(String string) {
            return cache.getUnchecked(string);
        }

        public long callsCount() {
            return counter.sum();
        }

        public long refreshCount() {
            return refreshCounter.sum();
        }

        private int computeComplicatedHash(String string) {
            counter.increment();
            sleep(100);
            return hash(string);
        }

        private void sleep(int millis) {
            try {
                Thread.sleep(millis);
            } catch (InterruptedException e) {
                System.out.println("Interrupted");
            }
        }
    }

    @Test
    public void should_cache_result_with_loading_cache() throws Exception {
        // Given
        HashServiceWithGuavaLoadingCache service = new HashServiceWithGuavaLoadingCache();

        // When
        service.computeHash("toto");
        service.computeHash("toto");

        // Then
        assertThat(service.callsCount()).isEqualTo(1);
    }

    @Test
    public void should_handle_population_by_multiple_threads() throws Exception {
        // Given
        HashServiceWithGuavaLoadingCache service = new HashServiceWithGuavaLoadingCache();
        ExecutorService executorService = Executors.newFixedThreadPool(4);

        // When
        CompletableFuture<Void> future1 = CompletableFuture.runAsync(() -> service.computeHash("toto"), executorService);
        CompletableFuture<Void> future2 = CompletableFuture.runAsync(() -> service.computeHash("toto"), executorService);
        CompletableFuture<Void> future3 = CompletableFuture.runAsync(() -> service.computeHash("toto"), executorService);
        CompletableFuture<Void> future4 = CompletableFuture.runAsync(() -> service.computeHash("toto"), executorService);
        CompletableFuture.allOf(future1, future2, future3, future4).get(200, MILLISECONDS);

        // Then
        assertThat(service.callsCount()).isEqualTo(1);
    }

    @Test
    public void should_refresh_value() throws Exception {
        // Given
        HashServiceWithGuavaLoadingCache service = new HashServiceWithGuavaLoadingCache();

        // When
        service.computeHash("toto");

        Thread.sleep(2_000);

        Integer hash = service.computeHash("toto");

        // Then
        assertThat(service.refreshCount()).isEqualTo(1);
        assertThat(hash).isNotNull();
    }

    @Test
    public void should_collect_stats() throws Exception {
        // Given
        HashServiceWithGuavaLoadingCache service = new HashServiceWithGuavaLoadingCache();

        // When
        service.computeHash("toto");
        service.computeHash("toto");
        service.computeHash("toto");

        // Then
        assertThat(service.cache.stats().hitCount()).isEqualTo(2);
        System.out.println(service.cache.stats());
    }

    private final Ticker ticker = new FixedTicker();

    public static class FixedTicker extends Ticker {
        private final AtomicLong time = new AtomicLong();

        @Override
        public long read() {
            return time.get();
        }

        public void addMillis(long millis) {
            time.addAndGet(millis);
        }
    }

}
