alter table spares
    add column description varchar(1000),
    add column unit varchar(32),
    add column compatible_machine varchar(255);

create index idx_spare_compatible_machine on spares (compatible_machine);
