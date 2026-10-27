package com.innowise.paymentservice.integration;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.jetty12.Jetty12HttpServerFactory;
import com.innowise.paymentservice.PaymentServiceApplication;
import com.innowise.paymentservice.dao.PaymentRepository;
import com.innowise.paymentservice.dto.PaymentDTO;
import com.innowise.paymentservice.entity.Payment;
import com.innowise.paymentservice.service.PaymentService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = PaymentServiceApplication.class)
@Testcontainers
@Transactional
@Rollback
class PaymentServiceIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("user")
            .withPassword("password");

    @Container
    static ConfluentKafkaContainer kafka = new ConfluentKafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.0"));

    static WireMockServer wireMockServer = new WireMockServer(
            options()
                    .httpServerFactory(new Jetty12HttpServerFactory())
                    .dynamicPort()
    );

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentRepository paymentRepository;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.payment.jdbc-url", postgres::getJdbcUrl);
        registry.add("spring.datasource.payment.username", postgres::getUsername);
        registry.add("spring.datasource.payment.password", postgres::getPassword);
        registry.add("spring.datasource.payment.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.kafka.producer.bootstrap-servers", kafka::getBootstrapServers);

        registry.add("spring.kafka.producer.key-serializer", () -> "org.apache.kafka.common.serialization.StringSerializer");
        registry.add("spring.kafka.producer.value-serializer", () -> "org.springframework.kafka.support.serializer.JsonSerializer");
    }

    @BeforeAll
    static void startContainers() {
        postgres.start();
        kafka.start();
        wireMockServer.start();
    }

    @AfterAll
    static void stopContainers() {
        postgres.stop();
        kafka.stop();
        wireMockServer.stop();
    }

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();
        // Настроить WireMock для external API
        configureFor("localhost", wireMockServer.port());
        stubFor(get(urlEqualTo("/integers/?num=1&min=1&max=10&col=1&base=10&format=plain&rnd=new"))
                .willReturn(aResponse().withBody("2"))); // SUCCESS
    }

    @Test
    void createPayment_integration() {
        PaymentDTO dto = new PaymentDTO();
        dto.setOrderId(1L);
        dto.setUserId(1L);
        dto.setPaymentAmount(new BigDecimal("100.00"));

        PaymentDTO result = paymentService.createPayment(dto);

        assertNotNull(result.getId());
        assertNotNull(result.getTimestamp());

        Payment saved = paymentRepository.findById(result.getId()).orElse(null);
        assertNotNull(saved);
        assertEquals(result.getStatus(), saved.getStatus());
    }

    @Test
    void findByUserIdOrOrderIdOrStatus_integration() {
        Payment payment = new Payment();
        payment.setUserId(1L);
        payment.setOrderId(1L);
        payment.setStatus("SUCCESS");
        payment.setTimestamp(new Date());
        payment.setPaymentAmount(new BigDecimal("100.00"));
        paymentRepository.save(payment);

        List<PaymentDTO> result = paymentService.findByUserIdOrOrderIdOrStatus(1L, null, null);

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getUserId());
    }

    @Test
    void getTotalSumForUserInDateRange_integration() {
        Payment payment = new Payment();
        payment.setUserId(1L);
        payment.setOrderId(1L);
        payment.setStatus("SUCCESS");
        payment.setTimestamp(new Date());
        payment.setPaymentAmount(new BigDecimal("100.00"));
        paymentRepository.save(payment);

        Date start = new Date(System.currentTimeMillis() - 86400000); // yesterday
        Date end = new Date(System.currentTimeMillis() + 86400000); // tomorrow

        BigDecimal sum = paymentService.getTotalSumForUserInDateRange(1L, start, end);

        assertEquals(new BigDecimal("100.00"), sum);
    }

    @Test
    void getTotalSumForAllInDateRange_integration() {
        Payment payment = new Payment();
        payment.setUserId(1L);
        payment.setOrderId(1L);
        payment.setStatus("SUCCESS");
        payment.setTimestamp(new Date());
        payment.setPaymentAmount(new BigDecimal("100.00"));
        paymentRepository.save(payment);

        Date start = new Date(System.currentTimeMillis() - 86400000);
        Date end = new Date(System.currentTimeMillis() + 86400000);

        BigDecimal sum = paymentService.getTotalSumForAllInDateRange(start, end);

        assertEquals(new BigDecimal("100.00"), sum);
    }
}