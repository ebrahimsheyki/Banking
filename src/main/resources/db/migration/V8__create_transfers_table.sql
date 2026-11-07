CREATE TABLE transfers
(
    id                     UUID                     NOT NULL,
    source_account_id      UUID                     NOT NULL,
    destination_account_id UUID                     NOT NULL,
    initiated_by_user_id   UUID                     NOT NULL,
    idempotency_key        VARCHAR(100)             NOT NULL,
    amount                 NUMERIC(19, 4)           NOT NULL,
    currency_code          VARCHAR(3)               NOT NULL,
    status                 VARCHAR(20)              NOT NULL,
    created_at             TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT pk_transfers
        PRIMARY KEY (id),

    CONSTRAINT uk_transfers_initiator_idempotency
        UNIQUE (initiated_by_user_id, idempotency_key),

    CONSTRAINT fk_transfers_source_account
        FOREIGN KEY (source_account_id)
            REFERENCES accounts (id),

    CONSTRAINT fk_transfers_destination_account
        FOREIGN KEY (destination_account_id)
            REFERENCES accounts (id),

    CONSTRAINT fk_transfers_initiated_by_user
        FOREIGN KEY (initiated_by_user_id)
            REFERENCES users (id),

    CONSTRAINT ck_transfers_different_accounts
        CHECK (source_account_id <> destination_account_id),

    CONSTRAINT ck_transfers_amount_positive
        CHECK (amount > 0),

    CONSTRAINT ck_transfers_status
        CHECK (status = 'COMPLETED')
);

CREATE INDEX idx_transfers_source_created
    ON transfers (source_account_id, created_at);

CREATE INDEX idx_transfers_destination_created
    ON transfers (destination_account_id, created_at);