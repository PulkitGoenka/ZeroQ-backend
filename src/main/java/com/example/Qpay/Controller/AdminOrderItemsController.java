package com.example.Qpay.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Returns the line items for one order — i.e. exactly what a customer bought
 * (product name, barcode, quantity, price, line total).
 *
 * Uses JdbcTemplate with a plain SQL query against order_items so it works
 * regardless of how OrderItem.java maps its relations.
 */
@RestController
@RequestMapping("/api/v1/admin/orders")
@CrossOrigin(origins = "*")
public class AdminOrderItemsController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @GetMapping("/{orderId}/items")
    public List<Map<String, Object>> getOrderItems(@PathVariable String orderId) {
        return jdbcTemplate.queryForList(
                "SELECT id, barcode, product_name, product_mongo_id, quantity, mrp, discount_price, line_total " +
                        "FROM order_items WHERE order_id = ?::uuid",
                orderId
        );
    }
}