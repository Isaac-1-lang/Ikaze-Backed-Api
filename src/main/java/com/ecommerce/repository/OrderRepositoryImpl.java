package com.ecommerce.repository;

import com.ecommerce.entity.Order;
import com.ecommerce.dto.OrderSearchDTO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
@Slf4j
public class OrderRepositoryImpl implements OrderRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Page<Order> searchOrders(OrderSearchDTO searchRequest, Pageable pageable) {
        log.info("Searching orders with criteria: {}", searchRequest);

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Order> query = cb.createQuery(Order.class);
        Root<Order> order = query.from(Order.class);

        // Create joins for related entities
        Join<Object, Object> user = order.join("user", JoinType.LEFT);
        Join<Object, Object> orderInfo = order.join("orderInfo", JoinType.LEFT);
        Join<Object, Object> orderAddress = order.join("orderAddress", JoinType.LEFT);
        Join<Object, Object> orderCustomerInfo = order.join("orderCustomerInfo", JoinType.LEFT);
        Join<Object, Object> orderTransaction = order.join("orderTransaction", JoinType.LEFT);

        List<Predicate> predicates = new ArrayList<>();

        // Order number filter
        if (searchRequest.getOrderNumber() != null && !searchRequest.getOrderNumber().trim().isEmpty()) {
            predicates.add(cb.like(cb.lower(order.get("orderCode")), 
                "%" + searchRequest.getOrderNumber().toLowerCase() + "%"));
        }

        // User ID filter
        if (searchRequest.getUserId() != null && !searchRequest.getUserId().trim().isEmpty()) {
            predicates.add(cb.like(cb.lower(user.get("id").as(String.class)), 
                "%" + searchRequest.getUserId().toLowerCase() + "%"));
        }

        // Customer name filter (both guest and registered users)
        if (searchRequest.getCustomerName() != null && !searchRequest.getCustomerName().trim().isEmpty()) {
            Predicate guestName = cb.like(cb.lower(orderCustomerInfo.get("fullName")), 
                "%" + searchRequest.getCustomerName().toLowerCase() + "%");
            Predicate registeredFirstName = cb.like(cb.lower(user.get("firstName")), 
                "%" + searchRequest.getCustomerName().toLowerCase() + "%");
            Predicate registeredLastName = cb.like(cb.lower(user.get("lastName")), 
                "%" + searchRequest.getCustomerName().toLowerCase() + "%");
            
            predicates.add(cb.or(guestName, registeredFirstName, registeredLastName));
        }

        // Customer email filter
        if (searchRequest.getCustomerEmail() != null && !searchRequest.getCustomerEmail().trim().isEmpty()) {
            Predicate guestEmail = cb.like(cb.lower(orderCustomerInfo.get("email")), 
                "%" + searchRequest.getCustomerEmail().toLowerCase() + "%");
            Predicate registeredEmail = cb.like(cb.lower(user.get("userEmail")), 
                "%" + searchRequest.getCustomerEmail().toLowerCase() + "%");
            
            predicates.add(cb.or(guestEmail, registeredEmail));
        }

        // Customer phone filter
        if (searchRequest.getCustomerPhone() != null && !searchRequest.getCustomerPhone().trim().isEmpty()) {
            Predicate guestPhone = cb.like(orderCustomerInfo.get("phone"), 
                "%" + searchRequest.getCustomerPhone() + "%");
            Predicate registeredPhone = cb.like(user.get("phone"), 
                "%" + searchRequest.getCustomerPhone() + "%");
            
            predicates.add(cb.or(guestPhone, registeredPhone));
        }

        // Order status filter
        if (searchRequest.getOrderStatus() != null && !searchRequest.getOrderStatus().trim().isEmpty() 
            && !searchRequest.getOrderStatus().equals("all")) {
            predicates.add(cb.equal(order.get("orderStatus"), 
                Order.OrderStatus.valueOf(searchRequest.getOrderStatus().toUpperCase())));
        }

        // Payment status filter
        if (searchRequest.getPaymentStatus() != null && !searchRequest.getPaymentStatus().trim().isEmpty() 
            && !searchRequest.getPaymentStatus().equals("all")) {
            predicates.add(cb.equal(orderTransaction.get("status"), searchRequest.getPaymentStatus().toUpperCase()));
        }

        // City filter
        if (searchRequest.getCity() != null && !searchRequest.getCity().trim().isEmpty()) {
            Predicate addressCity = cb.like(cb.lower(orderAddress.get("city")), 
                "%" + searchRequest.getCity().toLowerCase() + "%");
            Predicate customerCity = cb.like(cb.lower(orderCustomerInfo.get("city")), 
                "%" + searchRequest.getCity().toLowerCase() + "%");
            
            predicates.add(cb.or(addressCity, customerCity));
        }

