package com.smartcart.config;

import com.smartcart.dto.CategoryRequest;
import com.smartcart.dto.CategoryResponse;
import com.smartcart.dto.ProductRequest;
import com.smartcart.entity.Role;
import com.smartcart.entity.User;
import com.smartcart.repository.CategoryRepository;
import com.smartcart.repository.UserRepository;
import com.smartcart.service.CategoryService;
import com.smartcart.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

/** Creates the first ADMIN account and a few demo products so the app is usable right after startup. */
@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final CategoryService categoryService;
    private final ProductService productService;
    private final PasswordEncoder passwordEncoder;

    @Value("${smartcart.seed.enabled}") private boolean seedEnabled;
    @Value("${smartcart.seed.admin-email}") private String adminEmail;
    @Value("${smartcart.seed.admin-password}") private String adminPassword;

    @Override
    public void run(ApplicationArguments args) {
        if (!seedEnabled) return;
        if (!userRepository.existsByEmail(adminEmail.toLowerCase())) {
            userRepository.save(User.builder().name("Store Admin").email(adminEmail.toLowerCase())
                    .password(passwordEncoder.encode(adminPassword)).role(Role.ADMIN).build());
            log.warn("Created ADMIN account {} - change ADMIN_EMAIL / ADMIN_PASSWORD outside local development", adminEmail);
        }
        if (categoryRepository.count() == 0) seedCatalog();
    }

    private void seedCatalog() {
        Map<String, CategoryResponse> cats = Map.of(
                "Electronics", categoryService.create(new CategoryRequest("Electronics", "Gadgets and accessories")),
                "Books", categoryService.create(new CategoryRequest("Books", "Technical and general reading")),
                "Home & Kitchen", categoryService.create(new CategoryRequest("Home & Kitchen", "For your home")),
                "Fitness", categoryService.create(new CategoryRequest("Fitness", "Training and outdoor gear")));

        add(cats, "Electronics", "Wireless Headphones", "Bluetooth over-ear headphones, comfortable for long office hours", "2499.00", "ELEC-HP-001", 25);
        add(cats, "Electronics", "Budget Wired Earphones", "Basic wired earphones with microphone", "399.00", "ELEC-EP-002", 80);
        add(cats, "Electronics", "Mechanical Keyboard", "Compact wireless mechanical keyboard for office and gaming", "3299.00", "ELEC-KB-003", 15);
        add(cats, "Electronics", "USB-C Hub", "7-in-1 USB-C hub with HDMI and card reader", "1899.00", "ELEC-HB-004", 4);
        add(cats, "Books", "Effective Java", "Best practices for the Java platform", "899.00", "BOOK-EJ-005", 40);
        add(cats, "Books", "Designing Data-Intensive Applications", "Reliable, scalable and maintainable systems", "1299.00", "BOOK-DD-006", 30);
        add(cats, "Home & Kitchen", "Stainless Steel Water Bottle", "1 litre insulated bottle, keeps drinks cold for 24 hours", "749.00", "HOME-WB-007", 60);
        add(cats, "Fitness", "Yoga Mat", "6mm non-slip yoga mat", "999.00", "FIT-YM-008", 35);
        log.info("Seeded demo catalog");
    }

    private void add(Map<String, CategoryResponse> cats, String cat, String name, String desc, String price, String sku, int qty) {
        productService.create(new ProductRequest(name, desc, new BigDecimal(price), sku, cats.get(cat).id(), qty));
    }
}
