alter table spares
    add constraint ck_spares_stock_nonnegative check (stock >= 0);

alter table repair_spares
    add constraint ck_repair_spares_used_quantity_nonnegative check (used_quantity >= 0);

alter table repairs
    add constraint ck_repairs_end_after_start check (end_date is null or end_date >= start_date);

alter table machines
    add constraint ck_machines_decommission_after_commission
        check (decommissioned_at is null or commissioned_at is null or decommissioned_at >= commissioned_at);
