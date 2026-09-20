create table if not exists notifications (
    notification_id bigint auto_increment,
    event_id varchar(36) not null,
    recipient_user_id int not null,
    post_id int not null,
    type varchar(50) not null,
    title varchar(150) not null,
    message varchar(500) not null,
    is_read boolean not null default false,
    created_at datetime(6) not null,
    primary key (notification_id),
    constraint uk_notifications_event_id unique (event_id)
);

create index idx_notifications_recipient_created
    on notifications (recipient_user_id, created_at);
