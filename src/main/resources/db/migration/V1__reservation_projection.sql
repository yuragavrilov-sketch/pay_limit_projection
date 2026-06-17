create table reservation_event (
    event_id        uuid primary key,
    reservation_id  uuid not null,
    operation_id    text not null,
    event_type      text not null,
    state           text not null,
    occurred_at     timestamptz not null,
    merchant_id     text not null,
    operation_type  text not null,
    direction       text not null,
    amount          numeric(38, 2) not null,
    currency        text not null,
    reasons         jsonb not null default '[]'::jsonb,
    matched_rules   jsonb not null default '[]'::jsonb,
    payload_json    jsonb not null,
    received_at     timestamptz not null,
    kafka_topic     text not null,
    kafka_partition integer not null,
    kafka_offset    bigint not null
);

create index ix_reservation_event_reservation on reservation_event (reservation_id, occurred_at);
create index ix_reservation_event_operation on reservation_event (operation_id);
create index ix_reservation_event_merchant on reservation_event (merchant_id, occurred_at);

create table reservation_state (
    reservation_id    uuid primary key,
    operation_id      text not null,
    state             text not null,
    merchant_id       text not null,
    operation_type    text not null,
    direction         text not null,
    amount            numeric(38, 2) not null,
    currency          text not null,
    held_at           timestamptz,
    last_event_id     uuid not null,
    last_event_type   text not null,
    last_occurred_at  timestamptz not null,
    stale_after       timestamptz,
    updated_at        timestamptz not null
);

create index ix_reservation_state_merchant on reservation_state (merchant_id, last_occurred_at);
