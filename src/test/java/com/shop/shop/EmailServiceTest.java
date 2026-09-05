package com.shop.shop;

import com.shop.shop.entity.Order;
import com.shop.shop.entity.OrderItem;
import com.shop.shop.entity.OrderStatus;
import com.shop.shop.entity.User;
import com.shop.shop.service.EmailService;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.mail.javamail.JavaMailSender;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Properties;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class EmailServiceTest {

    private JavaMailSender mailSender;
    private EmailService emailService;
    private Order sampleOrder;

    @BeforeEach
    void setUp() {
        mailSender = Mockito.mock(JavaMailSender.class);
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService = new EmailService(mailSender);
        emailService.setAdminEmail("krishnansopi@gmail.com");
        emailService.setMailEnabled(true);
        emailService.setMailUsername("shopowner@gmail.com");

        User customer = new User();
        customer.setId(10L);
        customer.setFullName("Akash Ram");
        customer.setEmail("customer@test.com");
        customer.setPhone("+91 98765 43210");

        sampleOrder = new Order(
                "RJ-260906-5555",
                customer,
                new BigDecimal("45000.00"),
                OrderStatus.PLACED,
                "Akash Ram",
                "+91 98765 43210",
                "123 Main Street, Chennai - 600001",
                "Cash on Delivery",
                "Please call before delivery"
        );
        sampleOrder.setId(100L);
        sampleOrder.setCreatedAt(LocalDateTime.now());

        OrderItem item = new OrderItem(
                sampleOrder,
                null,
                "22K Royal Gold Necklace",
                new BigDecimal("45000.00"),
                1,
                new BigDecimal("45000.00"),
                "/uploads/necklace.jpg"
        );
        sampleOrder.setItems(Collections.singletonList(item));
    }

    @Test
    void testSendNewOrderAdminAlert_Success() {
        emailService.sendNewOrderAdminAlert(sampleOrder);

        ArgumentCaptor<MimeMessage> messageCaptor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender, times(1)).send(messageCaptor.capture());

        MimeMessage sentMessage = messageCaptor.getValue();
        Assertions.assertNotNull(sentMessage);
    }

    @Test
    void testSendOrderConfirmationCustomer_Success() {
        emailService.sendOrderConfirmationCustomer(sampleOrder);

        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    void testMailSkippedWhenUsernameBlank() {
        emailService.setMailUsername(""); // Blank username
        emailService.sendNewOrderAdminAlert(sampleOrder);

        // Should not call mailSender
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void testMailSkippedWhenDisabled() {
        emailService.setMailEnabled(false);
        emailService.sendNewOrderAdminAlert(sampleOrder);

        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void testSendHandlesExceptionGracefully() {
        doThrow(new RuntimeException("SMTP Connection Refused")).when(mailSender).send(any(MimeMessage.class));

        // Should catch exception without throwing out of the method
        Assertions.assertDoesNotThrow(() -> emailService.sendNewOrderAdminAlert(sampleOrder));
    }
}
