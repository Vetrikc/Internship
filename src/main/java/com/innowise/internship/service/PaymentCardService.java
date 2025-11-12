package com.innowise.internship.service;

import com.innowise.internship.dto.PaymentCardDto;
import com.innowise.internship.entitiy.PaymentCard;
import com.innowise.internship.entitiy.User;
import com.innowise.internship.exception.CardNotFoundException;
import com.innowise.internship.mapper.PaymentCardMapper;
import com.innowise.internship.dao.PaymentCardRepository;
import com.innowise.internship.dao.UserRepository;
import com.innowise.internship.exception.CardLimitExceededException;
import com.innowise.internship.exception.MissingUserException;
import com.innowise.internship.exception.UserNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PaymentCardService {

    private final PaymentCardRepository paymentCardRepository;
    private final UserRepository userRepository;
    private final PaymentCardMapper paymentCardMapper;

    public PaymentCardService(PaymentCardRepository paymentCardRepository, UserRepository userRepository, PaymentCardMapper paymentCardMapper) {
        this.paymentCardRepository = paymentCardRepository;
        this.userRepository = userRepository;
        this.paymentCardMapper = paymentCardMapper;
    }

    @Transactional
    public PaymentCardDto createCard(PaymentCardDto cardDto) {
        Long userId = cardDto.getUserId();
        if (userId == null) {
            throw new MissingUserException("User ID required");
        }
        User user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException("User not found"));
        long count = paymentCardRepository.countByUserId(userId);
        if (count >= 5) {
            throw new CardLimitExceededException("User cannot have more than 5 cards");
        }
        PaymentCard card = paymentCardMapper.toEntity(cardDto);
        card.setUser(user);
        user.getPaymentCards().add(card);
        userRepository.save(user);
        return paymentCardMapper.toDto(card);
    }

    public Optional<PaymentCardDto> getCardById(Long id) {
        return paymentCardRepository.findById(id).map(paymentCardMapper::toDto);
    }

    public Page<PaymentCardDto> getAllCards(Pageable pageable) {
        return paymentCardRepository.findAll(pageable).map(paymentCardMapper::toDto);
    }

    public List<PaymentCardDto> getCardsByUserId(Long userId) {
        return paymentCardRepository.findByUserId(userId).stream()
                .map(paymentCardMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public PaymentCardDto updateCard(Long id, PaymentCardDto updatedCardDto) {
        return paymentCardRepository.findById(id).map(card -> {
            PaymentCard updatedCard = paymentCardMapper.toEntity(updatedCardDto);
            if (updatedCard.getNumber() != null) card.setNumber(updatedCard.getNumber());
            if (updatedCard.getHolder() != null) card.setHolder(updatedCard.getHolder());
            if (updatedCard.getExpirationDate() != null) card.setExpirationDate(updatedCard.getExpirationDate());
            PaymentCard savedCard = paymentCardRepository.save(card);
            return paymentCardMapper.toDto(savedCard);
        }).orElseThrow(() -> new CardNotFoundException("Card not found"));
    }

    @Transactional
    public void deleteCard(Long id) {
        paymentCardRepository.deleteById(id);
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