package com.ecommerce.service.impl;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ecommerce.Enum.UserRole;
import com.ecommerce.Exception.CustomException;
import com.ecommerce.entity.Shop;
import com.ecommerce.entity.User;
import com.ecommerce.repository.ShopRepository;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.service.ShopAuthorizationService;

@Service
public class ShopAuthorizationServiceImpl implements ShopAuthorizationService {

    private final ShopRepository shopRepository;
    private final UserRepository userRepository;

    @Autowired
    public ShopAuthorizationServiceImpl(ShopRepository shopRepository, UserRepository userRepository) {
        this.shopRepository = shopRepository;
        this.userRepository = userRepository;
    }

    @Override
    public boolean canManageShop(UUID userId, UUID shopId) {
        if (userId == null || shopId == null) {
            return false;
        }

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return false;
        }

        if (user.getRole() != UserRole.VENDOR && user.getRole() != UserRole.EMPLOYEE) {
            return false;
        }

        Shop shop = shopRepository.findById(shopId).orElse(null);
        if (shop == null) {
            return false;
        }

        return shop.getOwner() != null && shop.getOwner().getId().equals(userId);
    }

    @Override
    public void assertCanManageShop(UUID userId, UUID shopId) {
        if (!canManageShop(userId, shopId)) {
            throw new CustomException("You are not authorized to manage this shop");
        }
    }
}


