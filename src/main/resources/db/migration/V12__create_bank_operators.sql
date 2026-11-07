CREATE TABLE bank_operators
(
    id         UUID                     NOT NULL,
    user_id    UUID                     NOT NULL,
    status     VARCHAR(20)              NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version    BIGINT                   NOT NULL DEFAULT 0,

    CONSTRAINT pk_bank_operators
        PRIMARY KEY (id),

    CONSTRAINT uk_bank_operators_user_id
        UNIQUE (user_id),

    CONSTRAINT fk_bank_operators_user
        FOREIGN KEY (user_id)
            REFERENCES users (id),

    CONSTRAINT ck_bank_operators_status
        CHECK (status IN ('ACTIVE', 'SUSPENDED'))
);
