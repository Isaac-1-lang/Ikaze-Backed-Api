package com.ecommerce.service.impl;

import com.ecommerce.Exception.CustomException;
import com.ecommerce.dto.ShopDTO;
import com.ecommerce.entity.Shop;
import com.ecommerce.entity.User;
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

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ShopServiceImpl implements ShopService {

    private final ShopRepository shopRepository;
    private final UserRepository userRepository;

    @Autowired
    public ShopServiceImpl(ShopRepository shopRepository, UserRepository userRepository) {
        this.shopRepository = shopRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public ShopDTO createShop(ShopDTO shopDTO, UUID ownerId) {
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + ownerId));

        if (shopRepository.existsBySlug(shopDTO.getSlug())) {
            throw new CustomException("Shop with slug '" + shopDTO.getSlug() + "' already exists");
        }

        if (shopRepository.existsByOwnerIdAndName(ownerId, shopDTO.getName())) {
            throw new CustomException("You already have a shop with this name");
        }

        Shop shop = convertToEntity(shopDTO);
        shop.setOwner(owner);
        shop.setStatus(Shop.ShopStatus.PENDING);

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
}

