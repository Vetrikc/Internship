package com.innowise.orderservice.specification;

import com.innowise.orderservice.entity.Order;
import com.innowise.orderservice.entity.OrderItem;
import jakarta.persistence.criteria.Fetch;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.List;

public class OrderSpecifications {

    public static Specification<Order> withCreationDateRangeAndStatuses(LocalDateTime from, LocalDateTime to, List<String> statuses) {
        return Specification.allOf(deletedFalse())
                .and(createdAtBetween(from, to))
                .and(statusIn(statuses));
    }

    private static Specification<Order> deletedFalse() {
        return (root, query, cb) -> cb.equal(root.get("deleted"), false);
    }

    private static Specification<Order> createdAtBetween(LocalDateTime from, LocalDateTime to) {
        if (from == null && to == null) {
            return (root, query, cb) -> cb.conjunction();
        }
        return (root, query, cb) -> {
            if (from == null) {
                return cb.lessThanOrEqualTo(root.get("createdAt"), to);
            }
            if (to == null) {
                return cb.greaterThanOrEqualTo(root.get("createdAt"), from);
            }
            return cb.between(root.get("createdAt"), from, to);
        };
    }

    private static Specification<Order> statusIn(List<String> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return (root, query, cb) -> cb.conjunction();
        }
        return (root, query, cb) -> root.get("status").in(statuses);
    }
    public static Specification<Order> fetchOrderItemsAndItem() {
        return (root, query, cb) -> {
            // Избегаем ошибки при count-запросе пагинации
            if (!query.getResultType().equals(Long.class) && !query.getResultType().equals(long.class)) {
                Fetch<Order, OrderItem> orderItemsFetch = root.fetch("orderItems", JoinType.LEFT);
                orderItemsFetch.fetch("item", JoinType.LEFT); // если нужен Item сразу
                query.distinct(true); // ОБЯЗАТЕЛЬНО, иначе дубли из-за join
            }
            return cb.conjunction();
        };
    }
}