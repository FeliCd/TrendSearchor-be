package com.fpt.swp.service;

import com.fpt.swp.model.*;
import com.fpt.swp.repository.NotificationRepository;
import com.fpt.swp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Transactional
    public Notification createNotification(Long userId, NotificationType type, String title, String message) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null || Boolean.FALSE.equals(user.getReceiveNotifications()))
            return null;

        Notification notification = Notification.builder()
                .user(user)
                .notificationType(type)
                .title(title)
                .message(message)
                .isRead(false)
                .build();

        return notificationRepository.save(notification);
    }

    @Async
    @Transactional
    public void createBulkNotifications(List<Long> userIds, NotificationType type, String title, String message) {
        for (Long userId : userIds) {
            try {
                User user = userRepository.findById(userId).orElse(null);
                if (user == null || Boolean.FALSE.equals(user.getReceiveNotifications())) {
                    continue;
                }
                Notification notification = Notification.builder()
                        .user(user)
                        .notificationType(type)
                        .title(title)
                        .message(message)
                        .isRead(false)
                        .build();
                notificationRepository.save(notification);
            } catch (Exception e) {
                log.error("Failed to create notification for user {}: {}", userId, e.getMessage());
            }
        }
    }

    @Transactional(readOnly = true)
    public Page<Notification> getUserNotifications(Long userId, int page, int size) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size));
    }

    @Transactional(readOnly = true)
    public Page<Notification> getUnreadNotifications(Long userId, int page, int size) {
        return notificationRepository.findUnreadByUserId(userId, PageRequest.of(page, size));
    }

    @Transactional(readOnly = true)
    public Page<Notification> getNotificationsByType(Long userId, NotificationType type, int page, int size) {
        return notificationRepository.findByUserIdAndType(userId, type, PageRequest.of(page, size));
    }

    @Transactional(readOnly = true)
    public Long getUnreadCount(Long userId) {
        return notificationRepository.countUnreadByUserId(userId);
    }

    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            if (n.getUser().getId().equals(userId)) {
                n.setIsRead(true);
                notificationRepository.save(n);
            }
        });
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsReadByUserId(userId);
    }

    @Transactional
    public void deleteNotification(Long id, Long userId) {
        notificationRepository.deleteByIdAndUserId(id, userId);
    }

    @Transactional
    public void deleteAllNotifications(Long userId) {
        notificationRepository.deleteAllByUserId(userId);
    }

    @Transactional(readOnly = true)
    public List<Notification> getRecentNotifications(Long userId, int limit) {
        return notificationRepository.findRecentByUserId(userId, limit);
    }
}
