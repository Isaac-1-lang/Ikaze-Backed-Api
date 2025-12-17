package com.ecommerce.service.impl;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import com.ecommerce.Enum.UserRole;
import com.ecommerce.Exception.CustomException;
import com.ecommerce.entity.Shop;
import com.ecommerce.entity.User;
import com.ecommerce.repository.ShopRepository;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.service.ShopAuthorizationService;

@Service
@Slf4j
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
        log.info("Checking shop management access - userId: {}, shopId: {}", userId, shopId);
        
        if (userId == null || shopId == null) {
            log.warn("userId or shopId is null - userId: {}, shopId: {}", userId, shopId);
            return false;
        }

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            log.warn("User not found with userId: {}", userId);
            return false;
        }

        log.info("User found - userId: {}, role: {}, email: {}", userId, user.getRole(), user.getUserEmail());

        if (user.getRole() != UserRole.VENDOR && user.getRole() != UserRole.EMPLOYEE) {
            log.warn("User role {} is not VENDOR or EMPLOYEE", user.getRole());
            return false;
        }

        Shop shop = shopRepository.findById(shopId).orElse(null);
        if (shop == null) {
            log.warn("Shop not found with shopId: {}", shopId);
            return false;
        }

        log.info("Shop found - shopId: {}, name: {}, ownerId: {}", shopId, shop.getName(), 
                shop.getOwner() != null ? shop.getOwner().getId() : "null");

        // VENDOR must own the shop
        if (user.getRole() == UserRole.VENDOR) {
            boolean canManage = shop.getOwner() != null && shop.getOwner().getId().equals(userId);
            log.info("VENDOR access check - canManage: {}, ownerId: {}, userId: {}", 
                    canManage, shop.getOwner() != null ? shop.getOwner().getId() : "null", userId);
            return canManage;
        }
        
        // EMPLOYEE can manage any shop (since they access shops via shopSlug in the URL)
        // In the future, this could be restricted to shops where the employee is explicitly associated
        if (user.getRole() == UserRole.EMPLOYEE) {
            log.info("EMPLOYEE access granted for shopId: {}", shopId);
            return true;
        }

        log.warn("Unexpected role: {}", user.getRole());
        return false;
    }

    @Override
    public void assertCanManageShop(UUID userId, UUID shopId) {
        log.info("Asserting shop management access - userId: {}, shopId: {}", userId, shopId);
        if (!canManageShop(userId, shopId)) {
            log.error("Access denied - userId: {}, shopId: {}", userId, shopId);
            throw new CustomException("Access Denied");
        }
        log.info("Access granted - userId: {}, shopId: {}", userId, shopId);
    }
}


