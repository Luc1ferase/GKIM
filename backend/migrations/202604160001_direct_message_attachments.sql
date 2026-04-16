create table message_attachments (
    message_id uuid primary key references messages(id) on delete cascade,
    attachment_type text not null,
    content_type text not null,
    storage_key text not null unique,
    byte_size bigint not null,
    data bytea not null,
    created_at timestamptz not null default timezone('utc', now()),
    check (attachment_type in ('image'))
);

create index message_attachments_created_at_idx
    on message_attachments (created_at desc);
