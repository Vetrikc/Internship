package com.innowise.paymentservice.service;

import com.innowise.paymentservice.dao.PaymentRepository;
import com.innowise.paymentservice.dto.PaymentDTO;
import com.innowise.paymentservice.entity.Payment;
import com.innowise.paymentservice.mapper.PaymentMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class PaymentService {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PaymentMapper paymentMapper;

    @Autowired
    private RestTemplate restTemplate;

    public PaymentDTO createPayment(PaymentDTO dto) {
        Payment entity = paymentMapper.toEntity(dto);

        // Call external API for random number
        String apiUrl = "https://www.random.org/integers/?num=1&min=1&max=10&col=1&base=10&format=plain&rnd=new";
        ResponseEntity<String> response = restTemplate.getForEntity(apiUrl, String.class);
        int randomNumber = Integer.parseInt(response.getBody().trim());

        String status = (randomNumber % 2 == 0) ? "SUCCESS" : "FAILED";
        entity.setStatus(status);
        entity.setTimestamp(new Date());

        Payment saved = paymentRepository.save(entity);
        return paymentMapper.toDto(saved);
    }

    public List<PaymentDTO> findByUserIdOrOrderIdOrStatus(Long userId, Long orderId, String status) {
        List<Payment> payments = paymentRepository.findByUserIdOrOrderIdOrStatus(userId, orderId, status);
        return payments.stream().map(paymentMapper::toDto).collect(Collectors.toList());
    }

    public BigDecimal getTotalSumForUserInDateRange(Long userId, Date startDate, Date endDate) {
        return paymentRepository.getTotalSumForUserInDateRange(userId, startDate, endDate);
    }

    public BigDecimal getTotalSumForAllInDateRange(Date startDate, Date endDate) {
        return paymentRepository.getTotalSumForAllInDateRange(startDate, endDate);
    }
}