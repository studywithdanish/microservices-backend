package com.danish.blog.notification.api;

import com.danish.blog.notification.security.CurrentUserProvider;
import com.danish.blog.notification.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final CurrentUserProvider currentUserProvider;

    public NotificationController(
            NotificationService notificationService,
            CurrentUserProvider currentUserProvider
    ) {
        this.notificationService = notificationService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping
    public ResponseEntity<List<NotificationDto>> getMine(Authentication authentication) {
        return ResponseEntity.ok(notificationService.getForUser(
                currentUserProvider.requireCurrentUser(authentication).id()
        ));
    }

    @PutMapping("/{notificationId}/read")
    public ResponseEntity<NotificationDto> markRead(
            @PathVariable Long notificationId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(notificationService.markRead(
                notificationId,
                currentUserProvider.requireCurrentUser(authentication).id()
        ));
    }
}
