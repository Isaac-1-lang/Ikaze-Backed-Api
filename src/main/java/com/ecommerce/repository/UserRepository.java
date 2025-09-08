package com.ecommerce.repository;
import com.ecommerce.Enum.UserRole;
import com.ecommerce.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByUserEmail(String userEmail);

  
   long countByRole(UserRole role);


    boolean existsByUserEmail(String userEmail);
}
