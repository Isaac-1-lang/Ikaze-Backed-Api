package com.ecommerce.repository;

import com.ecommerce.Enum.UserRole;
import com.ecommerce.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
        Optional<User> findByUserEmail(String userEmail);

        /**
         * Count users by role
         */
        long countByRole(String role);

        /**
         * Find users by role with pagination
         */
        @Query("SELECT u FROM User u WHERE u.role = :role")
        Page<User> findByRole(@Param("role") UserRole role, Pageable pageable);

        /**
         * Find all users by role
         */
        @Query("SELECT u FROM User u WHERE u.role = :role")
        List<User> findByRole(@Param("role") UserRole role);

        /**
         * Count users by role
         */
        int countByRole(UserRole role);

        /**
         * Find users by role and search term (name or email) with pagination
         */
        @Query("SELECT u FROM User u WHERE u.role = :role AND " +
                        "(LOWER(u.firstName) LIKE %:searchTerm% OR " +
                        "LOWER(u.lastName) LIKE %:searchTerm% OR " +
                        "LOWER(u.userEmail) LIKE %:searchTerm%)")
        Page<User> findByRoleAndSearchTerm(@Param("role") UserRole role,
                        @Param("searchTerm") String searchTerm,
                        Pageable pageable);

        /**
         * Find users by role and shop with pagination
         */
        @Query("SELECT u FROM User u WHERE u.role = :role AND u.shop.shopId = :shopId")
        Page<User> findByRoleAndShop(@Param("role") UserRole role,
                        @Param("shopId") UUID shopId,
                        Pageable pageable);

        /**
         * Find users by role, shop and search term with pagination
         */
        @Query("SELECT u FROM User u WHERE u.role = :role AND u.shop.shopId = :shopId AND " +
                        "(LOWER(u.firstName) LIKE %:searchTerm% OR " +
                        "LOWER(u.lastName) LIKE %:searchTerm% OR " +
                        "LOWER(u.userEmail) LIKE %:searchTerm%)")
        Page<User> findByRoleAndShopAndSearchTerm(@Param("role") UserRole role,
                        @Param("shopId") UUID shopId,
                        @Param("searchTerm") String searchTerm,
                        Pageable pageable);

        boolean existsByUserEmail(String email);

        /**
         * Find user by reset token
         */
        Optional<User> findByResetToken(String resetToken);
}
