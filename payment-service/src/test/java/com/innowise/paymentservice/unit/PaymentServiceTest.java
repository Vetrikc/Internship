package com.innowise.paymentservice.unit;

import com.innowise.paymentservice.dao.PaymentRepository;
import com.innowise.paymentservice.dto.PaymentDTO;
import com.innowise.paymentservice.entity.Payment;
import com.innowise.paymentservice.mapper.PaymentMapper;
import com.innowise.paymentservice.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentMapper paymentMapper;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private PaymentService paymentService;

    private PaymentDTO testDto;
    private Payment testEntity;

    @BeforeEach
    void setUp() {
        testDto = new PaymentDTO();
        testDto.setOrderId(1L);
        testDto.setUserId(2L);
        testDto.setPaymentAmount(new BigDecimal("100.00"));

        testEntity = new Payment();
        testEntity.setId(1L);
        testEntity.setOrderId(1L);
        testEntity.setUserId(2L);
        testEntity.setPaymentAmount(new BigDecimal("100.00"));
    }

    @Test
    void createPayment_SuccessStatus() {
        // Mock mapper to entity
        when(paymentMapper.toEntity(any(PaymentDTO.class))).thenReturn(testEntity);

        // Mock external API to return even number
        ResponseEntity<String> response = ResponseEntity.ok("4\n");
        when(restTemplate.getForEntity(eq("https://www.random.org/integers/?num=1&min=1&max=10&col=1&base=10&format=plain&rnd=new"), eq(String.class)))
                .thenReturn(response);

        // Mock save
        when(paymentRepository.save(any(Payment.class))).thenReturn(testEntity);

        // Mock mapper to DTO
        when(paymentMapper.toDto(any(Payment.class))).thenReturn(testDto);

        PaymentDTO result = paymentService.createPayment(testDto);

        assertNotNull(result);
        assertEquals("SUCCESS", testEntity.getStatus());
        assertNotNull(testEntity.getTimestamp());
        verify(paymentRepository).save(any(Payment.class));
        verify(paymentMapper).toDto(any(Payment.class));
    }

    @Test
    void createPayment_FailedStatus() {
        // Mock mapper to entity
        when(paymentMapper.toEntity(any(PaymentDTO.class))).thenReturn(testEntity);

        // Mock external API to return odd number
        ResponseEntity<String> response = ResponseEntity.ok("5\n");
        when(restTemplate.getForEntity(eq("https://www.random.org/integers/?num=1&min=1&max=10&col=1&base=10&format=plain&rnd=new"), eq(String.class)))
                .thenReturn(response);

        // Mock save
        when(paymentRepository.save(any(Payment.class))).thenReturn(testEntity);

        // Mock mapper to DTO
        when(paymentMapper.toDto(any(Payment.class))).thenReturn(testDto);

        PaymentDTO result = paymentService.createPayment(testDto);

        assertNotNull(result);
        assertEquals("FAILED", testEntity.getStatus());
        assertNotNull(testEntity.getTimestamp());
        verify(paymentRepository).save(any(Payment.class));
        verify(paymentMapper).toDto(any(Payment.class));
    }

    @Test
    void findByUserIdOrOrderIdOrStatus() {
        Long userId = 2L;
        Long orderId = 1L;
        String status = "SUCCESS";

        List<Payment> entities = Arrays.asList(testEntity);
        when(paymentRepository.findByUserIdOrOrderIdOrStatus(userId, orderId, status)).thenReturn(entities);

        PaymentDTO mappedDto = new PaymentDTO();
        when(paymentMapper.toDto(any(Payment.class))).thenReturn(mappedDto);

        List<PaymentDTO> result = paymentService.findByUserIdOrOrderIdOrStatus(userId, orderId, status);

        assertEquals(1, result.size());
        verify(paymentRepository).findByUserIdOrOrderIdOrStatus(userId, orderId, status);
        verify(paymentMapper, times(1)).toDto(any(Payment.class));
    }

    @Test
    void getTotalSumForUserInDateRange() {
        Long userId = 2L;
        Date startDate = new Date();
        Date endDate = new Date();
        BigDecimal expectedSum = new BigDecimal("200.00");

        when(paymentRepository.getTotalSumForUserInDateRange(userId, startDate, endDate)).thenReturn(expectedSum);

        BigDecimal result = paymentService.getTotalSumForUserInDateRange(userId, startDate, endDate);

        assertEquals(expectedSum, result);
        verify(paymentRepository).getTotalSumForUserInDateRange(userId, startDate, endDate);
    }

    @Test
    void getTotalSumForAllInDateRange() {
        Date startDate = new Date();
        Date endDate = new Date();
        BigDecimal expectedSum = new BigDecimal("500.00");

        when(paymentRepository.getTotalSumForAllInDateRange(startDate, endDate)).thenReturn(expectedSum);

        BigDecimal result = paymentService.getTotalSumForAllInDateRange(startDate, endDate);

        assertEquals(expectedSum, result);
        verify(paymentRepository).getTotalSumForAllInDateRange(startDate, endDate);
    }
}