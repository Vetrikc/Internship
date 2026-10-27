package com.innowise.orderservice.kafka;

import com.innowise.orderservice.event.PaymentEvent;
import com.innowise.orderservice.entity.Order;
import com.innowise.orderservice.dao.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentConsumer {

    private static final Logger logger = LoggerFactory.getLogger(PaymentConsumer.class);

    private final OrderRepository orderRepository;

    public PaymentConsumer(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @KafkaListener(topics = "payments", groupId = "order-group", containerFactory = "kafkaListenerContainerFactory")
    public void consumePaymentEvent(PaymentEvent event) {
        logger.info("Получено событие платежа: orderId={}, status={}", event.getOrderId(), event.getStatus());

        Order order = orderRepository.findByIdAndDeletedFalse(event.getOrderId())
                .orElse(null);

        if (order != null) {
            String newStatus = "PENDING";  // По умолчанию
            if ("SUCCESS".equals(event.getStatus())) {
                newStatus = "PAID";
            } else if ("FAILED".equals(event.getStatus())) {
                newStatus = "PAYMENT_FAILED";
            }
            order.setStatus(newStatus);
            orderRepository.save(order);
            logger.info("Статус заказа {} обновлен на {}", event.getOrderId(), newStatus);
        } else {
            logger.warn("Заказ с id={} не найден", event.getOrderId());
        }
    }
}