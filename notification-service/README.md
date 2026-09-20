# Notification Service

Consumes versioned `PostPublished` events from Kafka and creates durable, user-scoped publication notifications.

- Port: `9095`
- Topic: `blog.posts.published.v1`
- Consumer group: `notification-service-v1`
- Dead-letter topic: `blog.posts.published.v1.DLT`
- Database: `blog_notifications`

The consumer is idempotent by event ID. Failed records are retried twice and then published to the dead-letter topic. Notification reads require the Identity Service JWT and are routed publicly through `/api/notifications`.
