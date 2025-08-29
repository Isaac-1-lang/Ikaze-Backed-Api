package com.ecommerce.service.impl;

import com.ecommerce.dto.AddToCartDTO;
import com.ecommerce.dto.CartDTO;
import com.ecommerce.dto.CartItemDTO;
import com.ecommerce.dto.UpdateCartItemDTO;
import com.ecommerce.entity.Cart;
import com.ecommerce.entity.CartItem;
import com.ecommerce.entity.Product;
import com.ecommerce.entity.ProductVariant;
import com.ecommerce.entity.User;
import com.ecommerce.repository.CartItemRepository;
import com.ecommerce.repository.CartRepository;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.repository.ProductVariantRepository;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.service.CartService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;

    @Override
    @Transactional
    public CartItemDTO addToCart(UUID userId, AddToCartDTO addToCartDTO) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userId));

        Cart cart = getOrCreateUserCart(user);
        CartItem existingItem = null;

        if (addToCartDTO.isVariantBased()) {
            // Handle variant-based cart item
            ProductVariant variant = productVariantRepository.findById(addToCartDTO.getVariantId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Product variant not found with ID: " + addToCartDTO.getVariantId()));

            if (!variant.isActive()) {
                throw new IllegalArgumentException("Product variant is not active");
            }

            if (variant.getStockQuantity() < addToCartDTO.getQuantity()) {
                throw new IllegalArgumentException("Insufficient stock. Available: " + variant.getStockQuantity());
            }

            existingItem = cartItemRepository.findByCartIdAndProductVariantId(cart.getId(), variant.getId())
                    .orElse(null);

            if (existingItem != null) {
                int newQuantity = existingItem.getQuantity() + addToCartDTO.getQuantity();
                if (newQuantity > variant.getStockQuantity()) {
                    throw new IllegalArgumentException("Total quantity would exceed available stock");
                }
                existingItem.setQuantity(newQuantity);
                existingItem = cartItemRepository.save(existingItem);
                return mapCartItemToDTO(existingItem);
            } else {
                CartItem newItem = new CartItem();
                newItem.setCart(cart);
                newItem.setProductVariant(variant);
                newItem.setQuantity(addToCartDTO.getQuantity());
                newItem.setAddedAt(LocalDateTime.now());
                newItem.validateConfiguration();

                CartItem savedItem = cartItemRepository.save(newItem);
                return mapCartItemToDTO(savedItem);
            }

        } else if (addToCartDTO.isProductBased()) {
            // Handle product-based cart item (no variants)
            Product product = productRepository.findById(addToCartDTO.getProductId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Product not found with ID: " + addToCartDTO.getProductId()));

            if (!product.isActive()) {
                throw new IllegalArgumentException("Product is not active");
            }

            if (product.getStockQuantity() < addToCartDTO.getQuantity()) {
                throw new IllegalArgumentException("Insufficient stock. Available: " + product.getStockQuantity());
            }

            existingItem = cartItemRepository.findByCartIdAndProductProductId(cart.getId(), product.getProductId())
                    .orElse(null);

            if (existingItem != null) {
                int newQuantity = existingItem.getQuantity() + addToCartDTO.getQuantity();
                if (newQuantity > product.getStockQuantity()) {
                    throw new IllegalArgumentException("Total quantity would exceed available stock");
                }
                existingItem.setQuantity(newQuantity);
                existingItem = cartItemRepository.save(existingItem);
                return mapCartItemToDTO(existingItem);
            } else {
                CartItem newItem = new CartItem();
                newItem.setCart(cart);
                newItem.setProduct(product);
                newItem.setQuantity(addToCartDTO.getQuantity());
                newItem.setAddedAt(LocalDateTime.now());
                newItem.validateConfiguration();

                CartItem savedItem = cartItemRepository.save(newItem);
                return mapCartItemToDTO(savedItem);
            }
        } else {
            throw new IllegalArgumentException("Invalid cart item: must specify either productId or variantId");
        }
    }

    @Override
    @Transactional
    public CartItemDTO updateCartItem(UUID userId, UpdateCartItemDTO updateCartItemDTO) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userId));

        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("Cart not found for user: " + userId));

        CartItem cartItem = cartItemRepository.findById(updateCartItemDTO.getCartItemId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Cart item not found with ID: " + updateCartItemDTO.getCartItemId()));

        if (!cartItem.getCart().getId().equals(cart.getId())) {
            throw new IllegalArgumentException("Cart item does not belong to the user");
        }

        // Check stock based on whether it's a variant or product
        if (cartItem.isVariantBased()) {
            ProductVariant variant = cartItem.getProductVariant();
            if (updateCartItemDTO.getQuantity() > variant.getStockQuantity()) {
                throw new IllegalArgumentException(
                        "Quantity cannot exceed available stock of " + variant.getStockQuantity());
            }
        } else if (cartItem.isProductBased()) {
            Product product = cartItem.getProduct();
            if (updateCartItemDTO.getQuantity() > product.getStockQuantity()) {
                throw new IllegalArgumentException(
                        "Quantity cannot exceed available stock of " + product.getStockQuantity());
            }
        }

        cartItem.setQuantity(updateCartItemDTO.getQuantity());
        CartItem updatedItem = cartItemRepository.save(cartItem);
        return mapCartItemToDTO(updatedItem);
    }

    @Override
    @Transactional
    public boolean removeFromCart(UUID userId, Long cartItemId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userId));

        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("Cart not found for user: " + userId));

        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new EntityNotFoundException("Cart item not found with ID: " + cartItemId));

        if (!cartItem.getCart().getId().equals(cart.getId())) {
            throw new IllegalArgumentException("Cart item does not belong to the user");
        }

        cartItemRepository.delete(cartItem);
        return true;
    }

    @Override
    @Transactional
    public CartDTO viewCart(UUID userId, Pageable pageable) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userId));

        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("Cart not found for user: " + userId));

        Page<CartItem> cartItemsPage = cartItemRepository.findByCartId(cart.getId(), pageable);
        List<CartItemDTO> cartItemDTOs = cartItemsPage.getContent().stream()
                .map(this::mapCartItemToDTO)
                .collect(Collectors.toList());

        BigDecimal subtotal = calculateSubtotal(cartItemsPage.getContent());
        int totalItems = (int) cartItemsPage.getTotalElements();

        return CartDTO.builder()
                .cartId(cart.getId())
                .userId(userId)
                .items(cartItemDTOs)
                .totalItems(totalItems)
                .subtotal(subtotal)
                .total(subtotal)
                .createdAt(cart.getCreatedAt())
                .updatedAt(cart.getUpdatedAt())
                .isEmpty(totalItems == 0)
                .build();
    }

    @Override
    @Transactional
    public boolean clearCart(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userId));

        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("Cart not found for user: " + userId));

        cartItemRepository.deleteByCartId(cart.getId());
        return true;
    }

    @Override
    @Transactional
    public CartItemDTO getCartItem(UUID userId, Long cartItemId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userId));

        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("Cart not found for user: " + userId));

        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new EntityNotFoundException("Cart item not found with ID: " + cartItemId));

        if (!cartItem.getCart().getId().equals(cart.getId())) {
            throw new IllegalArgumentException("Cart item does not belong to the user");
        }

        return mapCartItemToDTO(cartItem);
    }

    @Override
    @Transactional
    public boolean hasItems(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userId));

        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("Cart not found for user: " + userId));

        long itemCount = cartItemRepository.countByCartId(cart.getId());
        return itemCount > 0;
    }

    private Cart getOrCreateUserCart(User user) {
        return cartRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    Cart newCart = new Cart();
                    newCart.setUser(user);
                    return cartRepository.save(newCart);
                });
    }

    private CartItemDTO mapCartItemToDTO(CartItem cartItem) {
        String productImage = null;
        BigDecimal price;
        String productName;
        String sku;
        boolean inStock;
        Integer availableStock;

        if (cartItem.isVariantBased()) {
            // Handle variant-based cart item
            ProductVariant variant = cartItem.getProductVariant();
            price = variant.getPrice();
            productName = variant.getProduct().getProductName() + " - " + variant.getVariantName();
            sku = variant.getVariantSku();
            inStock = variant.isInStock();
            availableStock = variant.getStockQuantity();

            if (variant.getImages() != null && !variant.getImages().isEmpty()) {
                productImage = variant.getImages().stream()
                        .filter(img -> img.isPrimary())
                        .findFirst()
                        .map(img -> img.getImageUrl())
                        .orElse(variant.getImages().get(0).getImageUrl());
            }
        } else {
            // Handle product-based cart item
            Product product = cartItem.getProduct();
            price = product.getDiscountedPrice();
            productName = product.getProductName();
            sku = product.getSku();
            inStock = product.isInStock();
            availableStock = product.getStockQuantity();

            if (product.getImages() != null && !product.getImages().isEmpty()) {
                productImage = product.getImages().stream()
                        .filter(img -> img.isPrimary())
                        .findFirst()
                        .map(img -> img.getImageUrl())
                        .orElse(product.getImages().get(0).getImageUrl());
            }
        }

        return CartItemDTO.builder()
                .id(cartItem.getId())
                .productId(
                        cartItem.getEffectiveProduct() != null ? cartItem.getEffectiveProduct().getProductId() : null)
                .variantId(cartItem.getProductVariant() != null ? cartItem.getProductVariant().getId() : null)
                .sku(sku)
                .productName(productName)
                .productImage(productImage)
                .quantity(cartItem.getQuantity())
                .price(price)
                .totalPrice(price.multiply(BigDecimal.valueOf(cartItem.getQuantity())))
                .addedAt(cartItem.getAddedAt())
                .inStock(inStock)
                .availableStock(availableStock)
                .isVariantBased(cartItem.isVariantBased())
                .build();
    }

    private BigDecimal calculateSubtotal(List<CartItem> cartItems) {
        return cartItems.stream()
                .map(item -> item.getEffectivePrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
