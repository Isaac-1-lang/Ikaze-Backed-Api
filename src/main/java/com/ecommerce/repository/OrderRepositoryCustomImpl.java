package com.ecommerce.repository;

import com.ecommerce.dto.OrderSearchDTO;
import com.ecommerce.entity.Order;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Repository
public class OrderRepositoryCustomImpl implements OrderRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Page<Order> searchOrders(OrderSearchDTO searchRequest, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Order> query = cb.createQuery(Order.class);
        Root<Order> root = query.from(Order.class);

        List<Predicate> predicates = buildPredicates(cb, root, searchRequest);

        if (!predicates.isEmpty()) {
            query.where(cb.and(predicates.toArray(new Predicate[0])));
        }

        applySorting(cb, root, query, pageable);

        TypedQuery<Order> typedQuery = entityManager.createQuery(query);
        typedQuery.setFirstResult((int) pageable.getOffset());
        typedQuery.setMaxResults(pageable.getPageSize());

        List<Order> results = typedQuery.getResultList();

        long total = countSearchResults(searchRequest);

        return new PageImpl<>(results, pageable, total);
    }

    private List<Predicate> buildPredicates(CriteriaBuilder cb, Root<Order> root, OrderSearchDTO searchRequest) {
        List<Predicate> predicates = new ArrayList<>();

        if (searchRequest.getOrderNumber() != null && !searchRequest.getOrderNumber().trim().isEmpty()) {
            predicates.add(cb.like(cb.lower(root.get("orderCode")), 
                "%" + searchRequest.getOrderNumber().toLowerCase() + "%"));
        }

        if (searchRequest.getUserId() != null && !searchRequest.getUserId().trim().isEmpty()) {
            predicates.add(cb.equal(root.get("user").get("id"), 
                java.util.UUID.fromString(searchRequest.getUserId())));
        }

        if (searchRequest.getCustomerEmail() != null && !searchRequest.getCustomerEmail().trim().isEmpty()) {
            Join<Object, Object> customerInfoJoin = root.join("orderCustomerInfo", jakarta.persistence.criteria.JoinType.LEFT);
            predicates.add(cb.like(cb.lower(customerInfoJoin.get("email")), 
                "%" + searchRequest.getCustomerEmail().toLowerCase() + "%"));
        }

        if (searchRequest.getCustomerName() != null && !searchRequest.getCustomerName().trim().isEmpty()) {
            Join<Object, Object> customerInfoJoin = root.join("orderCustomerInfo", jakarta.persistence.criteria.JoinType.LEFT);
            String namePattern = "%" + searchRequest.getCustomerName().toLowerCase() + "%";
            predicates.add(cb.or(
                cb.like(cb.lower(customerInfoJoin.get("firstName")), namePattern),
                cb.like(cb.lower(customerInfoJoin.get("lastName")), namePattern)
            ));
        }

        if (searchRequest.getCustomerPhone() != null && !searchRequest.getCustomerPhone().trim().isEmpty()) {
            Join<Object, Object> customerInfoJoin = root.join("orderCustomerInfo", jakarta.persistence.criteria.JoinType.LEFT);
            predicates.add(cb.like(customerInfoJoin.get("phoneNumber"), 
                "%" + searchRequest.getCustomerPhone() + "%"));
        }

        if (searchRequest.getOrderStatus() != null && !searchRequest.getOrderStatus().trim().isEmpty()) {
            Join<Object, Object> trackingJoin = root.join("shopTrackings", jakarta.persistence.criteria.JoinType.LEFT);
            predicates.add(cb.equal(trackingJoin.get("status").as(String.class), searchRequest.getOrderStatus()));
        }

        if (searchRequest.getOrderStatuses() != null && !searchRequest.getOrderStatuses().isEmpty()) {
            Join<Object, Object> trackingJoin = root.join("shopTrackings", jakarta.persistence.criteria.JoinType.LEFT);
            predicates.add(trackingJoin.get("status").as(String.class).in(searchRequest.getOrderStatuses()));
        }

        if (searchRequest.getPaymentStatus() != null && !searchRequest.getPaymentStatus().trim().isEmpty()) {
            Join<Object, Object> transactionJoin = root.join("orderTransaction", jakarta.persistence.criteria.JoinType.LEFT);
            predicates.add(cb.equal(transactionJoin.get("status"), searchRequest.getPaymentStatus()));
        }

        if (searchRequest.getPaymentStatuses() != null && !searchRequest.getPaymentStatuses().isEmpty()) {
            Join<Object, Object> transactionJoin = root.join("orderTransaction", jakarta.persistence.criteria.JoinType.LEFT);
            predicates.add(transactionJoin.get("status").in(searchRequest.getPaymentStatuses()));
        }

        if (searchRequest.getCity() != null && !searchRequest.getCity().trim().isEmpty()) {
            Join<Object, Object> addressJoin = root.join("orderAddress", jakarta.persistence.criteria.JoinType.LEFT);
            predicates.add(cb.like(cb.lower(addressJoin.get("city")), 
                "%" + searchRequest.getCity().toLowerCase() + "%"));
        }

        if (searchRequest.getState() != null && !searchRequest.getState().trim().isEmpty()) {
            Join<Object, Object> addressJoin = root.join("orderAddress", jakarta.persistence.criteria.JoinType.LEFT);
            predicates.add(cb.like(cb.lower(addressJoin.get("state")), 
                "%" + searchRequest.getState().toLowerCase() + "%"));
        }

        if (searchRequest.getCountry() != null && !searchRequest.getCountry().trim().isEmpty()) {
            Join<Object, Object> addressJoin = root.join("orderAddress", jakarta.persistence.criteria.JoinType.LEFT);
            predicates.add(cb.like(cb.lower(addressJoin.get("country")), 
                "%" + searchRequest.getCountry().toLowerCase() + "%"));
        }

        if (searchRequest.getTotalMin() != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("totalAmount"), searchRequest.getTotalMin()));
        }

        if (searchRequest.getTotalMax() != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("totalAmount"), searchRequest.getTotalMax()));
        }

        if (searchRequest.getStartDate() != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), searchRequest.getStartDate()));
        }

        if (searchRequest.getEndDate() != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), searchRequest.getEndDate()));
        }

        if (searchRequest.getCreatedAtMin() != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), searchRequest.getCreatedAtMin()));
        }

        if (searchRequest.getCreatedAtMax() != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), searchRequest.getCreatedAtMax()));
        }

        if (searchRequest.getSearchKeyword() != null && !searchRequest.getSearchKeyword().trim().isEmpty()) {
            String keyword = "%" + searchRequest.getSearchKeyword().toLowerCase() + "%";
            Join<Object, Object> customerInfoJoin = root.join("orderCustomerInfo", jakarta.persistence.criteria.JoinType.LEFT);
            predicates.add(cb.or(
                cb.like(cb.lower(root.get("orderCode")), keyword),
                cb.like(cb.lower(customerInfoJoin.get("email")), keyword),
                cb.like(cb.lower(customerInfoJoin.get("firstName")), keyword),
                cb.like(cb.lower(customerInfoJoin.get("lastName")), keyword)
            ));
        }

        return predicates;
    }

    private void applySorting(CriteriaBuilder cb, Root<Order> root, CriteriaQuery<Order> query, Pageable pageable) {
        if (pageable.getSort().isSorted()) {
            List<jakarta.persistence.criteria.Order> orders = new ArrayList<>();
            pageable.getSort().forEach(sort -> {
                Path<?> path = root.get(sort.getProperty());
                if (sort.isAscending()) {
                    orders.add(cb.asc(path));
                } else {
                    orders.add(cb.desc(path));
                }
            });
            query.orderBy(orders.toArray(new jakarta.persistence.criteria.Order[0]));
        } else {
            query.orderBy(cb.desc(root.get("createdAt")));
        }
    }

    private long countSearchResults(OrderSearchDTO searchRequest) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Order> root = countQuery.from(Order.class);
        countQuery.select(cb.count(root));

        List<Predicate> predicates = buildPredicates(cb, root, searchRequest);

        if (!predicates.isEmpty()) {
            countQuery.where(cb.and(predicates.toArray(new Predicate[0])));
        }

        return entityManager.createQuery(countQuery).getSingleResult();
    }
}

