package com.ecommerce.repository;

import com.ecommerce.entity.ShopOrder;
import com.ecommerce.dto.OrderSearchDTO;
import com.ecommerce.entity.Order;
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
public class ShopOrderRepositoryImpl implements ShopOrderRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Page<ShopOrder> searchShopOrders(OrderSearchDTO searchRequest, Pageable pageable) {
        log.info("Searching shop orders with criteria: {}", searchRequest);

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<ShopOrder> query = cb.createQuery(ShopOrder.class);
        Root<ShopOrder> shopOrder = query.from(ShopOrder.class);

        // Joins
        Join<ShopOrder, Order> order = shopOrder.join("order", JoinType.LEFT);
        Join<Object, Object> user = order.join("user", JoinType.LEFT);
        Join<Object, Object> orderCustomerInfo = order.join("orderCustomerInfo", JoinType.LEFT);
        Join<Object, Object> orderAddress = order.join("orderAddress", JoinType.LEFT);
        Join<Object, Object> transaction = order.join("orderTransaction", JoinType.LEFT);

        // We can fetch details eagerly here if we want to avoid N+1, similar to
        // findAllWithDetailsByShopId
        // But for search, usually we might rely on default fetching or explicit
        // fetches.
        // Let's rely on default but ensure we join properly for filtering.

        List<Predicate> predicates = new ArrayList<>();

        // Shop Filter (Primary filter)
        if (searchRequest.getShopId() != null) {
            predicates.add(cb.equal(shopOrder.get("shop").get("shopId"), searchRequest.getShopId()));
        }

        // Search Keyword (Order Number, Customer Name, etc.)
        if (searchRequest.getSearchKeyword() != null && !searchRequest.getSearchKeyword().trim().isEmpty()) {
            String keyword = "%" + searchRequest.getSearchKeyword().toLowerCase() + "%";

            Predicate shopOrderCodeMatch = cb.like(cb.lower(shopOrder.get("shopOrderCode")), keyword);
            Predicate orderCodeMatch = cb.like(cb.lower(order.get("orderCode")), keyword);

            // Customer matches
            Predicate guestFirstName = cb.like(cb.lower(orderCustomerInfo.get("firstName")), keyword);
            Predicate guestLastName = cb.like(cb.lower(orderCustomerInfo.get("lastName")), keyword);
            Predicate customerEmail = cb.like(cb.lower(orderCustomerInfo.get("email")), keyword);
            Predicate customerPhone = cb.like(cb.lower(orderCustomerInfo.get("phoneNumber")), keyword);

            Predicate userFirstName = cb.like(cb.lower(user.get("firstName")), keyword);
            Predicate userLastName = cb.like(cb.lower(user.get("lastName")), keyword);
            Predicate userEmail = cb.like(cb.lower(user.get("userEmail")), keyword);
            Predicate userPhone = cb.like(cb.lower(user.get("phone")), keyword);

            predicates.add(cb.or(
                    shopOrderCodeMatch, orderCodeMatch,
                    guestFirstName, guestLastName, customerEmail, customerPhone,
                    userFirstName, userLastName, userEmail, userPhone));
        }

        // Specific Field Filters

        // Order Number (Shop Order Code)
        if (searchRequest.getOrderNumber() != null && !searchRequest.getOrderNumber().trim().isEmpty()) {
            // In shop context, "Order Number" usually usually means "Shop Order Code"
            predicates.add(cb.like(cb.lower(shopOrder.get("shopOrderCode")),
                    "%" + searchRequest.getOrderNumber().toLowerCase() + "%"));
        }

        // Status
        if (searchRequest.getOrderStatus() != null && !searchRequest.getOrderStatus().trim().isEmpty()
                && !searchRequest.getOrderStatus().equals("all")) {
            try {
                com.ecommerce.entity.ShopOrder.ShopOrderStatus status = com.ecommerce.entity.ShopOrder.ShopOrderStatus
                        .valueOf(searchRequest.getOrderStatus().toUpperCase());
                predicates.add(cb.equal(shopOrder.get("status"), status));
            } catch (IllegalArgumentException e) {
                log.warn("Invalid shop order status: {}", searchRequest.getOrderStatus());
            }
        }

        // Payment Status (on parent order transaction)
        if (searchRequest.getPaymentStatus() != null && !searchRequest.getPaymentStatus().trim().isEmpty()
                && !searchRequest.getPaymentStatus().equals("all")) {
            predicates.add(cb.equal(transaction.get("status"), searchRequest.getPaymentStatus().toUpperCase()));
        }

        // Date Range
        if (searchRequest.getStartDate() != null) {
            predicates.add(cb.greaterThanOrEqualTo(shopOrder.get("createdAt"), searchRequest.getStartDate()));
        }
        if (searchRequest.getEndDate() != null) {
            predicates.add(cb.lessThanOrEqualTo(shopOrder.get("createdAt"), searchRequest.getEndDate()));
        }

        // Location (City)
        if (searchRequest.getCity() != null && !searchRequest.getCity().trim().isEmpty()) {
            Predicate addrCity = cb.like(cb.lower(orderAddress.get("city")),
                    "%" + searchRequest.getCity().toLowerCase() + "%");
            predicates.add(addrCity);
        }

