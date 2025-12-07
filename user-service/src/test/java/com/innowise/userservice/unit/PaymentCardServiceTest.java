package com.innowise.userservice.unit;

import com.innowise.userservice.dao.PaymentCardRepository;
import com.innowise.userservice.dao.UserRepository;
import com.innowise.userservice.dto.PaymentCardDto;
import com.innowise.userservice.entity.PaymentCard;
import com.innowise.userservice.entity.User;
import com.innowise.userservice.exception.CardLimitExceededException;
import com.innowise.userservice.exception.CardNotFoundException;
import com.innowise.userservice.exception.InvalidRequestException;
import com.innowise.userservice.exception.MissingUserException;
import com.innowise.userservice.mapper.PaymentCardMapper;
import com.innowise.userservice.service.PaymentCardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentCardServiceTest {

    @Mock
    private PaymentCardRepository paymentCardRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PaymentCardMapper paymentCardMapper;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache cache;

    @InjectMocks
    private PaymentCardService paymentCardService;

    private User user;
    private PaymentCard paymentCard;
    private PaymentCardDto paymentCardDto;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setPaymentCards(new ArrayList<>());

        paymentCard = new PaymentCard();
        paymentCard.setId(1L);
        paymentCard.setNumber("1234567890123456");
        paymentCard.setHolder("John Doe");
        paymentCard.setExpirationDate(LocalDate.of(2025, 12, 31));
        paymentCard.setActive(true);
        paymentCard.setUser(user);

        paymentCardDto = new PaymentCardDto();
        paymentCardDto.setId(1L);
        paymentCardDto.setUserId(1L);
        paymentCardDto.setNumber("1234567890123456");
        paymentCardDto.setHolder("John Doe");
        paymentCardDto.setExpirationDate(LocalDate.of(2025, 12, 31));
        paymentCardDto.setActive(true);

        lenient().when(cacheManager.getCache("users")).thenReturn(cache);
    }

    @Test
    void createCardUserIdNullThrowsMissingUserException() {
        paymentCardDto.setUserId(null);

        assertThrows(MissingUserException.class, () -> paymentCardService.createCard(paymentCardDto));
        verifyNoInteractions(userRepository, paymentCardRepository, paymentCardMapper, cache);
    }

    @Test
    void createCardUserNotFoundThrowsInvalidRequestException() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(InvalidRequestException.class, () -> paymentCardService.createCard(paymentCardDto));
        verify(userRepository).findById(1L);
        verifyNoInteractions(paymentCardRepository, paymentCardMapper, cache);
    }

    @Test
    void createCardCardLimitExceededThrowsCardLimitExceededException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(paymentCardRepository.countByUserId(1L)).thenReturn(5L);

        assertThrows(CardLimitExceededException.class, () -> paymentCardService.createCard(paymentCardDto));
        verify(userRepository).findById(1L);
        verify(paymentCardRepository).countByUserId(1L);
        verifyNoInteractions(paymentCardMapper, cache);
    }

    @Test
    void createCardSuccessfulCreationReturnsDtoAndEvictsCache() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(paymentCardRepository.countByUserId(1L)).thenReturn(4L);
        when(paymentCardMapper.toEntity(paymentCardDto)).thenReturn(paymentCard);
        when(paymentCardRepository.save(paymentCard)).thenReturn(paymentCard);

        when(paymentCardMapper.toDto(paymentCard)).thenReturn(paymentCardDto);


        PaymentCardDto result = paymentCardService.createCard(paymentCardDto);

        assertEquals(paymentCardDto, result);
        verify(userRepository).findById(1L);
        verify(paymentCardRepository).countByUserId(1L);
        verify(paymentCardMapper).toEntity(paymentCardDto);
        verify(paymentCardRepository).save(paymentCard);
        verify(paymentCardMapper).toDto(paymentCard);
    }

    @Test
    void getCardByIdCardFoundReturnsOptionalDto() {
        when(paymentCardRepository.findById(1L)).thenReturn(Optional.of(paymentCard));
        when(paymentCardMapper.toDto(paymentCard)).thenReturn(paymentCardDto);

        PaymentCardDto result = paymentCardService.getCardById(1L);

        assertNotNull(result);
        assertEquals(paymentCardDto, result);
        verify(paymentCardRepository).findById(1L);
        verify(paymentCardMapper).toDto(paymentCard);
        verifyNoInteractions(cache);
    }

    @Test
    void getCardByIdCardNotFoundReturnsEmptyOptional() {
        when(paymentCardRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(CardNotFoundException.class,
                () -> paymentCardService.getCardById(1L));

        verify(paymentCardRepository).findById(1L);
        verifyNoInteractions(paymentCardMapper, cache);
    }

    @Test
    void getAllCardsReturnsPagedDtos() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<PaymentCard> page = new PageImpl<>(List.of(paymentCard));
        when(paymentCardRepository.findAll(pageable)).thenReturn(page);
        when(paymentCardMapper.toDto(paymentCard)).thenReturn(paymentCardDto);

        Page<PaymentCardDto> result = paymentCardService.getAllCards(pageable);

        assertEquals(1, result.getContent().size());
        assertEquals(paymentCardDto, result.getContent().getFirst());
        verify(paymentCardRepository).findAll(pageable);
        verify(paymentCardMapper).toDto(paymentCard);
        verifyNoInteractions(cache);
    }

    @Test
    void getCardsByUserIdReturnsListOfDtos() {
        when(paymentCardRepository.findByUserId(1L)).thenReturn(List.of(paymentCard));
        when(paymentCardMapper.toDto(paymentCard)).thenReturn(paymentCardDto);

        List<PaymentCardDto> result = paymentCardService.getCardsByUserId(1L);

        assertEquals(1, result.size());
        assertEquals(paymentCardDto, result.getFirst());
        verify(paymentCardRepository).findByUserId(1L);
        verify(paymentCardMapper).toDto(paymentCard);
        verifyNoInteractions(cache);
    }

    @Test
    void updateCardCardNotFoundThrowsCardNotFoundException() {
        when(paymentCardRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(CardNotFoundException.class, () -> paymentCardService.updateCard(1L, paymentCardDto));
        verify(paymentCardRepository).findById(1L);
        verifyNoInteractions(paymentCardMapper, cache);
    }

    @Test
    void updateCardSuccessfulUpdateReturnsUpdatedDtoAndEvictsCache() {
        PaymentCard updatedEntity = new PaymentCard();
        updatedEntity.setNumber("newNumber");
        updatedEntity.setHolder("newHolder");
        updatedEntity.setExpirationDate(LocalDate.of(2026, 12, 31));

        PaymentCardDto updatedDto = new PaymentCardDto();
        updatedDto.setId(1L);
        updatedDto.setUserId(1L);
        updatedDto.setNumber("newNumber");
        updatedDto.setHolder("newHolder");
        updatedDto.setExpirationDate(LocalDate.of(2026, 12, 31));
        updatedDto.setActive(true);

        when(paymentCardRepository.findById(1L)).thenReturn(Optional.of(paymentCard));
        when(paymentCardMapper.toEntity(updatedDto)).thenReturn(updatedEntity);
        when(paymentCardRepository.save(paymentCard)).thenReturn(paymentCard);
        when(paymentCardMapper.toDto(paymentCard)).thenReturn(updatedDto);

        PaymentCardDto result = paymentCardService.updateCard(1L, updatedDto);

        assertEquals(updatedDto, result);
        assertEquals("newNumber", paymentCard.getNumber());
        assertEquals("newHolder", paymentCard.getHolder());
        assertEquals(LocalDate.of(2026, 12, 31), paymentCard.getExpirationDate());
        verify(paymentCardRepository).findById(1L);
        verify(paymentCardMapper).toEntity(updatedDto);
        verify(paymentCardRepository).save(paymentCard);
        verify(paymentCardMapper).toDto(paymentCard);
    }

    @Test
    void deleteCardCardNotFoundThrowsCardNotFoundException() {
        when(paymentCardRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(CardNotFoundException.class, () -> paymentCardService.deleteCard(1L));
        verify(paymentCardRepository).findById(1L);
        verifyNoInteractions(paymentCardMapper, cache);
    }

    @Test
    void deleteCardSuccessfulDeleteReturnsDeletedDtoAndEvictsCache() {
        when(paymentCardRepository.findById(1L)).thenReturn(Optional.of(paymentCard));
        when(paymentCardMapper.toDto(paymentCard)).thenReturn(paymentCardDto);
        doNothing().when(paymentCardRepository).deleteById(1L);

        PaymentCardDto result = paymentCardService.deleteCard(1L);

        assertEquals(paymentCardDto, result);
        verify(paymentCardRepository).findById(1L);
        verify(paymentCardMapper).toDto(paymentCard);
        verify(paymentCardRepository).deleteById(1L);
    }

    @Test
    void activateCardCardNotFoundThrowsCardNotFoundException() {
        when(paymentCardRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(CardNotFoundException.class, () -> paymentCardService.activateCard(1L));
        verify(paymentCardRepository).findById(1L);
        verifyNoInteractions(paymentCardMapper, cache);
    }

    @Test
    void activateCardSuccessfulActivationReturnsUpdatedDtoAndEvictsCache() {
        paymentCard.setActive(false);
        when(paymentCardRepository.findById(1L)).thenReturn(Optional.of(paymentCard));
        when(paymentCardRepository.save(paymentCard)).thenReturn(paymentCard);
        when(paymentCardMapper.toDto(paymentCard)).thenReturn(paymentCardDto);

        PaymentCardDto result = paymentCardService.activateCard(1L);

        assertEquals(paymentCardDto, result);
        assertTrue(paymentCard.getActive());
        verify(paymentCardRepository).findById(1L);
        verify(paymentCardRepository).save(paymentCard);
        verify(paymentCardMapper).toDto(paymentCard);
    }

    @Test
    void deactivateCardCardNotFoundThrowsCardNotFoundException() {
        when(paymentCardRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(CardNotFoundException.class, () -> paymentCardService.deactivateCard(1L));
        verify(paymentCardRepository).findById(1L);
        verifyNoInteractions(paymentCardMapper, cache);
    }

    @Test
    void deactivateCardSuccessfulDeactivationReturnsUpdatedDtoAndEvictsCache() {
        paymentCard.setActive(true);
        when(paymentCardRepository.findById(1L)).thenReturn(Optional.of(paymentCard));
        when(paymentCardRepository.save(paymentCard)).thenReturn(paymentCard);
        when(paymentCardMapper.toDto(paymentCard)).thenReturn(paymentCardDto);

        PaymentCardDto result = paymentCardService.deactivateCard(1L);

        assertEquals(paymentCardDto, result);
        assertFalse(paymentCard.getActive());
        verify(paymentCardRepository).findById(1L);
        verify(paymentCardRepository).save(paymentCard);
        verify(paymentCardMapper).toDto(paymentCard);
    }
}