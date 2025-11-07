package com.innowise.internship.service;

import com.innowise.internship.entitiy.PaymentCard;
import com.innowise.internship.entitiy.User;
import com.innowise.internship.dao.PaymentCardRepository;
import com.innowise.internship.dao.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PaymentCardService {

    private final PaymentCardRepository paymentCardRepository;
    private final UserRepository userRepository;

    public PaymentCardService(PaymentCardRepository paymentCardRepository, UserRepository userRepository) {
        this.paymentCardRepository = paymentCardRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public PaymentCard createCard(PaymentCard card) {
        if (card.getUser() == null || card.getUser().getId() == null) {
            throw new RuntimeException("User required");
        }
        Long userId = card.getUser().getId();
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        long count = paymentCardRepository.countByUserId(userId);
        if (count >= 5) {
            throw new RuntimeException("User cannot have more than 5 cards");
        }
        card.setUser(user);
        user.getPaymentCards().add(card);
        userRepository.save(user);
        return card;
    }

    public Optional<PaymentCard> getCardById(Long id) {
        return paymentCardRepository.findById(id);
    }

    public Page<PaymentCard> getAllCards(Pageable pageable) {
        return paymentCardRepository.findAll(pageable);
    }

    public List<PaymentCard> getCardsByUserId(Long userId) {
        return paymentCardRepository.findByUserId(userId);
    }

    @Transactional
    public PaymentCard updateCard(Long id, PaymentCard updatedCard) {
        return paymentCardRepository.findById(id).map(card -> {
            if (updatedCard.getNumber() != null) card.setNumber(updatedCard.getNumber());
            if (updatedCard.getHolder() != null) card.setHolder(updatedCard.getHolder());
            if (updatedCard.getExpirationDate() != null) card.setExpirationDate(updatedCard.getExpirationDate());
            return paymentCardRepository.save(card);
        }).orElseThrow(() -> new RuntimeException("Card not found"));
    }

    @Transactional
    public void activateCard(Long id) {
        paymentCardRepository.findById(id).ifPresent(card -> {
            card.setActive(true);
            paymentCardRepository.save(card);
        });
    }

    @Transactional
    public void deactivateCard(Long id) {
        paymentCardRepository.findById(id).ifPresent(card -> {
            card.setActive(false);
            paymentCardRepository.save(card);
        });
    }
}