        // State filter
        if (searchRequest.getState() != null && !searchRequest.getState().trim().isEmpty()) {
            Predicate addressState = cb.like(cb.lower(orderAddress.get("state")), 
                "%" + searchRequest.getState().toLowerCase() + "%");
            Predicate customerState = cb.like(cb.lower(orderCustomerInfo.get("state")), 
                "%" + searchRequest.getState().toLowerCase() + "%");
            
            predicates.add(cb.or(addressState, customerState));
        }

        // Country filter
        if (searchRequest.getCountry() != null && !searchRequest.getCountry().trim().isEmpty()) {
            Predicate addressCountry = cb.like(cb.lower(orderAddress.get("country")), 
                "%" + searchRequest.getCountry().toLowerCase() + "%");
            Predicate customerCountry = cb.like(cb.lower(orderCustomerInfo.get("country")), 
                "%" + searchRequest.getCountry().toLowerCase() + "%");
            
            predicates.add(cb.or(addressCountry, customerCountry));
        }

        // Total amount filters
        if (searchRequest.getTotalMin() != null) {
            predicates.add(cb.greaterThanOrEqualTo(order.get("total"), searchRequest.getTotalMin()));
        }
        if (searchRequest.getTotalMax() != null) {
            predicates.add(cb.lessThanOrEqualTo(order.get("total"), searchRequest.getTotalMax()));
        }

        // Date filters
        if (searchRequest.getStartDate() != null) {
            predicates.add(cb.greaterThanOrEqualTo(order.get("createdAt"), searchRequest.getStartDate()));
        }
        if (searchRequest.getEndDate() != null) {
            predicates.add(cb.lessThanOrEqualTo(order.get("createdAt"), searchRequest.getEndDate()));
        }

        // Payment method filter
        if (searchRequest.getPaymentMethod() != null && !searchRequest.getPaymentMethod().trim().isEmpty()) {
            predicates.add(cb.like(cb.lower(orderTransaction.get("paymentMethod")), 
                "%" + searchRequest.getPaymentMethod().toLowerCase() + "%"));
        }

        // Tracking number filter
        if (searchRequest.getTrackingNumber() != null && !searchRequest.getTrackingNumber().trim().isEmpty()) {
            predicates.add(cb.like(cb.lower(orderInfo.get("trackingNumber")), 
                "%" + searchRequest.getTrackingNumber().toLowerCase() + "%"));
        }

        // General search keyword (searches across multiple fields)
        if (searchRequest.getSearchKeyword() != null && !searchRequest.getSearchKeyword().trim().isEmpty()) {
            String keyword = "%" + searchRequest.getSearchKeyword().toLowerCase() + "%";
            
            Predicate orderNumberMatch = cb.like(cb.lower(order.get("orderCode")), keyword);
            Predicate userIdMatch = cb.like(cb.lower(user.get("id").as(String.class)), keyword);
            Predicate guestNameMatch = cb.like(cb.lower(orderCustomerInfo.get("fullName")), keyword);
            Predicate guestEmailMatch = cb.like(cb.lower(orderCustomerInfo.get("email")), keyword);
            Predicate registeredEmailMatch = cb.like(cb.lower(user.get("userEmail")), keyword);
            
            predicates.add(cb.or(orderNumberMatch, userIdMatch, guestNameMatch, guestEmailMatch, registeredEmailMatch));
        }

        // Apply all predicates
        if (!predicates.isEmpty()) {
            query.where(cb.and(predicates.toArray(new Predicate[0])));
        }

        // Add distinct to avoid duplicates from joins
        query.distinct(true);

        // Create the typed query
        TypedQuery<Order> typedQuery = entityManager.createQuery(query);

        // Get total count for pagination
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Order> countRoot = countQuery.from(Order.class);
        
        // Apply the same joins and predicates for count query
        Join<Object, Object> countUser = countRoot.join("user", JoinType.LEFT);
        Join<Object, Object> countOrderAddress = countRoot.join("orderAddress", JoinType.LEFT);
        Join<Object, Object> countOrderCustomerInfo = countRoot.join("orderCustomerInfo", JoinType.LEFT);
        Join<Object, Object> countOrderTransaction = countRoot.join("orderTransaction", JoinType.LEFT);

        // Rebuild predicates for count query (same logic as above)
        List<Predicate> countPredicates = new ArrayList<>();
        
