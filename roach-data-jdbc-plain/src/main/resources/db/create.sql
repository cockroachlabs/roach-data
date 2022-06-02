drop table if exists account;

create table account
(
    id      int            not null primary key,
    balance numeric(19, 2) not null,
    name    varchar(128)   not null,
    updated timestamptz    not null default clock_timestamp()
);

alter table account
    add constraint check_account_positive_balance check (balance  >= 0);

insert into account (id, balance, name)
select i,
       5000.00,
       concat('customer:', (i::varchar))
from generate_series(1, 100) as i;
