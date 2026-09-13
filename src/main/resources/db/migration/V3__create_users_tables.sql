CREATE TABLE users
(
    id            UUID                     NOT NULL,
    username      VARCHAR(100)             NOT NULL,
    password_hash VARCHAR(255)             NOT NULL,
    enabled       BOOLEAN                  NOT NULL,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uk_users_username UNIQUE (username)
);


CREATE TABLE user_roles
(
    user_id UUID        NOT NULL,
    role    VARCHAR(30) NOT NULL,

    CONSTRAINT pk_user_roles PRIMARY KEY (user_id, role),
    CONSTRAINT fk_user_roles_user
        FOREIGN KEY (user_id)
            REFERENCES users (id)
            ON DELETE CASCADE
);