package com.innowise.paymentservice.dao;

import com.innowise.paymentservice.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long>, JpaSpecificationExecutor<Payment> {

    @Query("SELECT p FROM Payment p WHERE "
            + "(:userId IS NULL OR p.userId = :userId) AND "
            + "(:orderId IS NULL OR p.orderId = :orderId) AND "
            + "(:status IS NULL OR p.status = :status)")
    List<Payment> findByUserIdOrOrderIdOrStatus(
            @Param("userId") Long userId,
            @Param("orderId") Long orderId,
            @Param("status") String status);

    @Query("SELECT SUM(p.paymentAmount) FROM Payment p "
            + "WHERE p.userId = :userId AND p.timestamp BETWEEN :startDate AND :endDate")
    BigDecimal getTotalSumForUserInDateRange(
            @Param("userId") Long userId,
            @Param("startDate") Date startDate,
            @Param("endDate") Date endDate);

    @Query("SELECT SUM(p.paymentAmount) FROM Payment p "
            + "WHERE p.timestamp BETWEEN :startDate AND :endDate")
    BigDecimal getTotalSumForAllInDateRange(
            @Param("startDate") Date startDate,
            @Param("endDate") Date endDate);
}