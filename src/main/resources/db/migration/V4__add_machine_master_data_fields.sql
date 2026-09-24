alter table machines
    add column equipment_type varchar(255),
    add column manufacturer varchar(255),
    add column model varchar(255),
    add column location varchar(255),
    add column commissioned_at timestamp(6) with time zone,
    add column decommissioned_at timestamp(6) with time zone,
    add column last_serviced_at timestamp(6) with time zone,
    add column next_service_due_at timestamp(6) with time zone,
    add column maintenance_interval_days integer;

alter table machines
    add constraint ck_machines_maintenance_interval
    check (maintenance_interval_days is null or maintenance_interval_days > 0);

alter table machines
    add constraint ck_machines_lifecycle_dates
    check (
        commissioned_at is null
        or decommissioned_at is null
        or decommissioned_at >= commissioned_at
    );
