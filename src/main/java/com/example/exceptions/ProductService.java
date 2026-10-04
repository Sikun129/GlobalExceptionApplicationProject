package com.example.exceptions;

import java.math.BigDecimal;
import org.springframework.stereotype.Service;

@Service
public class ProductService {
    public record Product(long id, String name, BigDecimal price, int stock) {}

    public Product find(long id) {
        if (id != 1) {
            throw new ProductNotFoundException(id);
        }
        return new Product(1, "Wireless Keyboard", new BigDecimal("2499.00"), 5);
    }

    public Product checkStock(long id, int quantity) {
        Product product = find(id);
        if (quantity > product.stock()) {
            throw new InsufficientStockException(product.stock());
        }
        return product;
    }
}
