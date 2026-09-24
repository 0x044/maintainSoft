create table machine_statuses (
    id uuid not null,
    name varchar(100) not null,
    color varchar(7) not null,
    system_key varchar(64),
    built_in boolean not null default false,
    deleted boolean not null default false,
    created_at timestamp(6) with time zone not null default current_timestamp,
    updated_at timestamp(6) with time zone not null default current_timestamp,
    created_by varchar(255) not null,
    updated_by varchar(255) not null,
    version bigint not null default 0,
    primary key (id),
    constraint ck_machine_statuses_color check (color ~ '^#[0-9A-Fa-f]{6}$'),
    constraint ck_machine_statuses_system_key check (system_key is null or built_in = true)
);

create unique index uq_machine_statuses_system_key
    on machine_statuses (system_key)
    where system_key is not null;

insert into machine_statuses (
    id, name, color, system_key, built_in, deleted,
    created_at, updated_at, created_by, updated_by, version
) values
    ('10000000-0000-0000-0000-000000000001', 'Operational', '#22C55E', 'OPERATIONAL', true, false, current_timestamp, current_timestamp, 'flyway', 'flyway', 0),
    ('10000000-0000-0000-0000-000000000002', 'Idle', '#6B7280', 'IDLE', true, false, current_timestamp, current_timestamp, 'flyway', 'flyway', 0),
    ('10000000-0000-0000-0000-000000000003', 'Under maintenance', '#F59E0B', 'UNDER_MAINTENANCE', true, false, current_timestamp, current_timestamp, 'flyway', 'flyway', 0),
    ('10000000-0000-0000-0000-000000000004', 'Fault', '#EF4444', 'FAULT', true, false, current_timestamp, current_timestamp, 'flyway', 'flyway', 0),
    ('10000000-0000-0000-0000-000000000005', 'Decommissioned', '#64748B', 'DECOMMISSIONED', true, false, current_timestamp, current_timestamp, 'flyway', 'flyway', 0);
