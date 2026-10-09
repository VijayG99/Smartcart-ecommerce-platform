package com.smartcart.service.impl;

import com.smartcart.cache.ProductCache;
import com.smartcart.dto.ProductRequest;
import com.smartcart.dto.ProductResponse;
import com.smartcart.dto.ProductSearchCriteria;
import com.smartcart.entity.Category;
import com.smartcart.entity.Inventory;
import com.smartcart.entity.Product;
import com.smartcart.exception.DuplicateResourceException;
import com.smartcart.exception.ResourceNotFoundException;
import com.smartcart.mapper.ProductMapper;
import com.smartcart.repository.CategoryRepository;
import com.smartcart.repository.InventoryRepository;
import com.smartcart.repository.ProductRepository;
import com.smartcart.service.InventoryService;
import com.smartcart.service.ProductService;
import com.smartcart.util.ProductSpecifications;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final InventoryRepository inventoryRepository;
    private final InventoryService inventoryService;

    @Override
    @Transactional
    public ProductResponse create(ProductRequest r) {
        if (productRepository.existsBySku(r.sku().trim())) {
            throw new DuplicateResourceException("SKU already exists: " + r.sku());
        }
        Product product = Product.builder()
                .name(r.name().trim()).description(r.description()).price(r.price())
                .sku(r.sku().trim()).category(findCategory(r.categoryId()))
                .build();
        productRepository.save(product);
        int quantity = r.initialQuantity() == null ? 0 : r.initialQuantity();
        inventoryService.createFor(product, quantity);
        log.info("Created product id={} sku={}", product.getId(), product.getSku());
        return ProductMapper.toResponse(product, quantity);
    }

    /**
     * Cache-aside read.
     *  - Cache HIT : Spring returns the value from Redis and this method body never runs.
     *  - Cache MISS: the body runs (PostgreSQL query) and Spring stores the result in Redis with a TTL.
     */
    @Override
    @Cacheable(cacheNames = ProductCache.PRODUCTS, key = "#id")
    @Transactional(readOnly = true)
    public ProductResponse getById(Long id) {
        log.info("Cache MISS for product {} - loading from PostgreSQL", id);
        Product product = productRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
        return ProductMapper.toResponse(product, quantityOf(id));
    }

    /** Write-through: the fresh value replaces the cached one, so readers never see the old price. */
    @Override
    @CachePut(cacheNames = ProductCache.PRODUCTS, key = "#id")
    @Transactional
    public ProductResponse update(Long id, ProductRequest r) {
        Product product = productRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
        if (productRepository.existsBySkuAndIdNot(r.sku().trim(), id)) {
            throw new DuplicateResourceException("SKU already exists: " + r.sku());
        }
        product.setName(r.name().trim());
        product.setDescription(r.description());
        product.setPrice(r.price());
        product.setSku(r.sku().trim());
        product.setCategory(findCategory(r.categoryId()));
        productRepository.flush();   // so updatedAt is populated in the response we cache
        return ProductMapper.toResponse(product, quantityOf(id));
    }

    @Override
    @CacheEvict(cacheNames = ProductCache.PRODUCTS, key = "#id")
    @Transactional
    public void delete(Long id) {
        Product product = productRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
        product.setActive(false);   // soft delete keeps order history intact
        log.info("Deactivated product id={}", id);
    }

    /** Search results vary per query, so they are not cached (only the hot single-product lookup is). */
    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> search(ProductSearchCriteria criteria, Pageable pageable) {
        Page<Product> page = productRepository.findAll(ProductSpecifications.fromCriteria(criteria), pageable);
        Map<Long, Integer> stock = inventoryRepository
                .findByProductIdIn(page.getContent().stream().map(Product::getId).toList())
                .stream().collect(Collectors.toMap(i -> i.getProduct().getId(), Inventory::getQuantity));
        return page.map(p -> ProductMapper.toResponse(p, stock.getOrDefault(p.getId(), 0)));
    }

    private int quantityOf(Long productId) {
        return inventoryRepository.findByProductId(productId).map(Inventory::getQuantity).orElse(0);
    }

    private Category findCategory(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + id));
    }
}
