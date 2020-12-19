drop table if exists t_account;

create table t_account
(
    id      int            not null primary key default unique_rowid(),
    balance numeric(19, 2) not null,
    name    varchar(128)   not null,
    updated timestamptz    not null default clock_timestamp()
);

insert into t_account (id, balance, name)
values (1, 1000.50, 'customer:a'),
       (2, 2000.50, 'customer:b'),
       (3, 3000.50, 'customer:c'),
       (4, 4000.50, 'customer:d'),
       (5, 5000.50, 'customer:e');

-- select *
-- from t_account AS OF SYSTEM TIME '-5s';