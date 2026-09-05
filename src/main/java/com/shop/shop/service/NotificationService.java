package com.shop.shop.service;

import com.shop.shop.dto.NotificationDTO;
import com.shop.shop.entity.Notification;
import com.shop.shop.entity.Order;
import com.shop.shop.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final EmailService emailService;

    public NotificationService(NotificationRepository notificationRepository, EmailService emailService) {
        this.notificationRepository = notificationRepository;
        this.emailService = emailService;
    }

    @Transactional
    public Notification createOrderNotification(Order order) {
        String title = "New Order " + order.getOrderNumber() + " Received";
        String message = "New order #" + order.getOrderNumber() + " placed by " + 
                         (order.getRecipientName() != null ? order.getRecipientName() : order.getUser().getFullName()) + 
                         ". Total: ₹" + order.getTotalAmount();

        Notification notification = new Notification(
                title,
                message,
                order.getId(),
                order.getOrderNumber(),
                order.getTotalAmount(),
                order.getRecipientName() != null ? order.getRecipientName() : order.getUser().getFullName()
        );

        Notification saved = notificationRepository.save(notification);

        // Dispatch Email notification asynchronously
        try {
            emailService.sendNewOrderAdminAlert(order);
            emailService.sendOrderConfirmationCustomer(order);
        } catch (Exception e) {
            // Never break transaction if email dispatch throws
        }

        return saved;
    }

    @Transactional(readOnly = true)
    public List<NotificationDTO> getAllNotifications() {
        return notificationRepository.findTop20ByOrderByCreatedAtDesc().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public long getUnreadCount() {
        return notificationRepository.countByIsReadFalse();
    }

    @Transactional
    public void markAsRead(Long id) {
        notificationRepository.findById(id).ifPresent(n -> {
            n.setIsRead(true);
            notificationRepository.save(n);
        });
    }

    @Transactional
    public void markAllAsRead() {
        List<Notification> unread = notificationRepository.findByIsReadFalseOrderByCreatedAtDesc();
        for (Notification n : unread) {
            n.setIsRead(true);
        }
        notificationRepository.saveAll(unread);
    }

    public NotificationDTO mapToDTO(Notification n) {
        return new NotificationDTO(
                n.getId(),
                n.getTitle(),
                n.getMessage(),
                n.getOrderId(),
                n.getOrderNumber(),
                n.getAmount(),
                n.getCustomerName(),
                n.getIsRead(),
                n.getCreatedAt()
        );
    }
}
