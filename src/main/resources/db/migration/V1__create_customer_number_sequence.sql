CREATE SEQUENCE customer_number_seq
    START WITH 1
    INCREMENT BY 1;

CREATE TABLE customers
(
    id              UUID                     NOT NULL,
    customer_number VARCHAR(20)              NOT NULL,
    first_name      VARCHAR(100)             NOT NULL,
    last_name       VARCHAR(100)             NOT NULL,
    email           VARCHAR(255)             NOT NULL,
    status          VARCHAR(20)              NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT pk_customers PRIMARY KEY (id),
    CONSTRAINT uk_customers_customer_number UNIQUE (customer_number),
    CONSTRAINT uk_customers_email UNIQUE (email)
);