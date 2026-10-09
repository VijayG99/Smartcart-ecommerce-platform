package com.smartcart.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Programmatic eviction used when stock changes (inside InventoryService).
 *
 * Eviction happens AFTER the database transaction commits. If we evicted before the commit, another
 * request could re-read the old row, put it back into Redis, and the cache would stay stale until the TTL.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductCache {
    public static final String PRODUCTS = "products";

    private final CacheManager cacheManager;

    public void evictAfterCommit(Long productId) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { evictNow(productId); }
            });
        } else {
            evictNow(productId);
        }
    }

    private void evictNow(Long productId) {
        try {
            Cache cache = cacheManager.getCache(PRODUCTS);
            if (cache != null) {
                cache.evict(productId);
                log.debug("Evicted product {} from cache", productId);
            }
        } catch (RuntimeException e) {
            log.error("Could not evict product {} from cache; TTL will clean it up: {}", productId, e.getMessage());
        }
    }
}
