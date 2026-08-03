package com.letsplay.service;

import com.letsplay.dto.ProductRequest;
import com.letsplay.dto.ProductResponse;
import com.letsplay.exception.ResourceNotFoundException;
import com.letsplay.model.Product;
import com.letsplay.model.User;
import com.letsplay.repository.ProductRepository;
import com.letsplay.event.UserDeletedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<ProductResponse> getAllProducts() {
        return productRepository.findAll().stream()
                .map(ProductResponse::fromProduct)
                .collect(Collectors.toList());
    }

    public ProductResponse getProductById(String id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        return ProductResponse.fromProduct(product);
    }

    public ProductResponse createProduct(ProductRequest request, User currentUser) {
        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .userId(currentUser.getId()) // Owner is the currently authenticated user
                .build();

        return ProductResponse.fromProduct(productRepository.save(product));
    }

    public ProductResponse updateProduct(String id, ProductRequest request, User currentUser) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        // Authorization: Admin can update anything, otherwise must be owner
        if (!currentUser.getRole().equalsIgnoreCase("ADMIN") && !product.getUserId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You are not authorized to update this product");
        }

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());

        return ProductResponse.fromProduct(productRepository.save(product));
    }

    public void deleteProduct(String id, User currentUser) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        // Authorization: Admin can delete anything, otherwise must be owner
        if (!currentUser.getRole().equalsIgnoreCase("ADMIN") && !product.getUserId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You are not authorized to delete this product");
        }

        productRepository.deleteById(id);
    }

    @EventListener
    public void handleUserDeleted(UserDeletedEvent event) {
        productRepository.findByUserId(event.getUserId())
                .forEach(product -> productRepository.deleteById(product.getId()));
    }
}
