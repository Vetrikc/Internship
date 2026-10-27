package com.innowise.paymentservice.unit;

import com.innowise.paymentservice.dao.PaymentRepository;
import com.innowise.paymentservice.dto.PaymentDTO;
import com.innowise.paymentservice.entity.Payment;
import com.innowise.paymentservice.event.PaymentEvent;
import com.innowise.paymentservice.mapper.PaymentMapper;
import com.innowise.paymentservice.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentMapper paymentMapper;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private KafkaTemplate<String, PaymentEvent> kafkaTemplate;

    @InjectMocks
    private PaymentService paymentService;

    private PaymentDTO paymentDTO;
    private Payment payment;

    @BeforeEach
    void setUp() {
        paymentDTO = new PaymentDTO();
        paymentDTO.setOrderId(1L);
        paymentDTO.setUserId(1L);
        paymentDTO.setPaymentAmount(new BigDecimal("100.00"));

        payment = new Payment();
        payment.setOrderId(1L);
        payment.setUserId(1L);
        payment.setPaymentAmount(new BigDecimal("100.00"));
    }

    @Test
    void createPayment_success() {
        when(paymentMapper.toEntity(paymentDTO)).thenReturn(payment);
        when(restTemplate.getForEntity(anyString(), eq(String.class)))
                .thenReturn(ResponseEntity.ok("2")); // even number -> SUCCESS
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);
        when(paymentMapper.toDto(payment)).thenReturn(paymentDTO);

        PaymentDTO result = paymentService.createPayment(paymentDTO);

        assertNotNull(result);
        assertEquals("SUCCESS", payment.getStatus());
        assertNotNull(payment.getTimestamp());

        ArgumentCaptor<PaymentEvent> eventCaptor = ArgumentCaptor.forClass(PaymentEvent.class);
        verify(kafkaTemplate).send(eq("payments"), eventCaptor.capture());
        PaymentEvent event = eventCaptor.getValue();
        assertEquals(1L, event.getOrderId());
        assertEquals("SUCCESS", event.getStatus());
    }

    @Test
    void createPayment_failed() {
        when(paymentMapper.toEntity(paymentDTO)).thenReturn(payment);
        when(restTemplate.getForEntity(anyString(), eq(String.class)))
                .thenReturn(ResponseEntity.ok("1")); // odd number -> FAILED
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);
        when(paymentMapper.toDto(payment)).thenReturn(paymentDTO);

        PaymentDTO result = paymentService.createPayment(paymentDTO);

        assertNotNull(result);
        assertEquals("FAILED", payment.getStatus());
        assertNotNull(payment.getTimestamp());

        ArgumentCaptor<PaymentEvent> eventCaptor = ArgumentCaptor.forClass(PaymentEvent.class);
        verify(kafkaTemplate).send(eq("payments"), eventCaptor.capture());
        PaymentEvent event = eventCaptor.getValue();
        assertEquals(1L, event.getOrderId());
        assertEquals("FAILED", event.getStatus());
    }

    @Test
    void findByUserIdOrOrderIdOrStatus() {
        List<Payment> payments = Arrays.asList(payment);
        when(paymentRepository.findByUserIdOrOrderIdOrStatus(1L, null, null)).thenReturn(payments);
        when(paymentMapper.toDto(payment)).thenReturn(paymentDTO);

        List<PaymentDTO> result = paymentService.findByUserIdOrOrderIdOrStatus(1L, null, null);

        assertEquals(1, result.size());
        assertEquals(paymentDTO, result.get(0));
    }

    @Test
    void getTotalSumForUserInDateRange() {
        Date start = new Date();
        Date end = new Date();
        BigDecimal sum = new BigDecimal("100.00");
        when(paymentRepository.getTotalSumForUserInDateRange(1L, start, end)).thenReturn(sum);

        BigDecimal result = paymentService.getTotalSumForUserInDateRange(1L, start, end);

        assertEquals(sum, result);
    }

    @Test
    void getTotalSumForAllInDateRange() {
        Date start = new Date();
        Date end = new Date();
        BigDecimal sum = new BigDecimal("100.00");
        when(paymentRepository.getTotalSumForAllInDateRange(start, end)).thenReturn(sum);

        BigDecimal result = paymentService.getTotalSumForAllInDateRange(start, end);

        assertEquals(sum, result);
    }
}