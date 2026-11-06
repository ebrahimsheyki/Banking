CREATE SEQUENCE account_number_seq
    START WITH 1
    INCREMENT BY 1;

CREATE TABLE accounts
(
    id             UUID                     NOT NULL,
    account_number VARCHAR(20)              NOT NULL,
    customer_id    UUID                     NOT NULL,
    account_type   VARCHAR(20)              NOT NULL,
    currency_code  VARCHAR(3)               NOT NULL,
    balance        NUMERIC(19, 4)           NOT NULL DEFAULT 0.0000,
    status         VARCHAR(20)              NOT NULL,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    version        BIGINT                   NOT NULL DEFAULT 0,

    CONSTRAINT pk_accounts
        PRIMARY KEY (id),

    CONSTRAINT uk_accounts_account_number
        UNIQUE (account_number),

    CONSTRAINT fk_accounts_customer
        FOREIGN KEY (customer_id)
            REFERENCES customers (id),

    CONSTRAINT ck_accounts_balance_nonnegative
        CHECK (balance >= 0),

    CONSTRAINT ck_accounts_type
        CHECK (account_type IN ('CURRENT', 'SAVINGS')),

    CONSTRAINT ck_accounts_status
        CHECK (status IN ('ACTIVE', 'BLOCKED', 'CLOSED'))
);

CREATE INDEX idx_accounts_customer_id
    ON accounts (customer_id);