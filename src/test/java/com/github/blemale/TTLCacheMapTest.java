package com.github.blemale;

import org.junit.Ignore;
import org.junit.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.LongAdder;

import static java.util.Objects.hash;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.assertj.core.api.Assertions.assertThat;

public class TTLCacheMapTest {

    @Test
    public void should_cache_result() throws Exception {
        // Given
        HashService service = new HashService();

        // When
        service.computeHash("toto");
        service.computeHash("toto");

        // Then
        assertThat(service.callsCount()).isEqualTo(1);
    }

    @Test
    @Ignore
    public void should_handle_population_by_multiple_threads() throws Exception {
        // Given
        HashService service = new HashService();
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

    public static class HashService {
        private final TTLCacheMap<String, Integer> cache = new TTLCacheMap<>(1_000, false, 10);
        private final LongAdder counter = new LongAdder();

        public int computeHash(String string) {
            Integer cachedHash = cache.get(string);
            if (cachedHash != null) {
                return cachedHash;
            } else {
                int computedHash = computeComplicatedHash(string);
                cache.put(string, computedHash);
                return computedHash;
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



}