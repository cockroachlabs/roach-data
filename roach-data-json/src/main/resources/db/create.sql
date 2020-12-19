-- drop table if exists databasechangelog cascade;
-- drop table if exists databasechangeloglock cascade;
-- drop table if exists account cascade;
-- drop table if exists journal cascade;
-- drop table if exists transaction cascade;
-- drop table if exists transaction_item cascade;

-- Stores immutable events in json format, mapped to entity types via JPA's single table inheritance model
create table journal
(
    id         STRING PRIMARY KEY AS (payload->>'id') STORED, -- computed primary index column
    event_type varchar(15) not null,                          -- entity type discriminator
    payload    json,
    tag        varchar(64),
    updated    timestamptz default clock_timestamp(),
    INVERTED   INDEX event_payload (payload)
);

-- Event type always used
create index idx_journal_main on journal (event_type, tag);

create table account
(
    id           uuid           not null default gen_random_uuid(),
    account_type varchar(10)    not null,
    balance      numeric(19, 2) not null,
    name         varchar(64),
    updated      timestamptz             default clock_timestamp(),
    primary key (id)
);

create unique index uidx_account_name on account (name);

create table transaction
(
    id            uuid not null default gen_random_uuid(),
    booking_date  date not null,
    transfer_date date not null,
    primary key (id)
);

create table transaction_item
(
    id              uuid           not null default gen_random_uuid(),
    amount          numeric(19, 2) not null,
    running_balance numeric(19, 2) not null,
    note            varchar(128),
    account_id      uuid           not null,
    transaction_id  uuid           not null,
    primary key (id)
);

alter table if exists transaction_item
    add constraint fk_id_ref_account
        foreign key (account_id)
            references account;

alter table if exists transaction_item
    add constraint fk_id_ref_transaction
        foreign key (transaction_id)
            references transaction;

create table department
(
    id    uuid not null default gen_random_uuid(),
    users jsonb
);

create table chat_history
(
    id        uuid not null default gen_random_uuid(),
    parent_id uuid,
    messages  jsonb,

    INVERTED   INDEX message_payload (messages)
);

INSERT INTO chat_history (messages) VALUES
('[
    {"title": "Sleeping Beauties", "genres": ["Fiction", "Thriller", "Horror"], "published": false},
    {"title": "The Dictator''s Handbook", "genres": ["Law", "Politics"], "authors": ["Bruce Bueno de Mesquita", "Alastair Smith"], "published": true}
]');
