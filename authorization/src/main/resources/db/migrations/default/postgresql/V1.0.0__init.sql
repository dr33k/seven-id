SET SCHEMA 'public';

CREATE TABLE auth_account (
        id UUID PRIMARY KEY,
        first_name VARCHAR(255) NOT NULL,
        last_name VARCHAR(255)  NOT NULL,
        other_name VARCHAR(255),
        phone_no VARCHAR(20) NOT NULL,
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

CREATE TABLE auth_application (
        id UUID PRIMARY KEY,
        name VARCHAR(255) NOT NULL,
        description VARCHAR(255) NOT NULL,
        schema_name VARCHAR(255) NOT NULL,
        date_created TIMESTAMP WITH TIME ZONE NOT NULL,
        date_updated TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
        created_by VARCHAR(255) NOT NULL,
        updated_by VARCHAR(255) NOT NULL,
        UNIQUE(name)
);

DO $$
DECLARE
    root_account_id UUID;
    root_account_email VARCHAR(255);
    root_role_id UUID;
    authorization_domain_id UUID;
    super_create_id UUID;
    super_update_id UUID;
    super_read_id UUID;
    super_delete_id UUID;
BEGIN
    root_account_id := gen_random_uuid();
    root_account_email := 'root@seven.com';
    root_role_id := gen_random_uuid();
    authorization_domain_id = gen_random_uuid();
    super_create_id = gen_random_uuid();
    super_update_id = gen_random_uuid();
    super_read_id = gen_random_uuid();
    super_delete_id = gen_random_uuid();

-- Create Superuser
INSERT INTO auth_account(id, first_name, last_name, email, phone_no, status, date_created, date_updated, created_by, updated_by, password, is_deleted)
VALUES (root_account_id, 'super', '', root_account_email, '+00000000000', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, root_account_email, root_account_email, '$2a$12$7BtwA4ZTgyVGM2F7SiCZaeAsM4VD1eP52zrSEdkaP3S60IxCgaXIC', false);

INSERT INTO auth_role(id, name, description, date_created, date_updated, created_by, updated_by)
VALUES(root_role_id, 'ROOT', 'Superuser administrator role', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, root_account_email, root_account_email);

INSERT INTO auth_assignment(account_email, role_id, date_created, date_updated, created_by, updated_by)
VALUES(root_account_email, root_role_id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, root_account_email, root_account_email);

INSERT INTO auth_domain(id, name, description, date_created, date_updated, created_by, updated_by)
VALUES(authorization_domain_id, 'authorization', 'This represents the authorization application domain', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, root_account_email, root_account_email);

INSERT INTO auth_permission(id, name, description, type, domain_id, date_created, date_updated, created_by, updated_by)
VALUES
(super_create_id, 'super_create', 'This represents the CREATE permission that overrides all others', 'CREATE', authorization_domain_id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, root_account_email, root_account_email),
(super_read_id, 'super_read', 'This represents the READ permission that overrides all others', 'READ', authorization_domain_id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, root_account_email, root_account_email),
(super_update_id, 'super_update', 'This represents the UPDATE permission that overrides all others', 'UPDATE', authorization_domain_id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, root_account_email, root_account_email),
(super_delete_id, 'super_delete', 'This represents the DELETE permission that overrides all others', 'DELETE', authorization_domain_id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, root_account_email, root_account_email)
;

INSERT INTO auth_grant(id, role_id, permission_id, description, date_created, date_updated, created_by, updated_by)
VALUES
(gen_random_uuid(), root_role_id, super_create_id, 'Grants the super_create role to the superuser', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, root_account_email, root_account_email),
(gen_random_uuid(), root_role_id, super_update_id, 'Grants the super_update role to the superuser', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, root_account_email, root_account_email),
(gen_random_uuid(), root_role_id, super_read_id, 'Grants the super_read role to the superuser', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, root_account_email, root_account_email),
(gen_random_uuid(), root_role_id, super_delete_id, 'Grants the super_delete role to the superuser', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, root_account_email, root_account_email)
;
--Create permissions for the Authorization domain
------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
INSERT INTO auth_permission(id, name, description, type, domain_id, date_created, date_updated, created_by, updated_by)
VALUES
(gen_random_uuid(), 'create_account', 'Grants holder permission to create accounts', 'CREATE', authorization_domain_id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, root_account_email, root_account_email),
(gen_random_uuid(), 'update_account', 'Grants holder permission to update accounts', 'UPDATE', authorization_domain_id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, root_account_email, root_account_email),
(gen_random_uuid(), 'read_account', 'Grants holder permission to read accounts', 'READ', authorization_domain_id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, root_account_email, root_account_email),
(gen_random_uuid(), 'delete_account', 'Grants holder permission to delete accounts', 'DELETE', authorization_domain_id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, root_account_email, root_account_email),

(gen_random_uuid(), 'create_role', 'Grants holder permission to create roles', 'CREATE', authorization_domain_id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, root_account_email, root_account_email),
(gen_random_uuid(), 'update_role', 'Grants holder permission to update roles', 'UPDATE', authorization_domain_id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, root_account_email, root_account_email),
(gen_random_uuid(), 'read_role', 'Grants holder permission to read roles', 'READ', authorization_domain_id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, root_account_email, root_account_email),
(gen_random_uuid(), 'delete_role', 'Grants holder permission to delete roles', 'DELETE', authorization_domain_id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, root_account_email, root_account_email),

(gen_random_uuid(), 'create_domain', 'Grants holder permission to create domains', 'CREATE', authorization_domain_id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, root_account_email, root_account_email),
(gen_random_uuid(), 'update_domain', 'Grants holder permission to update domains', 'UPDATE', authorization_domain_id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, root_account_email, root_account_email),
(gen_random_uuid(), 'read_domain', 'Grants holder permission to read domains', 'READ', authorization_domain_id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, root_account_email, root_account_email),
(gen_random_uuid(), 'delete_domain', 'Grants holder permission to delete domains', 'DELETE', authorization_domain_id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, root_account_email, root_account_email),

(gen_random_uuid(), 'create_permission', 'Grants holder permission to create permissions', 'CREATE', authorization_domain_id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, root_account_email, root_account_email),
(gen_random_uuid(), 'update_permission', 'Grants holder permission to update permissions', 'UPDATE', authorization_domain_id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, root_account_email, root_account_email),
(gen_random_uuid(), 'read_permission', 'Grants holder permission to read permissions', 'READ', authorization_domain_id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, root_account_email, root_account_email),
(gen_random_uuid(), 'delete_permission', 'Grants holder permission to delete permissions', 'DELETE', authorization_domain_id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, root_account_email, root_account_email),

(gen_random_uuid(), 'create_assignment', 'Grants holder permission to create assignments', 'CREATE', authorization_domain_id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, root_account_email, root_account_email),
(gen_random_uuid(), 'update_assignment', 'Grants holder permission to update assignments', 'UPDATE', authorization_domain_id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, root_account_email, root_account_email),
(gen_random_uuid(), 'read_assignment', 'Grants holder permission to read assignments', 'READ', authorization_domain_id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, root_account_email, root_account_email),
(gen_random_uuid(), 'delete_assignment', 'Grants holder permission to delete assignments', 'DELETE', authorization_domain_id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, root_account_email, root_account_email),

(gen_random_uuid(), 'create_grant', 'Grants holder permission to create grants', 'CREATE', authorization_domain_id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, root_account_email, root_account_email),
(gen_random_uuid(), 'update_grant', 'Grants holder permission to update grants', 'UPDATE', authorization_domain_id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, root_account_email, root_account_email),
(gen_random_uuid(), 'read_grant', 'Grants holder permission to read grants', 'READ', authorization_domain_id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, root_account_email, root_account_email),
(gen_random_uuid(), 'delete_grant', 'Grants holder permission to delete grants', 'DELETE', authorization_domain_id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, root_account_email, root_account_email)
;

INSERT INTO auth_application(id, name, description, schema_name, date_created, date_updated, created_by, updated_by)
VALUES (gen_random_uuid(), 'authorization', 'Default authorization application', 'public', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'root@seven.com', 'root@seven.com');

END
$$;