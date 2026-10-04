package com.example.exceptions;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    public record OrderRequest(
            @NotNull(message = "productId is required") @Min(value = 1, message = "productId must be at least 1") Long productId,
            @NotNull(message = "quantity is required") @Min(value = 1, message = "quantity must be at least 1") Integer quantity) {}
    public record OrderPreview(String message, ProductService.Product product, int quantity) {}

    private final ProductService products;

    public OrderController(ProductService products) {
        this.products = products;
    }

    @PostMapping("/preview")
    public OrderPreview preview(@Valid @RequestBody OrderRequest request) {
        var product = products.checkStock(request.productId(), request.quantity());
        return new OrderPreview("Order preview accepted; stock is unchanged.", product, request.quantity());
    }
}
