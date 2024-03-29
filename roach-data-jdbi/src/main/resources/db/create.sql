drop table if exists account;

create table if not exists account
(
    id      int            not null primary key,
    balance numeric(19, 2) not null,
    name    varchar(128)   not null,
    updated timestamptz    not null default clock_timestamp()
);

alter table account
    add constraint if not exists check_account_positive_balance check (balance  >= 0);

truncate table account;

insert into account (id, balance, name)
select i,
       5000.00,
       concat('customer:', (i::varchar))
from generate_series(1, 100) as i;
