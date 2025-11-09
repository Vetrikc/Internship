package com.innowise.internship.entitiy;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_cards", indexes = {
        @Index(name = "idx_cards_user_id", columnList = "user_id"),
        @Index(name = "idx_cards_active", columnList = "active")
})
@NamedQueries({
        @NamedQuery(name = "PaymentCard.findByUserId", query = "SELECT p FROM PaymentCard p WHERE p.user.id = :userId")
})
@NamedNativeQueries({
        @NamedNativeQuery(name = "PaymentCard.countByUserId", query = "SELECT COUNT(*) FROM payment_cards WHERE user_id = :userId")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public class PaymentCard {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 20)
    private String number;

    @Column(nullable = false)
    private String holder;

    @Column(nullable = false)
    private LocalDate expirationDate;

    private Boolean active;

    @CreatedDate
    @Column( updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column()
    private LocalDateTime updatedAt;
}