package com.ecommerce.repository;

import com.ecommerce.entity.Order;
import com.ecommerce.entity.ShopOrder;
import com.ecommerce.entity.ShopOrder.ShopOrderStatus;
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
                Join<Object, Object> shopOrders = order.join("shopOrders", JoinType.LEFT);
                Join<Object, Object> items = shopOrders.join("items", JoinType.LEFT);
                Join<Object, Object> product = items.join("product", JoinType.LEFT);
                Join<Object, Object> productVariant = items.join("productVariant", JoinType.LEFT);
                Join<Object, Object> variantProduct = productVariant.join("product", JoinType.LEFT);

                List<Predicate> predicates = getPredicates(cb, order, user, orderInfo, orderAddress, orderCustomerInfo,
                                orderTransaction, shopOrders, searchRequest);

                // Apply all predicates
                if (!predicates.isEmpty()) {
                        query.where(cb.and(predicates.toArray(new Predicate[0])));
                }

                // Add distinct to avoid duplicates from joins
                query.distinct(true);

                // Apply sorting
                if (pageable.getSort().isSorted()) {
                        List<jakarta.persistence.criteria.Order> ordersList = new ArrayList<>();
                        pageable.getSort().forEach(sortOrder -> {
                                if (sortOrder.isAscending()) {
                                        ordersList.add(cb.asc(order.get(sortOrder.getProperty())));
                                } else {
                                        ordersList.add(cb.desc(order.get(sortOrder.getProperty())));
                                }
                        });
                        query.orderBy(ordersList);
                }

                // Create the typed query
                TypedQuery<Order> typedQuery = entityManager.createQuery(query);

                // Get total count for pagination
                CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
                Root<Order> countRoot = countQuery.from(Order.class);

                // Apply the same joins and predicates for count query
                Join<Object, Object> countUser = countRoot.join("user", JoinType.LEFT);
                Join<Object, Object> countOrderInfo = countRoot.join("orderInfo", JoinType.LEFT); // Added for count
                                                                                                  // query
                Join<Object, Object> countOrderAddress = countRoot.join("orderAddress", JoinType.LEFT);
                Join<Object, Object> countOrderCustomerInfo = countRoot.join("orderCustomerInfo", JoinType.LEFT);
                Join<Object, Object> countOrderTransaction = countRoot.join("orderTransaction", JoinType.LEFT);
                // Add joins for shop filter in count query
                Join<Object, Object> countShopOrders = countRoot.join("shopOrders", JoinType.LEFT);
                Join<Object, Object> countItems = countShopOrders.join("items", JoinType.LEFT);
                Join<Object, Object> countProduct = countItems.join("product", JoinType.LEFT);
                Join<Object, Object> countProductVariant = countItems.join("productVariant", JoinType.LEFT);
                Join<Object, Object> countVariantProduct = countProductVariant.join("product", JoinType.LEFT);

                // Rebuild predicates for count query (same logic as above)
                List<Predicate> countPredicates = getPredicates(cb, countRoot, countUser, countOrderInfo,
                                countOrderAddress,
                                countOrderCustomerInfo, countOrderTransaction, countShopOrders, searchRequest);

                countQuery.select(cb.countDistinct(countRoot));
                if (!countPredicates.isEmpty()) {
                        countQuery.where(cb.and(countPredicates.toArray(new Predicate[0])));
                }

                Long totalCount = entityManager.createQuery(countQuery).getSingleResult();

                // Apply pagination
                typedQuery.setFirstResult((int) pageable.getOffset());
                typedQuery.setMaxResults(pageable.getPageSize());

                List<Order> orders = typedQuery.getResultList();

                return new PageImpl<>(orders, pageable, totalCount);
        }

        @Override
        public java.math.BigDecimal calculateTotalAmount(OrderSearchDTO searchRequest) {
                CriteriaBuilder cb = entityManager.getCriteriaBuilder();
                CriteriaQuery<java.math.BigDecimal> sumQuery = cb.createQuery(java.math.BigDecimal.class);
                Root<Order> root = sumQuery.from(Order.class);

                // Joins
                Join<Object, Object> user = root.join("user", JoinType.LEFT);
                Join<Object, Object> orderInfo = root.join("orderInfo", JoinType.LEFT);
                Join<Object, Object> orderAddress = root.join("orderAddress", JoinType.LEFT);
                Join<Object, Object> orderCustomerInfo = root.join("orderCustomerInfo", JoinType.LEFT);
                Join<Object, Object> orderTransaction = root.join("orderTransaction", JoinType.LEFT);
                Join<Object, Object> shopOrders = root.join("shopOrders", JoinType.LEFT);

                List<Predicate> predicates = getPredicates(cb, root, user, orderInfo, orderAddress, orderCustomerInfo,
                                orderTransaction, shopOrders, searchRequest);

                if (searchRequest.getShopId() != null) {
                        // For shop-specific view, we sum ShopOrder totals
                        sumQuery.select(cb.sum(shopOrders.get("totalAmount")));
                } else {
                        // For global view, we sum Order total (from OrderInfo)
                        sumQuery.select(cb.sum(orderInfo.get("totalAmount")));
                }

                if (!predicates.isEmpty()) {
                        sumQuery.where(cb.and(predicates.toArray(new Predicate[0])));
                }

                java.math.BigDecimal result = entityManager.createQuery(sumQuery).getSingleResult();
                return result != null ? result : java.math.BigDecimal.ZERO;
        }

        private List<Predicate> getPredicates(CriteriaBuilder cb, Root<Order> order, Join<Object, Object> user,
                        Join<Object, Object> orderInfo, Join<Object, Object> orderAddress,
                        Join<Object, Object> orderCustomerInfo, Join<Object, Object> orderTransaction,
                        Join<Object, Object> shopOrders, OrderSearchDTO searchRequest) {

                List<Predicate> predicates = new ArrayList<>();

                // Shop filter
                if (searchRequest.getShopId() != null) {
                        predicates.add(cb.equal(shopOrders.get("shop").get("shopId"), searchRequest.getShopId()));
                }

                // Order codes
                if (searchRequest.getOrderNumber() != null && !searchRequest.getOrderNumber().trim().isEmpty()) {
                        predicates.add(cb.like(cb.lower(order.get("orderCode")),
                                        "%" + searchRequest.getOrderNumber().toLowerCase() + "%"));
                }

                // User ID
                if (searchRequest.getUserId() != null && !searchRequest.getUserId().trim().isEmpty()) {
                        predicates.add(cb.like(cb.lower(user.get("id").as(String.class)),
                                        "%" + searchRequest.getUserId().toLowerCase() + "%"));
                }

                // Customer name
                if (searchRequest.getCustomerName() != null && !searchRequest.getCustomerName().trim().isEmpty()) {
                        String name = "%" + searchRequest.getCustomerName().toLowerCase() + "%";
                        predicates.add(cb.or(
                                        cb.like(cb.lower(orderCustomerInfo.get("firstName")), name),
                                        cb.like(cb.lower(orderCustomerInfo.get("lastName")), name),
                                        cb.like(cb.lower(user.get("firstName")), name),
                                        cb.like(cb.lower(user.get("lastName")), name)));
                }

                // Customer email
                if (searchRequest.getCustomerEmail() != null && !searchRequest.getCustomerEmail().trim().isEmpty()) {
                        String email = "%" + searchRequest.getCustomerEmail().toLowerCase() + "%";
                        predicates.add(cb.or(
                                        cb.like(cb.lower(orderCustomerInfo.get("email")), email),
                                        cb.like(cb.lower(user.get("userEmail")), email)));
                }

                // Order status
                if (searchRequest.getOrderStatus() != null && !searchRequest.getOrderStatus().trim().isEmpty()
                                && !searchRequest.getOrderStatus().equals("all")) {
                        try {
                                predicates.add(cb.equal(shopOrders.get("status"),
                                                ShopOrderStatus.valueOf(searchRequest.getOrderStatus().toUpperCase())));
                        } catch (IllegalArgumentException e) {
                                log.error("Invalid order status: {}", searchRequest.getOrderStatus());
                        }
                }

                // Payment status
                if (searchRequest.getPaymentStatus() != null && !searchRequest.getPaymentStatus().trim().isEmpty()
                                && !searchRequest.getPaymentStatus().equals("all")) {
                        predicates.add(cb.equal(orderTransaction.get("status"),
                                        searchRequest.getPaymentStatus().toUpperCase()));
                }

                // City
                if (searchRequest.getCity() != null && !searchRequest.getCity().trim().isEmpty()) {
                        String city = "%" + searchRequest.getCity().toLowerCase() + "%";
                        predicates.add(cb.or(
                                        cb.like(cb.lower(orderAddress.get("city")), city),
                                        cb.like(cb.lower(orderCustomerInfo.get("city")), city)));
                }

                // Dates
                if (searchRequest.getStartDate() != null) {
                        predicates.add(cb.greaterThanOrEqualTo(order.get("createdAt"), searchRequest.getStartDate()));
                }
                if (searchRequest.getEndDate() != null) {
                        predicates.add(cb.lessThanOrEqualTo(order.get("createdAt"), searchRequest.getEndDate()));
                }

                // Search keyword
                if (searchRequest.getSearchKeyword() != null && !searchRequest.getSearchKeyword().trim().isEmpty()) {
                        String keyword = "%" + searchRequest.getSearchKeyword().toLowerCase() + "%";
                        predicates.add(cb.or(
                                        cb.like(cb.lower(order.get("orderCode")), keyword),
                                        cb.like(cb.lower(user.get("id").as(String.class)), keyword),
                                        cb.like(cb.lower(orderCustomerInfo.get("firstName")), keyword),
                                        cb.like(cb.lower(orderCustomerInfo.get("lastName")), keyword),
                                        cb.like(cb.lower(orderCustomerInfo.get("email")), keyword),
                                        cb.like(cb.lower(user.get("userEmail")), keyword)));
                }

                return predicates;
        }
}
