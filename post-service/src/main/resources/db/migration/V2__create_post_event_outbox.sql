create table if not exists post_outbox_events (
    event_id varchar(36) not null,
    aggregate_type varchar(50) not null,
    aggregate_id int not null,
    event_type varchar(100) not null,
    payload text not null,
    occurred_at datetime(6) not null,
    published_at datetime(6),
    next_attempt_at datetime(6) not null,
    attempt_count int not null default 0,
    last_error varchar(1000),
    primary key (event_id)
);

create index idx_post_outbox_pending
    on post_outbox_events (published_at, next_attempt_at, occurred_at);
