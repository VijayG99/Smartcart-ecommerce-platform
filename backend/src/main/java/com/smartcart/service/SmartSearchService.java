package com.smartcart.service;

import com.smartcart.dto.ProductResponse;
import com.smartcart.dto.SmartSearchResponse;
import com.smartcart.entity.Inventory;
import com.smartcart.entity.Product;
import com.smartcart.mapper.ProductMapper;
import com.smartcart.repository.InventoryRepository;
import com.smartcart.repository.ProductRepository;
import com.smartcart.util.ProductSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * OPTIONAL EXTENSION - natural-language product search.
 *
 * This is a transparent, rule-based query interpreter, NOT a machine-learning model:
 *   "affordable wireless headphones for office use under 3000"
 *   -> keywords [wireless, headphone, office], maxPrice 3000, cheapest first
 * Results are ranked by how many keywords match. The response shows the interpretation, so it is explainable.
 * The interpretation step is isolated in {@link #interpret(String)}; it is the single place where an LLM call
 * could later replace the rules without touching the rest of the application.
 */
@Service
@RequiredArgsConstructor
public class SmartSearchService {
    private static final Pattern MAX = Pattern.compile("(?:under|below|less than|upto|up to|max|within)\\s*[$₹]?\\s*(\\d+(?:\\.\\d+)?)");
    private static final Pattern MIN = Pattern.compile("(?:over|above|more than|at least|min)\\s*[$₹]?\\s*(\\d+(?:\\.\\d+)?)");
    private static final Set<String> CHEAP_WORDS = Set.of("cheap", "affordable", "budget", "inexpensive", "economical");
    private static final Set<String> STOP_WORDS = Set.of("show", "find", "get", "give", "the", "and", "for", "with", "that",
            "need", "want", "looking", "some", "good", "best", "use", "using", "please", "are", "any", "can", "you", "have",
            "under", "below", "than", "less", "over", "above", "more", "least", "within", "upto");

    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;

    @Transactional(readOnly = true)
    public SmartSearchResponse search(String query) {
        Interpretation in = interpret(query == null ? "" : query);
        if (in.keywords().isEmpty()) {
            return new SmartSearchResponse(query, in.keywords(), in.min(), in.max(), in.cheapFirst(), List.of());
        }
        List<Product> candidates = productRepository.findAll(
                ProductSpecifications.anyKeyword(in.keywords()).and((r, q, cb) -> cb.isTrue(r.get("active"))),
                PageRequest.of(0, 50, Sort.by("name"))).getContent().stream()
                .filter(p -> in.min() == null || p.getPrice().compareTo(in.min()) >= 0)
                .filter(p -> in.max() == null || p.getPrice().compareTo(in.max()) <= 0)
                .toList();

        Map<Long, Integer> stock = inventoryRepository.findByProductIdIn(candidates.stream().map(Product::getId).toList())
                .stream().collect(Collectors.toMap(i -> i.getProduct().getId(), Inventory::getQuantity));

        Comparator<Product> byScore = Comparator.comparingInt((Product p) -> score(p, in.keywords())).reversed();
        Comparator<Product> order = in.cheapFirst() ? byScore.thenComparing(Product::getPrice) : byScore;
        List<ProductResponse> results = candidates.stream().sorted(order).limit(12)
                .map(p -> ProductMapper.toResponse(p, stock.getOrDefault(p.getId(), 0))).toList();
        return new SmartSearchResponse(query, in.keywords(), in.min(), in.max(), in.cheapFirst(), results);
    }

    static Interpretation interpret(String raw) {
        String text = raw.toLowerCase(Locale.ROOT);
        BigDecimal max = firstNumber(MAX, text);
        BigDecimal min = firstNumber(MIN, text);
        text = MAX.matcher(text).replaceAll(" ");
        text = MIN.matcher(text).replaceAll(" ");

        boolean cheap = false;
        Set<String> keywords = new LinkedHashSet<>();
        for (String token : text.replaceAll("[^a-z0-9 ]", " ").split("\\s+")) {
            if (token.length() < 3) continue;
            if (CHEAP_WORDS.contains(token)) { cheap = true; continue; }
            if (STOP_WORDS.contains(token) || token.matches("\\d+")) continue;
            keywords.add(token.length() > 4 && token.endsWith("s") ? token.substring(0, token.length() - 1) : token);
        }
        return new Interpretation(new ArrayList<>(keywords), min, max, cheap);
    }

    private static BigDecimal firstNumber(Pattern p, String text) {
        Matcher m = p.matcher(text);
        return m.find() ? new BigDecimal(m.group(1)) : null;
    }

    private static int score(Product p, List<String> keywords) {
        String haystack = (p.getName() + " " + (p.getDescription() == null ? "" : p.getDescription())).toLowerCase(Locale.ROOT);
        return (int) keywords.stream().filter(haystack::contains).count();
    }

    record Interpretation(List<String> keywords, BigDecimal min, BigDecimal max, boolean cheapFirst) {}
}
