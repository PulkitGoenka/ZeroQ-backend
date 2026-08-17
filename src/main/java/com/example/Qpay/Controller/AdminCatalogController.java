package com.example.Qpay.Controller;

import com.example.Qpay.Document.ProductDocument;
import com.example.Qpay.Entity.Brands;
import com.example.Qpay.Entity.Stores;
import com.example.Qpay.Repository.BrandsRepository;
import com.example.Qpay.Repository.StoreRepository;
import com.example.Qpay.Repository.mongo.ProductMongoRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@CrossOrigin(origins = "*")
public class AdminCatalogController {

    @Autowired private BrandsRepository brandsRepository;
    @Autowired private StoreRepository storeRepository;
    @Autowired private ProductMongoRepository productMongoRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // ── helpers to convert customFields Map <-> JSON string ──────
    private String mapToJson(Object map) {
        try { return objectMapper.writeValueAsString(map == null ? new HashMap<>() : map); }
        catch (Exception e) { return "{}"; }
    }
    @SuppressWarnings("unchecked")
    private Map<String, Object> jsonToMap(String json) {
        try { return json == null || json.isBlank() ? new HashMap<>() : objectMapper.readValue(json, Map.class); }
        catch (Exception e) { return new HashMap<>(); }
    }

    // ══════════════════════════════════════════════════════════
    //  BRANDS
    // ══════════════════════════════════════════════════════════

    @GetMapping("/brands")
    public List<Map<String, Object>> getAllBrands() {
        return brandsRepository.findAll().stream().map(this::brandToMap).toList();
    }

