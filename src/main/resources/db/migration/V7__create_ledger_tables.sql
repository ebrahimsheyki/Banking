CREATE TABLE ledger_transactions
(
    id               UUID                     NOT NULL,
    transaction_type VARCHAR(30)              NOT NULL,
    source_reference UUID                     NOT NULL,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT pk_ledger_transactions
        PRIMARY KEY (id),

    CONSTRAINT uk_ledger_transactions_source
        UNIQUE (transaction_type, source_reference),

    CONSTRAINT ck_ledger_transactions_type
        CHECK (
            transaction_type IN (
                                 'INTERNAL_TRANSFER',
                                 'DEPOSIT',
                                 'WITHDRAWAL'
                )
            )
);

CREATE TABLE ledger_entries
(
    id                    UUID                     NOT NULL,
    ledger_transaction_id UUID                     NOT NULL,
    account_id            UUID,
    system_account_code   VARCHAR(64),
    entry_side            VARCHAR(10)              NOT NULL,
    amount                NUMERIC(19, 4)           NOT NULL,
    currency_code         VARCHAR(3)               NOT NULL,
    created_at            TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT pk_ledger_entries
        PRIMARY KEY (id),

    CONSTRAINT fk_ledger_entries_transaction
        FOREIGN KEY (ledger_transaction_id)
            REFERENCES ledger_transactions (id),

    CONSTRAINT fk_ledger_entries_account
        FOREIGN KEY (account_id)
            REFERENCES accounts (id),

    CONSTRAINT ck_ledger_entries_account_reference
        CHECK (
            (account_id IS NOT NULL AND system_account_code IS NULL)
                OR
            (account_id IS NULL AND system_account_code IS NOT NULL)
            ),

    CONSTRAINT ck_ledger_entries_system_account_code
        CHECK (
            system_account_code IS NULL
                OR system_account_code IN ('BANK_CASH')
            ),

    CONSTRAINT ck_ledger_entries_side
        CHECK (entry_side IN ('DEBIT', 'CREDIT')),

    CONSTRAINT ck_ledger_entries_amount_positive
        CHECK (amount > 0)
);

CREATE INDEX idx_ledger_entries_account_created
    ON ledger_entries (account_id, created_at);

CREATE INDEX idx_ledger_entries_transaction
    ON ledger_entries (ledger_transaction_id);
