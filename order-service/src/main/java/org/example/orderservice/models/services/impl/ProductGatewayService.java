package org.example.orderservice.models.services.impl;

import feign.FeignException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import org.example.orderservice.clients.ProductClient;
import org.example.orderservice.exceptions.ProductNotFoundException;
import org.example.orderservice.exceptions.ProductServiceException;
import org.example.orderservice.models.dto.responses.ProductResponse;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ProductGatewayService {

    private final ProductClient productClient;

    @CircuitBreaker(
            name = "productService",
            fallbackMethod = "getProductByIdFallback"
    )
    public ProductResponse getProductById(Long productId) {
        try {
            ProductResponse product = productClient.getProductById(productId);
            if (product == null
                    || !Objects.equals(productId, product.id())
                    || product.name() == null
                    || product.name().isBlank()
                    || product.price() == null
                    || product.price() < 0) {
                throw new ProductServiceException("Product service returned invalid product data");
            }
            return product;
        } catch (FeignException.NotFound exception) {
            throw new ProductNotFoundException(productId);
        }
    }

    private ProductResponse getProductByIdFallback(Long productId, Throwable exception) {
        if (exception instanceof ProductNotFoundException productNotFoundException) {
            throw productNotFoundException;
        }
        throw new ProductServiceException("Product service is unavailable", exception);
    }
}