    private Map<String, Object> brandToMap(Brands b) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", b.getId());
        m.put("name", b.getName());
        m.put("slug", b.getSlug());
        m.put("description", b.getDescription());
        m.put("logoUrl", b.getLogoUrl());
        m.put("isActive", b.getIsActive());
        m.put("customFields", jsonToMap(b.getCustomFields()));
        return m;
    }

    @PostMapping("/brands")
    public Map<String, Object> createBrand(@RequestBody Map<String, Object> body) {
        Brands brand = Brands.builder()
                .name((String) body.get("name"))
                .slug((String) body.get("slug"))
                .description((String) body.get("description"))
                .logoUrl((String) body.get("logoUrl"))
                .isActive(true)
                .customFields(mapToJson(body.get("customFields")))
                .build();
        return brandToMap(brandsRepository.save(brand));
    }

    @PutMapping("/brands/{id}")
    public Map<String, Object> updateBrand(@PathVariable UUID id, @RequestBody Map<String, Object> body) {
        Brands existing = brandsRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Brand not found"));
        if (body.get("name") != null) existing.setName((String) body.get("name"));
        if (body.get("slug") != null) existing.setSlug((String) body.get("slug"));
        if (body.get("description") != null) existing.setDescription((String) body.get("description"));
        if (body.get("logoUrl") != null) existing.setLogoUrl((String) body.get("logoUrl"));
        if (body.get("isActive") != null) existing.setIsActive((Boolean) body.get("isActive"));
        if (body.get("customFields") != null) existing.setCustomFields(mapToJson(body.get("customFields")));
        return brandToMap(brandsRepository.save(existing));
    }

    @DeleteMapping("/brands/{id}")
    public ResponseEntity<?> deleteBrand(@PathVariable UUID id) {
        brandsRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("success", true));
    }

    // ══════════════════════════════════════════════════════════
    //  STORES
    // ══════════════════════════════════════════════════════════

    @GetMapping("/stores")
    public List<Map<String, Object>> getAllStores() {
        return storeRepository.findAll().stream().map(this::storeToMap).toList();
    }

    private Map<String, Object> storeToMap(Stores s) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", s.getId());
        m.put("brand", s.getBrand() == null ? null : Map.of("id", s.getBrand().getId(), "name", s.getBrand().getName()));
        m.put("name", s.getName());
        m.put("address", s.getAddress());
        m.put("city", s.getCity());
        m.put("district", s.getDistrict());
        m.put("state", s.getState());
        m.put("pincode", s.getPincode());
        m.put("isActive", s.getIsActive());
        m.put("customFields", jsonToMap(s.getCustomFields()));
        return m;
    }

    @PostMapping("/stores")
    public Map<String, Object> createStore(@RequestBody Map<String, Object> body) {
        Brands brand = brandsRepository.findById(UUID.fromString((String) body.get("brandId")))
                .orElseThrow(() -> new NoSuchElementException("Brand not found"));

        Stores store = Stores.builder()
                .brand(brand)
                .name((String) body.get("name"))
                .address((String) body.get("address"))
                .city((String) body.get("city"))
                .district((String) body.get("district"))
                .state((String) body.get("state"))
                .pincode((String) body.get("pincode"))
                .isActive(true)
                .customFields(mapToJson(body.get("customFields")))
                .build();
        return storeToMap(storeRepository.save(store));
    }

    @PutMapping("/stores/{id}")
    public Map<String, Object> updateStore(@PathVariable UUID id, @RequestBody Map<String, Object> body) {
        Stores existing = storeRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Store not found"));

        if (body.get("brandId") != null) {
            Brands brand = brandsRepository.findById(UUID.fromString((String) body.get("brandId")))
                    .orElseThrow(() -> new NoSuchElementException("Brand not found"));
            existing.setBrand(brand);
        }
        if (body.get("name") != null) existing.setName((String) body.get("name"));
        if (body.get("address") != null) existing.setAddress((String) body.get("address"));
        if (body.get("city") != null) existing.setCity((String) body.get("city"));
        if (body.get("district") != null) existing.setDistrict((String) body.get("district"));
        if (body.get("state") != null) existing.setState((String) body.get("state"));
        if (body.get("pincode") != null) existing.setPincode((String) body.get("pincode"));
        if (body.get("isActive") != null) existing.setIsActive((Boolean) body.get("isActive"));
        if (body.get("customFields") != null) existing.setCustomFields(mapToJson(body.get("customFields")));

        return storeToMap(storeRepository.save(existing));
    }

    @DeleteMapping("/stores/{id}")
    public ResponseEntity<?> deleteStore(@PathVariable UUID id) {
        storeRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("success", true));
    }

    // ══════════════════════════════════════════════════════════
    //  PRODUCTS (MongoDB — customFields already just a Map, no conversion needed)
    // ══════════════════════════════════════════════════════════

    @GetMapping("/products")
    public List<ProductDocument> getAllProducts() {
        return productMongoRepository.findAll();
    }

    @PostMapping("/products")
    public ProductDocument createProduct(@RequestBody ProductDocument product) {
        product.setId(null);
        product.setCreatedAt(java.time.OffsetDateTime.now());
        product.setUpdatedAt(java.time.OffsetDateTime.now());
        return productMongoRepository.save(product);
    }

    @PutMapping("/products/{id}")
    public ProductDocument updateProduct(@PathVariable String id, @RequestBody ProductDocument updated) {
        ProductDocument existing = productMongoRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Product not found"));

        existing.setBarcode(updated.getBarcode());
        existing.setBrandSlug(updated.getBrandSlug());
        existing.setBrandId(updated.getBrandId());
        existing.setName(updated.getName());
        existing.setDescription(updated.getDescription());
        existing.setMrp(updated.getMrp());
        existing.setImageUrl(updated.getImageUrl());
        existing.setUnit(updated.getUnit());
        existing.setActive(updated.isActive());
        existing.setStorePrices(updated.getStorePrices());
        existing.setCustomFields(updated.getCustomFields());
        existing.setUpdatedAt(java.time.OffsetDateTime.now());

        return productMongoRepository.save(existing);
    }

    @DeleteMapping("/products/{id}")
    public ResponseEntity<?> deleteProduct(@PathVariable String id) {
        productMongoRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("success", true));
    }
}