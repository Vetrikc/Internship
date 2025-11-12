package com.innowise.internship.dao;

import com.innowise.internship.entitiy.PaymentCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PaymentCardRepository extends JpaRepository<PaymentCard, Long>, JpaSpecificationExecutor<PaymentCard> {
    @Query(value = "SELECT COUNT(*) FROM payment_cards WHERE user_id = :userId", nativeQuery = true)
    long countByUserId(@Param("userId") Long userId);
    List<PaymentCard> findByUserId(Long userId);
}