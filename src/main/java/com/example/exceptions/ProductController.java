package com.example.exceptions;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    private final ProductService products;

    public ProductController(ProductService products) {
        this.products = products;
    }

    @GetMapping("/{id}")
    public ProductService.Product find(@PathVariable long id) {
        // No try/catch: exceptions propagate to the shared advice.
        return products.find(id);
    }

    @GetMapping("/demo-failure")
    public ProductService.Product simulateFailure() {
        // Deliberate failure for the presentation; this detail stays in server logs.
        throw new IllegalStateException("Simulated inventory database connection failure");
    }
}
