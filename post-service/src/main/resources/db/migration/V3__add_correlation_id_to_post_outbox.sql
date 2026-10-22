alter table post_outbox_events
    add column correlation_id varchar(128);

update post_outbox_events
set correlation_id = event_id
where correlation_id is null;

alter table post_outbox_events
    modify column correlation_id varchar(128) not null;
