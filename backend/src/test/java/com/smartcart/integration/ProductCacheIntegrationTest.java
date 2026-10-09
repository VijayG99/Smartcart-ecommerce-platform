package com.smartcart.integration;

import com.smartcart.dto.ProductRequest;
import com.smartcart.dto.ProductResponse;
import com.smartcart.entity.Category;
import com.smartcart.entity.Product;
import com.smartcart.exception.ResourceNotFoundException;
import com.smartcart.repository.CategoryRepository;
import com.smartcart.repository.ProductRepository;
import com.smartcart.service.InventoryService;
import com.smartcart.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Uses the in-memory cache implementation (profile "test") - the annotations and eviction logic are identical to Redis.
 * Cache hit is proven by changing the database behind the cache's back and still reading the old value.
 */
@SpringBootTest
@ActiveProfiles("test")
class ProductCacheIntegrationTest {
    @Autowired ProductService productService;
    @Autowired InventoryService inventoryService;
    @Autowired ProductRepository productRepository;
    @Autowired CategoryRepository categories;
    @Autowired CacheManager cacheManager;

    Long categoryId;

    @BeforeEach
    void setUp() {
        Category c = categories.save(Category.builder().name(TestData.name("cat")).build());
        categoryId = c.getId();
    }

    private ProductResponse newProduct(String name) {
        return productService.create(new ProductRequest(name, "d", new BigDecimal("10.00"), TestData.sku(), categoryId, 8));
    }

    private Cache cache() { return cacheManager.getCache("products"); }

    @Test
    void firstReadIsAMissThatPopulatesCacheSecondReadIsAHit() {
        ProductResponse created = newProduct("Original");
        assertNull(cache().get(created.getId()), "nothing cached before the first read (cache miss)");

        assertEquals("Original", productService.getById(created.getId()).getName());
        assertNotNull(cache().get(created.getId()), "miss stored the result");

        // change the row WITHOUT going through the service -> cache is not told
        Product row = productRepository.findById(created.getId()).orElseThrow();
        row.setName("Changed in DB");
        productRepository.save(row);

        assertEquals("Original", productService.getById(created.getId()).getName(), "served from cache (hit)");
    }

    @Test
    void updateRefreshesTheCachedValue() {
        ProductResponse created = newProduct("Before");
        productService.getById(created.getId());

        productService.update(created.getId(),
                new ProductRequest("After", "d", new BigDecimal("12.00"), created.getSku(), categoryId, null));

        assertEquals("After", productService.getById(created.getId()).getName());
        assertEquals(0, new BigDecimal("12.00").compareTo(productService.getById(created.getId()).getPrice()));
    }

    @Test
    void stockChangeEvictsTheCachedProduct() {
        ProductResponse created = newProduct("Stocked");
        assertEquals(8, productService.getById(created.getId()).getAvailableQuantity());

        inventoryService.updateStock(created.getId(), 3);

        assertNull(cache().get(created.getId()), "evicted after commit");
        assertEquals(3, productService.getById(created.getId()).getAvailableQuantity());
    }

    @Test
    void deleteEvictsCacheAndHidesProduct() {
        ProductResponse created = newProduct("Doomed");
        productService.getById(created.getId());

        productService.delete(created.getId());

        assertNull(cache().get(created.getId()));
        assertThrows(ResourceNotFoundException.class, () -> productService.getById(created.getId()));
    }
}
