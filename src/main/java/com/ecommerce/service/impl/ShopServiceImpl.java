package com.ecommerce.service.impl;

import com.ecommerce.Exception.CustomException;
import com.ecommerce.dto.ShopDTO;
import com.ecommerce.entity.AdminInvitation;
import com.ecommerce.entity.Shop;
import com.ecommerce.entity.User;
import com.ecommerce.repository.AdminInvitationRepository;
import com.ecommerce.repository.ShopRepository;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.service.ShopService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import com.ecommerce.dto.ShopDetailsDTO;
import com.ecommerce.entity.Product;
import org.springframework.data.domain.PageRequest;

@Service
@Slf4j
public class ShopServiceImpl implements ShopService {

    private final ShopRepository shopRepository;
    private final UserRepository userRepository;
    private final com.ecommerce.repository.ProductRepository productRepository;
    private final AdminInvitationRepository adminInvitationRepository;

    @Autowired
    public ShopServiceImpl(ShopRepository shopRepository, UserRepository userRepository,
            com.ecommerce.repository.ProductRepository productRepository,
            AdminInvitationRepository adminInvitationRepository) {
        this.shopRepository = shopRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.adminInvitationRepository = adminInvitationRepository;
    }

    @Override
    @Transactional
    public ShopDTO createShop(ShopDTO shopDTO, UUID ownerId) {
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + ownerId));

        com.ecommerce.Enum.UserRole userRole = owner.getRole();

        if (userRole == com.ecommerce.Enum.UserRole.ADMIN) {
            throw new CustomException("Administrators cannot create shops. Only vendors can own shops.");
        }

        if (userRole == com.ecommerce.Enum.UserRole.DELIVERY_AGENT ||
                userRole == com.ecommerce.Enum.UserRole.EMPLOYEE) {
            throw new CustomException(
                    "You are currently associated with a shop as " + userRole.name() + ". " +
                            "Please contact the shop administrator to remove your association first. " +
                            "Once your role is changed back to CUSTOMER, you can create your own shop.");
        }

        List<Shop> existingShops = shopRepository.findByOwnerId(ownerId);
        if (!existingShops.isEmpty()) {
            throw new CustomException("You already have a shop. Each user can only own one shop.");
        }

        if (shopDTO.getSlug() != null && shopRepository.existsBySlug(shopDTO.getSlug())) {
            throw new CustomException("Shop with slug '" + shopDTO.getSlug() + "' already exists");
        }

        if (shopRepository.existsByOwnerIdAndName(ownerId, shopDTO.getName())) {
            throw new CustomException("You already have a shop with this name");
        }

        Shop shop = convertToEntity(shopDTO);
        shop.setOwner(owner);
        shop.setStatus(Shop.ShopStatus.ACTIVE);

        if (userRole == com.ecommerce.Enum.UserRole.CUSTOMER) {
            owner.setRole(com.ecommerce.Enum.UserRole.VENDOR);
            userRepository.save(owner);
        }

        Shop savedShop = shopRepository.save(shop);
        return convertToDTO(savedShop);
    }

    @Override
    @Transactional
    public ShopDTO updateShop(UUID shopId, ShopDTO shopDTO, UUID ownerId) {
        Shop existingShop = shopRepository.findById(shopId)
                .orElseThrow(() -> new EntityNotFoundException("Shop not found with id: " + shopId));

        if (!existingShop.getOwner().getId().equals(ownerId)) {
            throw new CustomException("You are not authorized to update this shop");
        }

        if (shopDTO.getSlug() != null && !shopDTO.getSlug().equals(existingShop.getSlug())) {
            if (shopRepository.existsBySlug(shopDTO.getSlug())) {
                throw new CustomException("Shop with slug '" + shopDTO.getSlug() + "' already exists");
            }
            existingShop.setSlug(shopDTO.getSlug());
        }

        if (shopDTO.getName() != null) {
            existingShop.setName(shopDTO.getName());
        }
        if (shopDTO.getDescription() != null) {
            existingShop.setDescription(shopDTO.getDescription());
        }
        if (shopDTO.getLogoUrl() != null) {
            existingShop.setLogoUrl(shopDTO.getLogoUrl());
        }
        if (shopDTO.getContactEmail() != null) {
            existingShop.setContactEmail(shopDTO.getContactEmail());
        }
        if (shopDTO.getContactPhone() != null) {
            existingShop.setContactPhone(shopDTO.getContactPhone());
        }
        if (shopDTO.getAddress() != null) {
            existingShop.setAddress(shopDTO.getAddress());
        }
        if (shopDTO.getIsActive() != null) {
            existingShop.setIsActive(shopDTO.getIsActive());
        }
        if (shopDTO.getStatus() != null) {
            existingShop.setStatus(shopDTO.getStatus());
        }

        Shop updatedShop = shopRepository.save(existingShop);
        return convertToDTO(updatedShop);
    }

    @Override
    @Transactional
    public void deleteShop(UUID shopId, UUID ownerId) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new EntityNotFoundException("Shop not found with id: " + shopId));

        if (!shop.getOwner().getId().equals(ownerId)) {
            throw new CustomException("You are not authorized to delete this shop");
        }

        shopRepository.delete(shop);
    }

    @Override
    @Transactional(readOnly = true)
    public ShopDTO getShopById(UUID shopId) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new EntityNotFoundException("Shop not found with id: " + shopId));
        return convertToDTO(shop);
    }

    @Override
    @Transactional(readOnly = true)
    public ShopDTO getShopBySlug(String slug) {
        Shop shop = shopRepository.findBySlug(slug)
                .orElseThrow(() -> new EntityNotFoundException("Shop not found with slug: " + slug));
        return convertToDTO(shop);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShopDTO> getShopsByOwner(UUID ownerId) {
        List<Shop> shops = shopRepository.findByOwnerId(ownerId);
        return shops.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ShopDTO> getAllShops(Pageable pageable) {
        Page<Shop> shops = shopRepository.findAll(pageable);
        List<ShopDTO> shopDTOs = shops.getContent().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        return new PageImpl<>(shopDTOs, pageable, shops.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ShopDTO> searchShops(String search, String category, Pageable pageable) {
        log.info("========== SEARCH SHOPS DEBUG ==========");
        log.info("Search term: {}", search);
        log.info("Category: {}", category);
        log.info("Pageable: {}", pageable);
        log.info("Sort: {}", pageable.getSort());

        try {
            log.info("Calling shopRepository.searchShops...");
            Page<Shop> shops = shopRepository.searchShops(search, category, pageable);
            log.info("Successfully retrieved {} shops", shops.getTotalElements());

            List<ShopDTO> shopDTOs = shops.getContent().stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());

            log.info("Successfully converted to DTOs");
            log.info("========== END SEARCH SHOPS DEBUG ==========");
            return new PageImpl<>(shopDTOs, pageable, shops.getTotalElements());
        } catch (Exception e) {
            log.error("========== ERROR IN SEARCH SHOPS ==========");
            log.error("Error message: {}", e.getMessage());
            log.error("Error class: {}", e.getClass().getName());
            log.error("Stack trace:", e);
            log.error("========== END ERROR ==========");
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShopDTO> getActiveShops() {
        List<Shop> shops = shopRepository.findActiveShops();
        return shops.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public ShopDTO convertToDTO(Shop shop) {
        ShopDTO dto = new ShopDTO();
        dto.setShopId(shop.getShopId());
        dto.setName(shop.getName());
        dto.setDescription(shop.getDescription());
        dto.setSlug(shop.getSlug());
        dto.setLogoUrl(shop.getLogoUrl());
        dto.setStatus(shop.getStatus());
        dto.setContactEmail(shop.getContactEmail());
        dto.setContactPhone(shop.getContactPhone());
        dto.setAddress(shop.getAddress());
        dto.setIsActive(shop.getIsActive());
        dto.setCategory(shop.getCategory());
        dto.setRating(shop.getRating());
        dto.setTotalReviews(shop.getTotalReviews());
        dto.setProductCount(shop.getProductCount() != null ? shop.getProductCount().longValue() : 0L);
        dto.setCreatedAt(shop.getCreatedAt());
        dto.setUpdatedAt(shop.getUpdatedAt());

        if (shop.getOwner() != null) {
            dto.setOwnerId(shop.getOwner().getId());
            dto.setOwnerName(shop.getOwner().getFirstName() + " " + shop.getOwner().getLastName());
            dto.setOwnerEmail(shop.getOwner().getUserEmail());
        }

        return dto;
    }

    @Override
    public Shop convertToEntity(ShopDTO shopDTO) {
        Shop shop = new Shop();
        shop.setName(shopDTO.getName());
        shop.setDescription(shopDTO.getDescription());
        shop.setSlug(shopDTO.getSlug());
        shop.setLogoUrl(shopDTO.getLogoUrl());
        shop.setContactEmail(shopDTO.getContactEmail());
        shop.setContactPhone(shopDTO.getContactPhone());
        shop.setAddress(shopDTO.getAddress());
        shop.setIsActive(shopDTO.getIsActive());
        shop.setCategory(shopDTO.getCategory());
        if (shopDTO.getStatus() != null) {
            shop.setStatus(shopDTO.getStatus());
        }
        return shop;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isOwner(UUID shopId, UUID userId) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new EntityNotFoundException("Shop not found with id: " + shopId));
        return shop.getOwner().getId().equals(userId);
    }

    @Override
    @Transactional
    public List<ShopDTO> getUserShops(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));

        List<ShopDTO> shops = new java.util.ArrayList<>();

        if (user.getRole() == com.ecommerce.Enum.UserRole.VENDOR) {
            shops = getShopsByOwner(userId);
        } else if (user.getRole() == com.ecommerce.Enum.UserRole.EMPLOYEE ||
                user.getRole() == com.ecommerce.Enum.UserRole.DELIVERY_AGENT) {
            // Get shop from user's shop relationship (set when accepting invitation)
            if (user.getShop() != null) {
                Shop shop = user.getShop();
                shops = List.of(convertToDTO(shop));
                log.info("Found shop {} for {} user {} via direct relationship", shop.getShopId(), user.getRole(),
                        userId);
            } else {
                // Fallback: try to find shop from accepted invitation
                log.warn("User {} has no shop relationship, checking accepted invitations", userId);
                List<AdminInvitation> acceptedInvitations = adminInvitationRepository
                        .findAcceptedInvitationsByUser(userId);

                if (!acceptedInvitations.isEmpty()) {
                    // Get the most recent accepted invitation and use its shop
                    AdminInvitation latestInvitation = acceptedInvitations.get(0);
                    if (latestInvitation.getShop() != null) {
                        Shop shop = latestInvitation.getShop();
                        shops = List.of(convertToDTO(shop));
                        log.info("Found shop {} for {} user {} via accepted invitation, updating user relationship",
                                shop.getShopId(), user.getRole(), userId);

                        // Update user's shop relationship for future queries
                        user.setShop(shop);
                        userRepository.save(user);
                        log.info("Updated user {} shop relationship to shop {}", userId, shop.getShopId());
                    } else {
                        log.warn("Accepted invitation {} has no shop associated", latestInvitation.getInvitationId());
                        shops = new java.util.ArrayList<>();
                    }
                } else {
                    log.warn("No accepted invitations found for user {}", userId);
                    shops = new java.util.ArrayList<>();
                }
            }
        }

        return shops;
    }

    @Override
    @Transactional(readOnly = true)
    public ShopDetailsDTO getShopDetails(UUID shopId) {
        log.info("Fetching details for shop ID: {}", shopId);
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new EntityNotFoundException("Shop not found with ID: " + shopId));

        User owner = shop.getOwner();

        // Fetch top products for the shop
        Page<Product> productsPage = productRepository.findByShopIdForCustomers(shopId, PageRequest.of(0, 8));

        List<ShopDetailsDTO.FeaturedProduct> featuredProducts = productsPage.getContent().stream()
                .map(product -> ShopDetailsDTO.FeaturedProduct.builder()
                        .productId(product.getProductId())
                        .name(product.getProductName())
                        .slug(product.getSlug())
                        .price(product.getVariants().isEmpty() ? 0.0
                                : product.getVariants().get(0).getPrice().doubleValue())
                        .compareAtPrice(product.getVariants().isEmpty() ? 0.0
                                : (product.getVariants().get(0).getCompareAtPrice() != null
                                        ? product.getVariants().get(0).getCompareAtPrice().doubleValue()
                                        : null))
                        .primaryImage(product.getMainImageUrl())
                        .rating(product.getAverageRating())
                        .reviewCount(product.getReviewCount())
                        .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                        .isInStock(product.isInStock())
                        .build())
                .collect(Collectors.toList());

        ShopDetailsDTO.OwnerInfo ownerInfo = ShopDetailsDTO.OwnerInfo.builder()
                .id(owner.getId())
                .firstName(owner.getFirstName())
                .lastName(owner.getLastName())
                .email(owner.getUserEmail())
                .avatar(null) // User entity doesn't seem to have an avatar field in the viewed code
                .build();

        return ShopDetailsDTO.builder()
                .shopId(shop.getShopId())
                .name(shop.getName())
                .slug(shop.getSlug())
                .description(shop.getDescription())
                .logoUrl(shop.getLogoUrl())
                .category(shop.getCategory())
                .address(shop.getAddress())
                .contactEmail(shop.getContactEmail())
                .contactPhone(shop.getContactPhone())
                .isActive(shop.getIsActive())
                .status(shop.getStatus().name())
                .rating(shop.getRating())
                .totalReviews(shop.getTotalReviews())
                .productCount(shop.getProductCount())
                .createdAt(shop.getCreatedAt())
                .updatedAt(shop.getUpdatedAt())
                .owner(ownerInfo)
                .featuredProducts(featuredProducts)
                .build();
    }
}
