package com.ecommerce.service.impl;

import com.ecommerce.dto.AddToCartDTO;
import com.ecommerce.dto.CartDTO;
import com.ecommerce.dto.CartItemDTO;
import com.ecommerce.dto.UpdateCartItemDTO;
import com.ecommerce.dto.CartProductsRequestDTO;
import com.ecommerce.dto.CartProductsResponseDTO;
import com.ecommerce.entity.Cart;
import com.ecommerce.entity.CartItem;
import com.ecommerce.entity.Product;
import com.ecommerce.entity.ProductVariant;
import com.ecommerce.entity.ProductImage;
import com.ecommerce.entity.ProductVariantImage;
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
import java.util.ArrayList;
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

            if (variant.getTotalStockQuantity() < addToCartDTO.getQuantity()) {
                throw new IllegalArgumentException("Insufficient stock. Available: " + variant.getTotalStockQuantity());
            }

            existingItem = cartItemRepository.findByCartIdAndProductVariantId(cart.getId(), variant.getId())
                    .orElse(null);

            if (existingItem != null) {
                int newQuantity = existingItem.getQuantity() + addToCartDTO.getQuantity();
                if (newQuantity > variant.getTotalStockQuantity()) {
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

            if (product.getTotalStockQuantity() < addToCartDTO.getQuantity()) {
                throw new IllegalArgumentException("Insufficient stock. Available: " + product.getTotalStockQuantity());
            }

            existingItem = cartItemRepository.findByCartIdAndProductProductId(cart.getId(), product.getProductId())
                    .orElse(null);

            if (existingItem != null) {
                int newQuantity = existingItem.getQuantity() + addToCartDTO.getQuantity();
                if (newQuantity > product.getTotalStockQuantity()) {
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
            if (updateCartItemDTO.getQuantity() > variant.getTotalStockQuantity()) {
                throw new IllegalArgumentException(
                        "Quantity cannot exceed available stock of " + variant.getTotalStockQuantity());
            }
        } else if (cartItem.isProductBased()) {
            Product product = cartItem.getProduct();
            if (updateCartItemDTO.getQuantity() > product.getTotalStockQuantity()) {
                throw new IllegalArgumentException(
                        "Quantity cannot exceed available stock of " + product.getTotalStockQuantity());
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
        String productName;
        String sku;
        boolean inStock;
        Integer availableStock;

        // Get basic information
        if (cartItem.isVariantBased()) {
            // Handle variant-based cart item
            ProductVariant variant = cartItem.getProductVariant();
            productName = variant.getProduct().getProductName() + " - " + variant.getVariantName();
            sku = variant.getVariantSku();
            inStock = variant.isInStock();
            availableStock = variant.getTotalStockQuantity();

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
            productName = product.getProductName();
            sku = product.getSku();
            inStock = product.isInStock();
            availableStock = product.getTotalStockQuantity();

            if (product.getImages() != null && !product.getImages().isEmpty()) {
                productImage = product.getImages().stream()
                        .filter(img -> img.isPrimary())
                        .findFirst()
                        .map(img -> img.getImageUrl())
                        .orElse(product.getImages().get(0).getImageUrl());
            }
        }

        // Get pricing information with discount
        BigDecimal originalPrice = cartItem.getOriginalPrice();
        BigDecimal effectivePrice = cartItem.getEffectivePrice();
        BigDecimal discountPercentage = cartItem.getDiscountPercentage();
        String discountName = cartItem.getDiscountName();
        boolean hasDiscount = discountPercentage.compareTo(BigDecimal.ZERO) > 0;
        BigDecimal discountAmount = originalPrice.subtract(effectivePrice);
        BigDecimal totalPrice = effectivePrice.multiply(BigDecimal.valueOf(cartItem.getQuantity()));

        return CartItemDTO.builder()
                .id(cartItem.getId())
                .productId(
                        cartItem.getEffectiveProduct() != null ? cartItem.getEffectiveProduct().getProductId() : null)
                .variantId(cartItem.getProductVariant() != null ? cartItem.getProductVariant().getId() : null)
                .sku(sku)
                .productName(productName)
                .productImage(productImage)
                .quantity(cartItem.getQuantity())
                .price(effectivePrice) // Current price with discount
                .originalPrice(originalPrice) // Original price before discount
                .totalPrice(totalPrice)
                .addedAt(cartItem.getAddedAt())
                .inStock(inStock)
                .availableStock(availableStock)
                .isVariantBased(cartItem.isVariantBased())
                .discountPercentage(discountPercentage)
                .discountName(discountName)
                .discountAmount(discountAmount)
                .hasDiscount(hasDiscount)
                .build();
    }

    private BigDecimal calculateSubtotal(List<CartItem> cartItems) {
        return cartItems.stream()
                .map(item -> item.getEffectivePrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public CartProductsResponseDTO getCartProductsInfo(CartProductsRequestDTO request) {
        try {
            List<CartProductsResponseDTO.CartProductDTO> cartProducts = new ArrayList<>();
            BigDecimal subtotal = BigDecimal.ZERO;
            int totalItems = 0;

            for (CartProductsRequestDTO.CartItemRequestDTO item : request.getItems()) {
                try {
                    CartProductsResponseDTO.CartProductDTO cartProduct;

                    if (item.getVariantId() != null) {
                        // Handle variant - get product from variant
                        ProductVariant variant = productVariantRepository.findById(item.getVariantId())
                                .orElseThrow(() -> new EntityNotFoundException(
                                        "Variant not found with ID: " + item.getVariantId()));

                        Product product = variant.getProduct();

                        // Get variant images, fallback to product images
                        String imageUrl = getVariantOrProductImage(variant, product);

                        // Build variant attributes
                        List<CartProductsResponseDTO.CartProductDTO.VariantAttributeDTO> attributes = variant
                                .getAttributeValues() != null ? variant.getAttributeValues().stream()
                                        .map(attr -> CartProductsResponseDTO.CartProductDTO.VariantAttributeDTO
                                                .builder()
                                                .attributeTypeName(
                                                        attr.getAttributeValue().getAttributeType().getName())
                                                .attributeValue(attr.getAttributeValue().getValue())
                                                .build())
                                        .collect(Collectors.toList()) : new ArrayList<>();

                        // Calculate discount information for variant
                        BigDecimal variantOriginalPrice = variant.getPrice();
                        BigDecimal variantEffectivePrice = getVariantEffectivePrice(variant);
                        BigDecimal variantDiscountPercentage = getVariantDiscountPercentage(variant);
                        String variantDiscountName = getVariantDiscountName(variant);

                        cartProduct = CartProductsResponseDTO.CartProductDTO.builder()
                                .itemId(item.getItemId())
                                .productId(product.getProductId().toString())
                                .variantId(variant.getId())
                                .productName(product.getProductName())
                                .productDescription(product.getDescription())
                                .price(variantEffectivePrice) // Use effective price with discount
                                .previousPrice(variantDiscountPercentage.compareTo(BigDecimal.ZERO) > 0
                                        ? variantOriginalPrice
                                        : null)
                                .productImage(imageUrl)
                                .quantity(item.getQuantity())
                                .availableStock(variant.getTotalStockQuantity())
                                .totalPrice(variantEffectivePrice.multiply(BigDecimal.valueOf(item.getQuantity())))
                                .averageRating(product.getAverageRating())
                                .reviewCount(product.getReviewCount())
                                .variantSku(variant.getVariantSku())
                                .variantAttributes(attributes)
                                .build();
                    } else {
                        UUID productId;
                        try {
                            productId = UUID.fromString(item.getProductId());
                        } catch (IllegalArgumentException e) {
                            log.warn("Invalid product ID format: {}", item.getProductId());
                            continue;
                        }

                        Product product = productRepository.findById(productId)
                                .orElseThrow(() -> new EntityNotFoundException(
                                        "Product not found with ID: " + item.getProductId()));

                        String imageUrl = getProductMainImage(product);

                        // Calculate discount information for product
                        BigDecimal productOriginalPrice = product.getPrice();
                        BigDecimal productEffectivePrice = getProductEffectivePrice(product);
                        BigDecimal productDiscountPercentage = getProductDiscountPercentage(product);
                        String productDiscountName = getProductDiscountName(product);

                        cartProduct = CartProductsResponseDTO.CartProductDTO.builder()
                                .itemId(item.getItemId())
                                .productId(product.getProductId().toString())
                                .variantId(null)
                                .productName(product.getProductName())
                                .productDescription(product.getDescription())
                                .price(productEffectivePrice) // Use effective price with discount
                                .previousPrice(productDiscountPercentage.compareTo(BigDecimal.ZERO) > 0
                                        ? productOriginalPrice
                                        : null)
                                .productImage(imageUrl)
                                .quantity(item.getQuantity())
                                .availableStock(product.getTotalStockQuantity())
                                .totalPrice(productEffectivePrice.multiply(BigDecimal.valueOf(item.getQuantity())))
                                .averageRating(product.getAverageRating())
                                .reviewCount(product.getReviewCount())
                                .variantSku(null)
                                .variantAttributes(new ArrayList<>())
                                .build();
                    }

                    cartProducts.add(cartProduct);
                    subtotal = subtotal.add(cartProduct.getTotalPrice());
                    totalItems += item.getQuantity();

                } catch (EntityNotFoundException e) {
                    log.warn("Product or variant not found for cart item: {}", e.getMessage());
                    // Skip this item and continue with others
                    continue;
                }
            }

            return CartProductsResponseDTO.builder()
                    .items(cartProducts)
                    .subtotal(subtotal)
                    .totalItems(totalItems)
                    .build();

        } catch (Exception e) {
            log.error("Error getting cart products info: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get cart products information", e);
        }
    }

    private String getVariantOrProductImage(ProductVariant variant, Product product) {
        // Try to get variant primary image first
        if (variant.getImages() != null && !variant.getImages().isEmpty()) {
            return variant.getImages().stream()
                    .filter(ProductVariantImage::isPrimary)
                    .findFirst()
                    .map(ProductVariantImage::getImageUrl)
                    .orElse(variant.getImages().get(0).getImageUrl());
        }

        // Fallback to product image
        return getProductMainImage(product);
    }

    private String getProductMainImage(Product product) {
        if (product.getImages() != null && !product.getImages().isEmpty()) {
            return product.getImages().stream()
                    .filter(ProductImage::isPrimary)
                    .findFirst()
                    .map(ProductImage::getImageUrl)
                    .orElse(product.getImages().get(0).getImageUrl());
        }
        return ""; // Return empty string if no image found
    }

    /**
     * Calculates the effective price for a variant considering discounts
     */
    private BigDecimal getVariantEffectivePrice(ProductVariant variant) {
        // Check if variant has its own discount
        if (variant.getDiscount() != null && isDiscountActive(variant.getDiscount())) {
            return calculateDiscountedPrice(variant.getPrice(), variant.getDiscount().getPercentage());
        }
        // If product has discount, apply it to variant
        if (variant.getProduct().getDiscount() != null && isDiscountActive(variant.getProduct().getDiscount())) {
            return calculateDiscountedPrice(variant.getPrice(), variant.getProduct().getDiscount().getPercentage());
        }
        return variant.getPrice();
    }

    /**
     * Calculates the effective price for a product considering discounts
     */
    private BigDecimal getProductEffectivePrice(Product product) {
        // Check if product has discount
        if (product.getDiscount() != null && isDiscountActive(product.getDiscount())) {
            return calculateDiscountedPrice(product.getPrice(), product.getDiscount().getPercentage());
        }
        return product.getPrice();
    }

    /**
     * Gets the discount percentage for a variant
     */
    private BigDecimal getVariantDiscountPercentage(ProductVariant variant) {
        // Check if variant has its own discount
        if (variant.getDiscount() != null && isDiscountActive(variant.getDiscount())) {
            return variant.getDiscount().getPercentage();
        }
        // If product has discount, return product discount percentage
        if (variant.getProduct().getDiscount() != null && isDiscountActive(variant.getProduct().getDiscount())) {
            return variant.getProduct().getDiscount().getPercentage();
        }
        return BigDecimal.ZERO;
    }

    /**
     * Gets the discount percentage for a product
     */
    private BigDecimal getProductDiscountPercentage(Product product) {
        if (product.getDiscount() != null && isDiscountActive(product.getDiscount())) {
            return product.getDiscount().getPercentage();
        }
        return BigDecimal.ZERO;
    }

    /**
     * Gets the discount name for a variant
     */
    private String getVariantDiscountName(ProductVariant variant) {
        // Check if variant has its own discount
        if (variant.getDiscount() != null && isDiscountActive(variant.getDiscount())) {
            return variant.getDiscount().getName();
        }
        // If product has discount, return product discount name
        if (variant.getProduct().getDiscount() != null && isDiscountActive(variant.getProduct().getDiscount())) {
            return variant.getProduct().getDiscount().getName();
        }
        return null;
    }

    /**
     * Gets the discount name for a product
     */
    private String getProductDiscountName(Product product) {
        if (product.getDiscount() != null && isDiscountActive(product.getDiscount())) {
            return product.getDiscount().getName();
        }
        return null;
    }

    /**
     * Checks if a discount is currently active based on its date range
     */
    private boolean isDiscountActive(com.ecommerce.entity.Discount discount) {
        if (discount == null || !discount.isActive()) {
            return false;
        }

        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        return (discount.getStartDate() == null || now.isAfter(discount.getStartDate())
                || now.isEqual(discount.getStartDate())) &&
                (discount.getEndDate() == null || now.isBefore(discount.getEndDate())
                        || now.isEqual(discount.getEndDate()));
    }

    /**
     * Calculates the discounted price based on original price and discount
     * percentage
     */
    private BigDecimal calculateDiscountedPrice(BigDecimal originalPrice, BigDecimal discountPercentage) {
        if (originalPrice == null || discountPercentage == null) {
            return originalPrice;
        }
        return originalPrice.multiply(BigDecimal.ONE.subtract(discountPercentage.divide(BigDecimal.valueOf(100))));
    }
}
