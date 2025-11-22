package com.innowise.internship.service;

import com.innowise.internship.dao.PaymentCardRepository;
import com.innowise.internship.dao.UserRepository;
import com.innowise.internship.dto.PaymentCardDto;
import com.innowise.internship.entity.PaymentCard;
import com.innowise.internship.entity.User;
import com.innowise.internship.exception.CardLimitExceededException;
import com.innowise.internship.exception.CardNotFoundException;
import com.innowise.internship.exception.InvalidRequestException;
import com.innowise.internship.exception.MissingUserException;
import com.innowise.internship.mapper.PaymentCardMapper;
import jakarta.transaction.Transactional;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PaymentCardService {

    private final PaymentCardRepository paymentCardRepository;
    private final UserRepository userRepository;
    private final PaymentCardMapper paymentCardMapper;

    public PaymentCardService(PaymentCardRepository paymentCardRepository,
                              UserRepository userRepository, PaymentCardMapper paymentCardMapper) {
        this.paymentCardRepository = paymentCardRepository;
        this.userRepository = userRepository;
        this.paymentCardMapper = paymentCardMapper;
    }

    @Transactional
    @CacheEvict(value = "users", key = "#result.userId")
    public PaymentCardDto createCard(PaymentCardDto cardDto) {
        Long userId = cardDto.getUserId();
        if (userId == null) {
            throw new MissingUserException("User ID required");
        }
        User user = userRepository.findById(userId).orElseThrow(() -> new InvalidRequestException("User not found"));
        long count = paymentCardRepository.countByUserId(userId);
        if (count >= 5) {
            throw new CardLimitExceededException();
        }
        PaymentCard card = paymentCardMapper.toEntity(cardDto);
        card.setActive(true);//todo hardcoded
        card.setUser(user);
        user.getPaymentCards().add(card);
        PaymentCard savedCard = paymentCardRepository.save(card);
        return paymentCardMapper.toDto(savedCard);
    }

    @Transactional
    public PaymentCardDto getCardById(Long id) {
        return paymentCardRepository.findById(id).map(paymentCardMapper::toDto).orElseThrow(() -> new CardNotFoundException(id));
    }

    @Transactional
    public Page<PaymentCardDto> getAllCards(Pageable pageable) {
        return paymentCardRepository.findAll(pageable).map(paymentCardMapper::toDto);
    }

    @Transactional
    public List<PaymentCardDto> getCardsByUserId(Long userId) {
        return paymentCardRepository.findByUserId(userId).stream()
                .map(paymentCardMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    @CacheEvict(value = "users", key = "#result.userId")
    public PaymentCardDto updateCard(Long id, PaymentCardDto updatedCardDto) {
        return paymentCardRepository.findById(id).map(card -> {
            PaymentCard updatedCard = paymentCardMapper.toEntity(updatedCardDto);
            if (updatedCard.getNumber() != null) card.setNumber(updatedCard.getNumber());
            if (updatedCard.getHolder() != null) card.setHolder(updatedCard.getHolder());
            if (updatedCard.getExpirationDate() != null) card.setExpirationDate(updatedCard.getExpirationDate());
            PaymentCard savedCard = paymentCardRepository.save(card);
            return paymentCardMapper.toDto(savedCard);
        }).orElseThrow(() -> new CardNotFoundException(id));
    }

    @Transactional
    @CacheEvict(value = "users", key = "#result.userId")
    public PaymentCardDto updateUserCard(Long id, PaymentCardDto updatedCardDto, Long userId) {
        return paymentCardRepository.findById(id).map(card -> {
            if (!card.getUser().getId().equals(userId)) throw new CardNotFoundException(id);
            PaymentCard updatedCard = paymentCardMapper.toEntity(updatedCardDto);
            if (updatedCard.getNumber() != null) card.setNumber(updatedCard.getNumber());
            if (updatedCard.getHolder() != null) card.setHolder(updatedCard.getHolder());
            if (updatedCard.getExpirationDate() != null) card.setExpirationDate(updatedCard.getExpirationDate());
            PaymentCard savedCard = paymentCardRepository.save(card);
            return paymentCardMapper.toDto(savedCard);
        }).orElseThrow(() -> new CardNotFoundException(id));
    }

    @Transactional
    @CacheEvict(value = "users", key = "#result.userId")
    public PaymentCardDto deleteCard(Long id) {
        return paymentCardRepository.findById(id).map(card -> {
            PaymentCardDto deletedDto = paymentCardMapper.toDto(card); // Копируем DTO перед удалением
            paymentCardRepository.deleteById(id);
            return deletedDto; // Возвращаем копию удалённой карты
        }).orElseThrow(() -> new CardNotFoundException(id));
    }

    @Transactional
    @CacheEvict(value = "users", key = "#result.userId")
    public PaymentCardDto deleteUserCard(Long id, Long userId) {
        return paymentCardRepository.findByIdAndUserId(id, userId).map(card -> {
            PaymentCardDto deletedDto = paymentCardMapper.toDto(card);
            paymentCardRepository.deleteById(id);
            return deletedDto;
        }).orElseThrow(() -> new CardNotFoundException(id));
    }

    @Transactional
    @CacheEvict(value = "users", key = "#result.userId")
    public PaymentCardDto activateCard(Long id) {
        return paymentCardRepository.findById(id).map(card -> {
            card.setActive(true);
            PaymentCard savedCard = paymentCardRepository.save(card);
            return paymentCardMapper.toDto(savedCard); // Возвращаем обновлённый DTO
        }).orElseThrow(() -> new CardNotFoundException(id));
    }

    @Transactional
    @CacheEvict(value = "users", key = "#result.userId")
    public PaymentCardDto deactivateCard(Long id) {
        return paymentCardRepository.findById(id).map(card -> {
            card.setActive(false);
            PaymentCard savedCard = paymentCardRepository.save(card);
            return paymentCardMapper.toDto(savedCard); // Возвращаем обновлённый DTO
        }).orElseThrow(() -> new CardNotFoundException(id));
    }

    @Transactional
    public PaymentCardDto getUserCardById(Long id, Long userId) {
        return paymentCardRepository.findByIdAndUserId(id, userId)
                .map(paymentCardMapper::toDto)
                .orElseThrow(() -> new CardNotFoundException(id));
    }
}