drop table if exists account;

create table if not exists account
(
    id      int            not null primary key default unique_rowid(),
    balance numeric(19, 2) not null,
    name    varchar(128)   not null,
    type    varchar(25)    not null             default 'expense'
);

insert into account (id, balance, name)
select id::float8,
       5000.00::decimal,
       md5(random()::text)
from generate_series(1, 10000) as id;