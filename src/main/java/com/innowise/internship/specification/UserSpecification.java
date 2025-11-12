package com.innowise.internship.specification;

import com.innowise.internship.entitiy.User;
import org.springframework.data.jpa.domain.Specification;

public class UserSpecification {

    public static Specification<User> hasName(String name) {
        return (root, query, cb) -> name == null ? cb.conjunction() : cb.equal(root.get("name"), name);
    }

    public static Specification<User> hasSurname(String surname) {
        return (root, query, cb) -> surname == null ? cb.conjunction() : cb.equal(root.get("surname"), surname);
    }
}