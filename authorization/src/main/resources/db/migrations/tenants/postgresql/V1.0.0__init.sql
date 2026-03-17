CREATE TABLE auth_account (
        id UUID PRIMARY KEY,
        first_name VARCHAR(255) NOT NULL,
        last_name VARCHAR(255)  NOT NULL,
        other_name VARCHAR(255),
        phone_no VARCHAR(20),
        phone_no_alt VARCHAR(20),
        email VARCHAR(255) NOT NULL UNIQUE,
        email_alt VARCHAR(255) UNIQUE,
        status VARCHAR(20) NOT NULL,
        password VARCHAR(512) NOT NULL,
        dob DATE,
        auth_provider VARCHAR(15) NOT NULL DEFAULT 'in_house',
        date_created TIMESTAMP WITH TIME ZONE NOT NULL,
        date_updated TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
        created_by VARCHAR(255) REFERENCES auth_account(email) ON DELETE SET NULL ON UPDATE CASCADE,
        updated_by VARCHAR(255) REFERENCES auth_account(email) ON DELETE SET NULL ON UPDATE CASCADE,
        is_deleted BOOL NOT NULL,
        date_deleted TIMESTAMP WITH TIME ZONE
        );
CREATE INDEX auth_account_email_idx ON auth_account(email);

CREATE TABLE auth_role(
    id UUID PRIMARY KEY,
    name VARCHAR(100) UNIQUE NOT NULL,
    description VARCHAR(255),
    date_created TIMESTAMP WITH TIME ZONE NOT NULL,
    date_updated TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255) REFERENCES auth_account(email) ON DELETE SET NULL ON UPDATE CASCADE,
    updated_by VARCHAR(255) REFERENCES auth_account(email) ON DELETE SET NULL ON UPDATE CASCADE
);

CREATE TABLE auth_domain(
    id UUID PRIMARY KEY,
    name VARCHAR(100) UNIQUE NOT NULL,
    description VARCHAR(255),
    date_created TIMESTAMP WITH TIME ZONE NOT NULL,
    date_updated TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255) REFERENCES auth_account(email) ON DELETE SET NULL ON UPDATE CASCADE,
    updated_by VARCHAR(255) REFERENCES auth_account(email) ON DELETE SET NULL ON UPDATE CASCADE
);
CREATE INDEX auth_domain_name_idx ON auth_domain(name);

CREATE TABLE auth_permission(
    id UUID PRIMARY KEY,
    name VARCHAR(100) UNIQUE NOT NULL,
    description VARCHAR(255),
    type VARCHAR(20) NOT NULL,
    domain_id UUID NOT NULL REFERENCES auth_domain(id) ON DELETE CASCADE,
    date_created TIMESTAMP WITH TIME ZONE NOT NULL,
    date_updated TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255) REFERENCES auth_account(email) ON DELETE SET NULL ON UPDATE CASCADE,
    updated_by VARCHAR(255) REFERENCES auth_account(email) ON DELETE SET NULL ON UPDATE CASCADE
);

CREATE TABLE auth_grant(
    id UUID PRIMARY KEY,
    role_id UUID NOT NULL REFERENCES auth_role(id) ON DELETE CASCADE,
    permission_id UUID NOT NULL REFERENCES auth_permission(id) ON DELETE CASCADE,
    description VARCHAR(255),
    date_created TIMESTAMP WITH TIME ZONE NOT NULL,
    date_updated TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255) REFERENCES auth_account(email) ON DELETE SET NULL ON UPDATE CASCADE,
    updated_by VARCHAR(255) REFERENCES auth_account(email) ON DELETE SET NULL ON UPDATE CASCADE
);

CREATE TABLE auth_assignment(
    account_email VARCHAR(255) NOT NULL REFERENCES auth_account(email) ON DELETE CASCADE,
    role_id UUID NOT NULL REFERENCES auth_role(id) ON DELETE CASCADE,
    date_created TIMESTAMP WITH TIME ZONE NOT NULL,
    date_updated TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255) REFERENCES auth_account(email) ON DELETE SET NULL ON UPDATE CASCADE,
    updated_by VARCHAR(255) REFERENCES auth_account(email) ON DELETE SET NULL ON UPDATE CASCADE,
    PRIMARY KEY(account_email, role_id)
);

DO $$
DECLARE
    admin_account_id UUID;
    root_account_email VARCHAR(255);
    admin_account_email VARCHAR(255);
    admin_role_id UUID;
    global_domain_id UUID;
    authorization_domain_id UUID;
    elev_create_id UUID;
    elev_update_id UUID;
    elev_read_id UUID;
    elev_delete_id UUID;
    elev_create_auth_id UUID;
    elev_update_auth_id UUID;
    elev_read_auth_id UUID;
    elev_delete_auth_id UUID;
