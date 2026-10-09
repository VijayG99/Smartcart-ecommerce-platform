package com.smartcart.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SmartSearchServiceTest {

    @Test
    void interpretsKeywordsPriceLimitAndCheapIntent() {
        var result = SmartSearchService.interpret("Show me affordable wireless headphones for office use under 3000");

        assertEquals(List.of("wireless", "headphone", "office"), result.keywords());
        assertEquals(0, new BigDecimal("3000").compareTo(result.max()));
        assertTrue(result.cheapFirst());
        assertNull(result.min());
    }

    @Test
    void understandsMinimumPrice() {
        var result = SmartSearchService.interpret("keyboard above 2000");
        assertEquals(List.of("keyboard"), result.keywords());
        assertEquals(0, new BigDecimal("2000").compareTo(result.min()));
    }
}