        if (searchRequest.getOrderNumber() != null && !searchRequest.getOrderNumber().trim().isEmpty()) {
            countPredicates.add(cb.like(cb.lower(countRoot.get("orderCode")), 
                "%" + searchRequest.getOrderNumber().toLowerCase() + "%"));
        }
        if (searchRequest.getUserId() != null && !searchRequest.getUserId().trim().isEmpty()) {
            countPredicates.add(cb.like(cb.lower(countUser.get("id").as(String.class)), 
                "%" + searchRequest.getUserId().toLowerCase() + "%"));
        }
        if (searchRequest.getCustomerName() != null && !searchRequest.getCustomerName().trim().isEmpty()) {
            Predicate guestName = cb.like(cb.lower(countOrderCustomerInfo.get("fullName")), 
                "%" + searchRequest.getCustomerName().toLowerCase() + "%");
            Predicate registeredFirstName = cb.like(cb.lower(countUser.get("firstName")), 
                "%" + searchRequest.getCustomerName().toLowerCase() + "%");
            Predicate registeredLastName = cb.like(cb.lower(countUser.get("lastName")), 
                "%" + searchRequest.getCustomerName().toLowerCase() + "%");
            countPredicates.add(cb.or(guestName, registeredFirstName, registeredLastName));
        }
        if (searchRequest.getCustomerEmail() != null && !searchRequest.getCustomerEmail().trim().isEmpty()) {
            Predicate guestEmail = cb.like(cb.lower(countOrderCustomerInfo.get("email")), 
                "%" + searchRequest.getCustomerEmail().toLowerCase() + "%");
            Predicate registeredEmail = cb.like(cb.lower(countUser.get("userEmail")), 
                "%" + searchRequest.getCustomerEmail().toLowerCase() + "%");
            countPredicates.add(cb.or(guestEmail, registeredEmail));
        }
        if (searchRequest.getCustomerPhone() != null && !searchRequest.getCustomerPhone().trim().isEmpty()) {
            Predicate guestPhone = cb.like(countOrderCustomerInfo.get("phone"), 
                "%" + searchRequest.getCustomerPhone() + "%");
            Predicate registeredPhone = cb.like(countUser.get("phone"), 
                "%" + searchRequest.getCustomerPhone() + "%");
            countPredicates.add(cb.or(guestPhone, registeredPhone));
        }
        if (searchRequest.getOrderStatus() != null && !searchRequest.getOrderStatus().trim().isEmpty() 
            && !searchRequest.getOrderStatus().equals("all")) {
            countPredicates.add(cb.equal(countRoot.get("orderStatus"), 
                Order.OrderStatus.valueOf(searchRequest.getOrderStatus().toUpperCase())));
        }
        if (searchRequest.getPaymentStatus() != null && !searchRequest.getPaymentStatus().trim().isEmpty() 
            && !searchRequest.getPaymentStatus().equals("all")) {
            countPredicates.add(cb.equal(countOrderTransaction.get("status"), searchRequest.getPaymentStatus().toUpperCase()));
        }
        if (searchRequest.getCity() != null && !searchRequest.getCity().trim().isEmpty()) {
            Predicate addressCity = cb.like(cb.lower(countOrderAddress.get("city")), 
                "%" + searchRequest.getCity().toLowerCase() + "%");
            Predicate customerCity = cb.like(cb.lower(countOrderCustomerInfo.get("city")), 
                "%" + searchRequest.getCity().toLowerCase() + "%");
            countPredicates.add(cb.or(addressCity, customerCity));
        }
        if (searchRequest.getStartDate() != null) {
            countPredicates.add(cb.greaterThanOrEqualTo(countRoot.get("createdAt"), searchRequest.getStartDate()));
        }
        if (searchRequest.getEndDate() != null) {
            countPredicates.add(cb.lessThanOrEqualTo(countRoot.get("createdAt"), searchRequest.getEndDate()));
        }
        if (searchRequest.getSearchKeyword() != null && !searchRequest.getSearchKeyword().trim().isEmpty()) {
            String keyword = "%" + searchRequest.getSearchKeyword().toLowerCase() + "%";
            Predicate orderNumberMatch = cb.like(cb.lower(countRoot.get("orderCode")), keyword);
            Predicate userIdMatch = cb.like(cb.lower(countUser.get("id").as(String.class)), keyword);
            Predicate guestNameMatch = cb.like(cb.lower(countOrderCustomerInfo.get("fullName")), keyword);
            Predicate guestEmailMatch = cb.like(cb.lower(countOrderCustomerInfo.get("email")), keyword);
            Predicate registeredEmailMatch = cb.like(cb.lower(countUser.get("userEmail")), keyword);
            countPredicates.add(cb.or(orderNumberMatch, userIdMatch, guestNameMatch, guestEmailMatch, registeredEmailMatch));
        }

        countQuery.select(cb.countDistinct(countRoot));
        if (!countPredicates.isEmpty()) {
            countQuery.where(cb.and(countPredicates.toArray(new Predicate[0])));
        }

        Long totalCount = entityManager.createQuery(countQuery).getSingleResult();

        // Apply pagination
        typedQuery.setFirstResult((int) pageable.getOffset());
        typedQuery.setMaxResults(pageable.getPageSize());

        List<Order> orders = typedQuery.getResultList();

        log.info("Found {} orders matching search criteria", orders.size());

        return new PageImpl<>(orders, pageable, totalCount);
    }
}
