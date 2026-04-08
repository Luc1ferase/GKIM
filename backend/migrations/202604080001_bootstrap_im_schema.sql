create extension if not exists pgcrypto;

create table users (
    id uuid primary key default gen_random_uuid(),
    external_id text not null unique,
    display_name text not null,
    title text not null,
    avatar_text text not null,
    created_at timestamptz not null default timezone('utc', now())
);

create table contacts (
    user_id uuid not null references users(id) on delete cascade,
    contact_user_id uuid not null references users(id) on delete cascade,
    nickname_override text,
    created_at timestamptz not null default timezone('utc', now()),
    primary key (user_id, contact_user_id),
    check (user_id <> contact_user_id)
);

create index contacts_user_created_at_idx
    on contacts (user_id, created_at desc);

create table direct_conversations (
    id uuid primary key default gen_random_uuid(),
    participant_a_user_id uuid not null references users(id) on delete cascade,
    participant_b_user_id uuid not null references users(id) on delete cascade,
    created_at timestamptz not null default timezone('utc', now()),
    updated_at timestamptz not null default timezone('utc', now()),
    check (participant_a_user_id <> participant_b_user_id)
);

create unique index direct_conversations_participants_idx
    on direct_conversations (
        least(participant_a_user_id, participant_b_user_id),
        greatest(participant_a_user_id, participant_b_user_id)
    );

create table conversation_members (
    conversation_id uuid not null references direct_conversations(id) on delete cascade,
    user_id uuid not null references users(id) on delete cascade,
    peer_user_id uuid not null references users(id) on delete cascade,
    unread_count integer not null default 0,
    last_read_message_id uuid,
    last_read_at timestamptz,
    last_delivered_message_id uuid,
    last_delivered_at timestamptz,
    joined_at timestamptz not null default timezone('utc', now()),
    primary key (conversation_id, user_id),
    unique (user_id, peer_user_id),
    check (user_id <> peer_user_id),
    check (unread_count >= 0)
);

create index conversation_members_user_unread_idx
    on conversation_members (user_id, unread_count desc);

create table messages (
    id uuid primary key default gen_random_uuid(),
    conversation_id uuid not null references direct_conversations(id) on delete cascade,
    sender_user_id uuid not null references users(id) on delete restrict,
    client_message_id text,
    message_kind text not null default 'text',
    body text not null,
    metadata jsonb not null default '{}'::jsonb,
    created_at timestamptz not null default timezone('utc', now()),
    edited_at timestamptz,
    unique (sender_user_id, client_message_id),
    check (message_kind in ('text', 'aigc'))
);

create index messages_conversation_created_at_idx
    on messages (conversation_id, created_at desc);

create table message_receipts (
    message_id uuid not null references messages(id) on delete cascade,
    user_id uuid not null references users(id) on delete cascade,
    delivered_at timestamptz,
    read_at timestamptz,
    primary key (message_id, user_id)
);

create index message_receipts_user_delivery_idx
    on message_receipts (user_id, delivered_at desc, read_at desc);

alter table conversation_members
    add constraint conversation_members_last_read_message_fk
    foreign key (last_read_message_id) references messages(id) on delete set null;

alter table conversation_members
    add constraint conversation_members_last_delivered_message_fk
    foreign key (last_delivered_message_id) references messages(id) on delete set null;

create table session_tokens (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references users(id) on delete cascade,
    token_hash text not null unique,
    issued_at timestamptz not null default timezone('utc', now()),
    expires_at timestamptz not null,
    last_used_at timestamptz,
    revoked_at timestamptz,
    created_for text not null default 'dev-bootstrap'
);

create index session_tokens_user_expires_idx
    on session_tokens (user_id, expires_at desc);

insert into users (external_id, display_name, title, avatar_text)
values
    ('nox-dev', 'Nox Dev', 'IM Milestone Owner', 'NX'),
    ('leo-vance', 'Leo Vance', 'Realtime Systems', 'LV'),
    ('aria-thorne', 'Aria Thorne', 'Prompt Architect', 'AT'),
    ('clara-wu', 'Clara Wu', 'Visual Systems', 'CW');

with seed_owner as (
    select id
    from users
    where external_id = 'nox-dev'
),
contact_users as (
    select id
    from users
    where external_id in ('leo-vance', 'aria-thorne', 'clara-wu')
)
insert into contacts (user_id, contact_user_id)
select seed_owner.id, contact_users.id
from seed_owner
cross join contact_users;