BEGIN
    admin_account_id := gen_random_uuid();
    root_account_email := 'root@seven.com';
    admin_account_email := current_schema||'@seven.com';
    admin_role_id := gen_random_uuid();
    global_domain_id := gen_random_uuid();
    authorization_domain_id := gen_random_uuid(); -- Represents the 'authorization' domain in the public schema
    elev_create_id = gen_random_uuid();
    elev_update_id = gen_random_uuid();
    elev_read_id = gen_random_uuid();
    elev_delete_id = gen_random_uuid();
    elev_create_auth_id = gen_random_uuid();
    elev_update_auth_id = gen_random_uuid();
    elev_read_auth_id = gen_random_uuid();
    elev_delete_auth_id = gen_random_uuid();

-- Create an elevated user and a root user alias
INSERT INTO auth_account(id, first_name, last_name, email, status, date_created, date_updated, created_by, updated_by, password, is_deleted)
VALUES
((SELECT id FROM public.auth_account WHERE email = root_account_email), 'super', '', root_account_email, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, root_account_email, root_account_email, '_', false),
(admin_account_id, 'admin', '', admin_account_email, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, admin_account_email, admin_account_email, '$2a$12$7BtwA4ZTgyVGM2F7SiCZaeAsM4VD1eP52zrSEdkaP3S60IxCgaXIC', false);

INSERT INTO auth_role(id, name, description, date_created, date_updated, created_by, updated_by)
VALUES(admin_role_id, 'ADMIN', 'Administrator role', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, admin_account_email, admin_account_email);

INSERT INTO auth_assignment(account_email, role_id, date_created, date_updated, created_by, updated_by)
VALUES(admin_account_email, admin_role_id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, admin_account_email, admin_account_email);

INSERT INTO auth_domain(id, name, description, date_created, date_updated, created_by, updated_by)
VALUES
(global_domain_id, current_schema, 'This is an umbrella domain for all the future domains in the '||current_schema||' schema', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, admin_account_email, admin_account_email),
(authorization_domain_id, 'authorization', 'Represents the ''authorization'' domain in the public schema', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, admin_account_email, admin_account_email)
;

INSERT INTO auth_permission(id, name, description, type, domain_id, date_created, date_updated, created_by, updated_by)
VALUES
(elev_create_id, 'elev_create_'||current_schema, 'This represents the CREATE permission that overrides all others for the '||current_schema||' domain', 'CREATE', global_domain_id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, admin_account_email, admin_account_email),
(elev_read_id, 'elev_read_'||current_schema, 'This represents the READ permission that overrides all others for the '||current_schema||' domain', 'READ', global_domain_id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, admin_account_email, admin_account_email),
(elev_update_id, 'elev_update_'||current_schema, 'This represents the UPDATE permission that overrides all others for the '||current_schema||' domain', 'UPDATE', global_domain_id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, admin_account_email, admin_account_email),
(elev_delete_id, 'elev_delete_'||current_schema, 'This represents the DELETE permission that overrides all others for the '||current_schema||' domain', 'DELETE', global_domain_id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, admin_account_email, admin_account_email),
-- Elevated permissions for the 'authorization' domain in the public schema
(elev_create_auth_id, 'elev_create_authorization', 'This represents the elevated CREATE permission for the ''authorization'' domain', 'CREATE', authorization_domain_id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, admin_account_email, admin_account_email),
(elev_read_auth_id, 'elev_read_authorization', 'This represents the elevated READ permission for the ''authorization'' domain', 'READ', authorization_domain_id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, admin_account_email, admin_account_email),
(elev_update_auth_id, 'elev_update_authorization', 'This represents the elevated UPDATE permission for the ''authorization'' domain', 'UPDATE', authorization_domain_id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, admin_account_email, admin_account_email),
(elev_delete_auth_id, 'elev_delete_authorization', 'This represents the elevated DELETE permission for the ''authorization'' domain', 'DELETE', authorization_domain_id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, admin_account_email, admin_account_email)
;

INSERT INTO auth_grant(id, role_id, permission_id, description, date_created, date_updated, created_by, updated_by)
VALUES
(gen_random_uuid(), admin_role_id, elev_create_id, 'Grants the elev_create permission to the admin', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, admin_account_email, admin_account_email),
(gen_random_uuid(), admin_role_id, elev_update_id, 'Grants the elev_update permission to the admin', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, admin_account_email, admin_account_email),
(gen_random_uuid(), admin_role_id, elev_read_id, 'Grants the elev_read permission to the admin', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, admin_account_email, admin_account_email),
(gen_random_uuid(), admin_role_id, elev_delete_id, 'Grants the elev_delete permission to the admin', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, admin_account_email, admin_account_email),
-- Grants for the elevated permissions for the 'authorization' domain in the public schema
(gen_random_uuid(), admin_role_id, elev_create_auth_id, 'Grants the elev_create_authorization permission to the admin', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, admin_account_email, admin_account_email),
(gen_random_uuid(), admin_role_id, elev_update_auth_id, 'Grants the elev_update_authorization permission to the admin', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, admin_account_email, admin_account_email),
(gen_random_uuid(), admin_role_id, elev_read_auth_id, 'Grants the elev_read_authorization permission to the admin', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, admin_account_email, admin_account_email),
(gen_random_uuid(), admin_role_id, elev_delete_auth_id, 'Grants the elev_delete_authorization permission to the admin', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, admin_account_email, admin_account_email)
;
END
$$;