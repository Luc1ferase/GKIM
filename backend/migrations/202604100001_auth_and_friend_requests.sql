-- Add credential columns to users table for real registration/login.
-- password_hash is nullable: dev-seeded users retain dev-session-only access,
-- while new registered users must set a hash.
alter table users add column username text;
alter table users add column password_hash text;

-- Backfill existing dev users: username = external_id (so they remain addressable).
update users set username = external_id where username is null;

alter table users alter column username set not null;
create unique index users_username_lower_idx on users (lower(username));

-- Create friend_requests table
create table friend_requests (
    id uuid primary key default gen_random_uuid(),
    from_user_id uuid not null references users(id) on delete cascade,
    to_user_id uuid not null references users(id) on delete cascade,
    status text not null default 'pending',
    created_at timestamptz not null default timezone('utc', now()),
    responded_at timestamptz,
    check (from_user_id <> to_user_id),
    check (status in ('pending', 'accepted', 'rejected'))
);

-- Only one pending request may exist between any pair (in either direction)
create unique index friend_requests_pending_pair_idx
    on friend_requests (
        least(from_user_id, to_user_id),
        greatest(from_user_id, to_user_id)
    )
    where status = 'pending';

create index friend_requests_to_user_pending_idx
    on friend_requests (to_user_id) where status = 'pending';

-- Ensure existing dev contacts are reciprocal so contact-gated messaging keeps working.
insert into contacts (user_id, contact_user_id)
select c.contact_user_id, c.user_id
from contacts c
where not exists (
    select 1 from contacts c2
    where c2.user_id = c.contact_user_id
      and c2.contact_user_id = c.user_id
)
on conflict (user_id, contact_user_id) do nothing;
