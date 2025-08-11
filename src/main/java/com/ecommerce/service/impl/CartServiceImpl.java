package com.ecommerce.service.impl;

import com.ecommerce.dto.AddToCartDTO;
import com.ecommerce.dto.CartDTO;
import com.ecommerce.dto.CartItemDTO;
import com.ecommerce.dto.UpdateCartItemDTO;
import com.ecommerce.entity.Cart;
import com.ecommerce.entity.CartItem;
import com.ecommerce.entity.ProductVariant;
import com.ecommerce.entity.User;
import com.ecommerce.repository.CartItemRepository;
import com.ecommerce.repository.CartRepository;
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
    private final ProductVariantRepository productVariantRepository;

    @Override
    @Transactional
    public CartItemDTO addToCart(UUID userId, AddToCartDTO addToCartDTO) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userId));

        ProductVariant variant = productVariantRepository.findById(addToCartDTO.getVariantId())
                .orElseThrow(() -> new EntityNotFoundException("Product variant not found with ID: " + addToCartDTO.getVariantId()));

        if (!variant.isActive()) {
            throw new IllegalArgumentException("Product variant is not active");
        }

        if (variant.getStockQuantity() < addToCartDTO.getQuantity()) {
            throw new IllegalArgumentException("Insufficient stock. Available: " + variant.getStockQuantity());
        }

        Cart cart = getOrCreateUserCart(user);
        CartItem existingItem = cartItemRepository.findByCartIdAndProductVariantId(cart.getId(), variant.getId()).orElse(null);

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

            CartItem savedItem = cartItemRepository.save(newItem);
            return mapCartItemToDTO(savedItem);
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
                .orElseThrow(() -> new EntityNotFoundException("Cart item not found with ID: " + updateCartItemDTO.getCartItemId()));

        if (!cartItem.getCart().getId().equals(cart.getId())) {
            throw new IllegalArgumentException("Cart item does not belong to the user");
        }

        ProductVariant variant = cartItem.getProductVariant();
        if (updateCartItemDTO.getQuantity() > variant.getStockQuantity()) {
            throw new IllegalArgumentException("Quantity cannot exceed available stock of " + variant.getStockQuantity());
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
        // Implementation will be added
        return null;
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
        ProductVariant variant = cartItem.getProductVariant();
        String productImage = null;
        
        if (variant.getImages() != null && !variant.getImages().isEmpty()) {
            productImage = variant.getImages().stream()
                    .filter(img -> img.isPrimary())
                    .findFirst()
                    .map(img -> img.getImageUrl())
                    .orElse(variant.getImages().get(0).getImageUrl());
        }

        return CartItemDTO.builder()
                .id(cartItem.getId())
                .variantId(variant.getId())
                .variantSku(variant.getVariantSku())
                .productName(variant.getProduct().getProductName())
                .productImage(productImage)
                .quantity(cartItem.getQuantity())
                .price(variant.getPrice())
                .totalPrice(variant.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())))
                .addedAt(cartItem.getAddedAt())
                .inStock(variant.isInStock())
                .availableStock(variant.getStockQuantity())
                .build();
    }

    private BigDecimal calculateSubtotal(List<CartItem> cartItems) {
        return cartItems.stream()
                .map(item -> item.getProductVariant().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
