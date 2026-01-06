package com.ecommerce.service;

import java.util.UUID;

public interface ShopAuthorizationService {

    boolean canManageShop(UUID userId, UUID shopId);

    void assertCanManageShop(UUID userId, UUID shopId);
}


