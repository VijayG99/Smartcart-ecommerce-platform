package com.smartcart.integration;

import java.util.UUID;

final class TestData {
    private TestData() {}
    static String email() { return "user-" + UUID.randomUUID() + "@test.com"; }
    static String sku() { return "SKU-" + UUID.randomUUID().toString().substring(0, 12); }
    static String name(String prefix) { return prefix + "-" + UUID.randomUUID().toString().substring(0, 8); }
}
