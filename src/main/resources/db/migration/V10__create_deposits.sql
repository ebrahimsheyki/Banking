CREATE TABLE deposits
(
    id                   UUID                     NOT NULL,
    account_id           UUID                     NOT NULL,
    initiated_by_user_id UUID                     NOT NULL,
    idempotency_key      VARCHAR(100)             NOT NULL,
    amount               NUMERIC(19, 4)           NOT NULL,
    currency_code        VARCHAR(3)               NOT NULL,
    status               VARCHAR(20)              NOT NULL,
    created_at           TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT pk_deposits
        PRIMARY KEY (id),

    CONSTRAINT fk_deposits_account
        FOREIGN KEY (account_id)
            REFERENCES accounts (id),

    CONSTRAINT fk_deposits_initiator
        FOREIGN KEY (initiated_by_user_id)
            REFERENCES users (id),

    CONSTRAINT uk_deposits_initiator_idempotency
        UNIQUE (initiated_by_user_id, idempotency_key),

    CONSTRAINT ck_deposits_amount_positive
        CHECK (amount > 0),

    CONSTRAINT ck_deposits_status
        CHECK (status = 'COMPLETED'),

    CONSTRAINT ck_deposits_idempotency_key
        CHECK (LENGTH(TRIM(idempotency_key)) > 0)
);

CREATE INDEX idx_deposits_account_created
    ON deposits (account_id, created_at);
