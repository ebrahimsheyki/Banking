CREATE TABLE roles
(
    id          UUID         NOT NULL,
    code        VARCHAR(30)  NOT NULL,
    description VARCHAR(255) NOT NULL,

    CONSTRAINT pk_roles PRIMARY KEY (id),
    CONSTRAINT uk_roles_code UNIQUE (code)
);


INSERT INTO roles (id, code, description)
VALUES
    ('00000000-0000-0000-0000-000000000001',
     'CUSTOMER',
     'Banking customer'),

    ('00000000-0000-0000-0000-000000000002',
     'EMPLOYEE',
     'Bank employee'),

    ('00000000-0000-0000-0000-000000000003',
     'ADMIN',
     'Bank administrator');


ALTER TABLE user_roles
    ADD COLUMN role_id UUID;

UPDATE user_roles ur
SET role_id = (
    SELECT r.id
    FROM roles r
    WHERE r.code = ur.role
);

ALTER TABLE user_roles
    ALTER COLUMN role_id SET NOT NULL;


ALTER TABLE user_roles
DROP CONSTRAINT pk_user_roles;


ALTER TABLE user_roles
DROP CONSTRAINT fk_user_roles_user;


ALTER TABLE user_roles
    ADD CONSTRAINT pk_user_roles
        PRIMARY KEY (user_id, role_id);


ALTER TABLE user_roles
    ADD CONSTRAINT fk_user_roles_user
        FOREIGN KEY (user_id)
            REFERENCES users (id)
            ON DELETE CASCADE;


ALTER TABLE user_roles
    ADD CONSTRAINT fk_user_roles_role
        FOREIGN KEY (role_id)
            REFERENCES roles (id);


ALTER TABLE user_roles
DROP COLUMN role;