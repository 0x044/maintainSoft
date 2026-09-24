create table refresh_tokens (
    id uuid not null,
    user_id uuid not null,
    family_id uuid not null,
    jti varchar(255) not null,
    token_hash varchar(64) not null,
    expires_at timestamp(6) with time zone not null,
    revoked_at timestamp(6) with time zone,
    replaced_by_jti varchar(255),
    created_at timestamp(6) with time zone not null default current_timestamp,
    updated_at timestamp(6) with time zone not null default current_timestamp,
    created_by varchar(255) not null,
    updated_by varchar(255) not null,
    version bigint not null default 0,
    primary key (id),
    constraint uq_refresh_tokens_jti unique (jti),
    constraint uq_refresh_tokens_hash unique (token_hash)
);

create index idx_refresh_tokens_user on refresh_tokens (user_id);
create index idx_refresh_tokens_family on refresh_tokens (family_id);

alter table refresh_tokens
    add constraint fk_refresh_tokens_user
    foreign key (user_id)
    references users (id);
