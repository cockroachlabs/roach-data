create table customer
(
    id         uuid        not null default gen_random_uuid(),
    first_name varchar(45),
    last_name  varchar(45),
    user_name  varchar(15) not null,

    primary key (id)
);

create table orders
(
    id          uuid           not null default gen_random_uuid(),
    customer_id uuid           not null,
    total_price numeric(19, 2) not null,
    tags        string         null,

    primary key (id)
);

create table product
(
    id          uuid           not null default gen_random_uuid(),
    inventory   int            not null,
    name        string         not null,
    description jsonb          null,
    price       numeric(19, 2) not null,
    sku         string         not null,

    primary key (id)
);

create table order_item
(
    order_id   uuid           not null,
    product_id uuid           not null,
    quantity   int            not null,
    unit_price numeric(19, 2) not null,

    primary key (order_id, product_id)
);

alter table product
    add constraint check_product_positive_inventory check (product.inventory >= 0);

alter table if exists customer
    add constraint uc_user_name unique (user_name);

alter table if exists product
    add constraint uc_product_sku unique (sku);

alter table if exists order_item
    add constraint fk_order_item_ref_product
        foreign key (product_id)
            references product;

alter table if exists order_item
    add constraint fk_order_item_ref_order
        foreign key (order_id)
            references orders;

alter table if exists orders
    add constraint fk_order_ref_customer
        foreign key (customer_id)
            references customer;

create index fk_order_item_ref_product_idx on order_item (product_id);

create index fk_order_ref_customer_idx on orders (customer_id);