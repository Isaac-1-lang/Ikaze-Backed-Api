package com.ecommerce.service;

import com.ecommerce.dto.ShopDTO;
import com.ecommerce.entity.Shop;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface ShopService {

    ShopDTO createShop(ShopDTO shopDTO, UUID ownerId);

    ShopDTO updateShop(UUID shopId, ShopDTO shopDTO, UUID ownerId);

    void deleteShop(UUID shopId, UUID ownerId);

    ShopDTO getShopById(UUID shopId);

    ShopDTO getShopBySlug(String slug);

    List<ShopDTO> getShopsByOwner(UUID ownerId);

    Page<ShopDTO> getAllShops(Pageable pageable);

    List<ShopDTO> getActiveShops();

    ShopDTO convertToDTO(Shop shop);

    Shop convertToEntity(ShopDTO shopDTO);

    boolean isOwner(UUID shopId, UUID userId);
}

