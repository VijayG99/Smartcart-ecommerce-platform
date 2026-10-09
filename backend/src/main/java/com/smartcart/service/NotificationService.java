package com.smartcart.service;

import com.smartcart.event.OrderCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Development-friendly notifications: always logs. Sends a real email only when
 * NOTIFICATION_EMAIL_ENABLED=true and the MAIL_* variables point to an SMTP server.
 */
@Slf4j
@Service
public class NotificationService {
    private final JavaMailSender mailSender;
    private final boolean emailEnabled;
    private final String from;

    public NotificationService(JavaMailSender mailSender,
                               @Value("${smartcart.notification.email-enabled}") boolean emailEnabled,
                               @Value("${smartcart.notification.from}") String from) {
        this.mailSender = mailSender;
        this.emailEnabled = emailEnabled;
        this.from = from;
    }

    public void sendOrderPlaced(OrderCreatedEvent event) {
        String text = "Thanks for your order #" + event.orderId() + ". Total: " + event.totalAmount()
                + ". We will let you know when it ships.";
        log.info("NOTIFICATION to {}: {}", event.customerEmail(), text);
        if (emailEnabled) {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(from);
            msg.setTo(event.customerEmail());
            msg.setSubject("SmartCart order #" + event.orderId() + " received");
            msg.setText(text);
            mailSender.send(msg);   // an exception here triggers the Kafka retry + dead-letter flow
        }
    }
}
