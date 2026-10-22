ALTER TABLE post_outbox_events
    ADD COLUMN traceparent VARCHAR(55) NULL AFTER correlation_id,
    ADD COLUMN tracestate VARCHAR(512) NULL AFTER traceparent;
