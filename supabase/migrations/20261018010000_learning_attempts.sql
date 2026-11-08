-- Learning attempts record practice, never grant a skill or raise a member's level.
create table public.learning_attempts (
    id uuid primary key,
    member_id uuid not null references public.profiles(id) on delete cascade default auth.uid(),
    activity_id text not null check (length(activity_id) between 1 and 120),
    version integer not null check (version > 0),
    passed boolean not null,
    day date not null,
    received_at timestamptz not null default now()
);
create index learning_attempts_member on public.learning_attempts(member_id, received_at);
alter table public.learning_attempts enable row level security;
create policy "members read their learning history" on public.learning_attempts
    for select to authenticated using (public.is_member() and member_id = auth.uid());
create policy "members append their learning attempts" on public.learning_attempts
    for insert to authenticated with check (public.is_member() and member_id = auth.uid());
-- Immutable history. An idempotent retry uses INSERT ... ON CONFLICT DO NOTHING.
revoke all on public.learning_attempts from anon;
grant select, insert on public.learning_attempts to authenticated;
