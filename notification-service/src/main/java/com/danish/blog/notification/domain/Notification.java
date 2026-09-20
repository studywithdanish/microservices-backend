package com.danish.blog.notification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Long id;

    @Column(name = "event_id", nullable = false, unique = true, length = 36)
    private String eventId;

    @Column(name = "recipient_user_id", nullable = false)
    private Integer recipientUserId;

    @Column(name = "post_id", nullable = false)
    private Integer postId;

    @Column(nullable = false, length = 50)
    private String type;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(name = "is_read", nullable = false)
    private boolean read;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Notification() {
    }

    public Notification(
            String eventId,
            Integer recipientUserId,
            Integer postId,
            String type,
            String title,
            String message,
            Instant createdAt
    ) {
        this.eventId = eventId;
        this.recipientUserId = recipientUserId;
        this.postId = postId;
        this.type = type;
        this.title = title;
        this.message = message;
        this.createdAt = createdAt;
    }

    public void markRead() {
        this.read = true;
    }

    public Long getId() {
        return id;
    }

    public String getEventId() {
        return eventId;
    }

    public Integer getRecipientUserId() {
        return recipientUserId;
    }

    public Integer getPostId() {
        return postId;
    }

    public String getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public boolean isRead() {
        return read;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
