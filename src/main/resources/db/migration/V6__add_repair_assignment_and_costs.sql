alter table repairs
    add column external_technician_name varchar(255),
    add column external_technician_phone varchar(32),
    add column assigned_supervisor_id uuid;

alter table repairs
    add constraint fk_repairs_assigned_supervisor
    foreign key (assigned_supervisor_id)
    references users (id);

create index idx_repair_assigned_supervisor on repairs (assigned_supervisor_id);

create table repair_costs (
    id uuid not null,
    repair_id uuid not null,
    category varchar(20) not null,
    amount numeric(19,2) not null,
    description varchar(1000),
    created_at timestamp(6) with time zone not null default current_timestamp,
    updated_at timestamp(6) with time zone not null default current_timestamp,
    created_by varchar(255) not null,
    updated_by varchar(255) not null,
    primary key (id),
    constraint ck_repair_costs_category
        check (category in ('LABOR', 'PARTS', 'TRAVEL', 'OTHER')),
    constraint ck_repair_costs_amount check (amount >= 0)
);

create index idx_repair_cost_repair on repair_costs (repair_id);

alter table repair_costs
    add constraint fk_repair_costs_repair
    foreign key (repair_id)
    references repairs (id);
