alter table machines add column status_id uuid;

update machines m
set status_id = s.id
from machine_statuses s
where s.system_key = m.status;

do $$
begin
    if exists (select 1 from machines where status_id is null) then
        raise exception 'Unable to map every machine to a machine status';
    end if;
end;
$$;

alter table machines alter column status_id set not null;

alter table machines
    add constraint fk_machines_status
    foreign key (status_id)
    references machine_statuses (id);

drop index if exists idx_machine_status;

alter table machines drop column status;

create index idx_machine_status_id on machines (status_id);