        // Apply predicates
        if (!predicates.isEmpty()) {
            query.where(cb.and(predicates.toArray(new Predicate[0])));
        }

        // Sorting
        // Default sort by createdAt desc if not specified
        if (searchRequest.getSortBy() != null) {
            Path<Object> sortPath;
            if ("totalAmount".equals(searchRequest.getSortBy())) {
                sortPath = shopOrder.get("totalAmount");
            } else {
                sortPath = shopOrder.get("createdAt");
            }

            if ("asc".equalsIgnoreCase(searchRequest.getSortDirection())) {
                query.orderBy(cb.asc(sortPath));
            } else {
                query.orderBy(cb.desc(sortPath));
            }
        } else {
            query.orderBy(cb.desc(shopOrder.get("createdAt")));
        }

        // Pagination
        TypedQuery<ShopOrder> typedQuery = entityManager.createQuery(query);
        typedQuery.setFirstResult((int) pageable.getOffset());
        typedQuery.setMaxResults(pageable.getPageSize());

        // Exact Count Query
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<ShopOrder> countRoot = countQuery.from(ShopOrder.class);
        // ... (Repeat joins and predicates for count query - simplified for brevity,
        // assume similar structure necessary for correct filtering)
        // Since we are filtering by ShopOrder properties mostly but also joined
        // properties (customer name), we need joins.

        Join<ShopOrder, Order> countOrder = countRoot.join("order", JoinType.LEFT);
        Join<Object, Object> countUser = countOrder.join("user", JoinType.LEFT);
        Join<Object, Object> countOrderCustomerInfo = countOrder.join("orderCustomerInfo", JoinType.LEFT);
        Join<Object, Object> countOrderAddress = countOrder.join("orderAddress", JoinType.LEFT);
        Join<Object, Object> countTransaction = countOrder.join("orderTransaction", JoinType.LEFT);

        List<Predicate> countPredicates = new ArrayList<>();
        // Re-add predicates for count
        if (searchRequest.getShopId() != null) {
            countPredicates.add(cb.equal(countRoot.get("shop").get("shopId"), searchRequest.getShopId()));
        }
        if (searchRequest.getSearchKeyword() != null && !searchRequest.getSearchKeyword().trim().isEmpty()) {
            String keyword = "%" + searchRequest.getSearchKeyword().toLowerCase() + "%";
            Predicate p1 = cb.like(cb.lower(countRoot.get("shopOrderCode")), keyword);
            Predicate p2 = cb.like(cb.lower(countOrder.get("orderCode")), keyword);
            Predicate p3 = cb.like(cb.lower(countOrderCustomerInfo.get("firstName")), keyword);
            Predicate p4 = cb.like(cb.lower(countOrderCustomerInfo.get("lastName")), keyword);
            Predicate p5 = cb.like(cb.lower(countOrderCustomerInfo.get("email")), keyword);
            Predicate p6 = cb.like(cb.lower(countUser.get("firstName")), keyword);
            Predicate p7 = cb.like(cb.lower(countUser.get("lastName")), keyword);
            Predicate p8 = cb.like(cb.lower(countUser.get("userEmail")), keyword);
            countPredicates.add(cb.or(p1, p2, p3, p4, p5, p6, p7, p8));
        }
        if (searchRequest.getOrderNumber() != null && !searchRequest.getOrderNumber().trim().isEmpty()) {
            countPredicates.add(cb.like(cb.lower(countRoot.get("shopOrderCode")),
                    "%" + searchRequest.getOrderNumber().toLowerCase() + "%"));
        }
        if (searchRequest.getOrderStatus() != null && !searchRequest.getOrderStatus().trim().isEmpty()
                && !searchRequest.getOrderStatus().equals("all")) {
            try {
                countPredicates.add(cb.equal(countRoot.get("status"), com.ecommerce.entity.ShopOrder.ShopOrderStatus
                        .valueOf(searchRequest.getOrderStatus().toUpperCase())));
            } catch (IllegalArgumentException e) {
            }
        }
        if (searchRequest.getPaymentStatus() != null && !searchRequest.getPaymentStatus().trim().isEmpty()
                && !searchRequest.getPaymentStatus().equals("all")) {
            countPredicates
                    .add(cb.equal(countTransaction.get("status"), searchRequest.getPaymentStatus().toUpperCase()));
        }
        if (searchRequest.getStartDate() != null) {
            countPredicates.add(cb.greaterThanOrEqualTo(countRoot.get("createdAt"), searchRequest.getStartDate()));
        }
        if (searchRequest.getEndDate() != null) {
            countPredicates.add(cb.lessThanOrEqualTo(countRoot.get("createdAt"), searchRequest.getEndDate()));
        }
        if (searchRequest.getCity() != null && !searchRequest.getCity().trim().isEmpty()) {
            countPredicates.add(cb.like(cb.lower(countOrderAddress.get("city")),
                    "%" + searchRequest.getCity().toLowerCase() + "%"));
        }

        countQuery.select(cb.count(countRoot));
        if (!countPredicates.isEmpty()) {
            countQuery.where(cb.and(countPredicates.toArray(new Predicate[0])));
        }

        Long totalCount = entityManager.createQuery(countQuery).getSingleResult();

        return new PageImpl<>(typedQuery.getResultList(), pageable, totalCount);
    }
}
