-- Growth path: Débutant → Confirmé → Pro. A member asks to move up once the tiers (singers) or the
-- objectives (musicians) of the next level are done; the super admin confirms or sends it back
-- with a comment. Musicians tick their objectives, the super admin validates each one. After
-- onboarding, only the super admin changes a member's level.

create or replace function public.guard_profile_update() returns trigger
    language plpgsql set search_path = ''
as $$
begin
    if new.id <> old.id or new.email <> old.email then
        raise exception 'FORBIDDEN' using errcode = '42501';
    end if;
    if new.role <> old.role and not public.is_super_admin() then
        raise exception 'FORBIDDEN' using errcode = '42501';
    end if;
    -- The level is the member's own answer at onboarding, then the super admin's call.
    if new.level is distinct from old.level and old.level is not null and not public.is_super_admin() then
        raise exception 'FORBIDDEN' using errcode = '42501';
    end if;
    new.updated_at := now();
    return new;
end;
$$;

create table public.level_requests (
    id           uuid primary key default gen_random_uuid(),
    member_id    uuid not null default auth.uid() references public.profiles (id) on delete cascade,
    target_level text not null check (target_level in ('CONFIRMED', 'PRO')),
    status       text not null default 'SUBMITTED' check (status in ('SUBMITTED', 'VALIDATED', 'TO_REWORK')),
    feedback     text check (feedback is null or length(trim(feedback)) > 0),
    submitted_at timestamptz not null default now(),
    reviewed_at  timestamptz,
    unique (member_id, target_level)
);

create table public.objective_checks (
    member_id    uuid not null default auth.uid() references public.profiles (id) on delete cascade,
    objective_id text not null,
    status       text not null default 'SUBMITTED' check (status in ('SUBMITTED', 'VALIDATED', 'TO_REWORK')),
    feedback     text check (feedback is null or length(trim(feedback)) > 0),
    submitted_at timestamptz not null default now(),
    reviewed_at  timestamptz,
    primary key (member_id, objective_id)
);

-- A confirmed request moves the member up. SECURITY DEFINER: the guard above lets the super admin
-- through, and only the super admin can set VALIDATED (see the policies below).
create function public.apply_level_request() returns trigger
    language plpgsql security definer set search_path = ''
as $$
begin
    if new.status = 'VALIDATED' and old.status is distinct from 'VALIDATED' then
        update public.profiles set level = new.target_level where id = new.member_id;
    end if;
    return new;
end;
$$;

create trigger on_level_request_reviewed
    after update on public.level_requests
    for each row execute function public.apply_level_request();

alter table public.level_requests enable row level security;
alter table public.objective_checks enable row level security;

create policy "members see their requests, reviewers see all" on public.level_requests
    for select to authenticated using (member_id = auth.uid() or public.is_super_admin());
create policy "members ask to move up" on public.level_requests
    for insert to authenticated
    with check (member_id = auth.uid() and status = 'SUBMITTED' and feedback is null and reviewed_at is null);
create policy "members ask again, reviewers answer" on public.level_requests
    for update to authenticated
    using (member_id = auth.uid() or public.is_super_admin())
    with check (
        public.is_super_admin()
        or (member_id = auth.uid() and status = 'SUBMITTED' and feedback is null and reviewed_at is null)
    );

create policy "members see their objectives, reviewers see all" on public.objective_checks
    for select to authenticated using (member_id = auth.uid() or public.is_super_admin());
create policy "members tick an objective" on public.objective_checks
    for insert to authenticated
    with check (member_id = auth.uid() and status = 'SUBMITTED' and feedback is null and reviewed_at is null);
create policy "members tick again, reviewers answer" on public.objective_checks
    for update to authenticated
    using (member_id = auth.uid() or public.is_super_admin())
    with check (
        public.is_super_admin()
        or (member_id = auth.uid() and status = 'SUBMITTED' and feedback is null and reviewed_at is null)
    );
create policy "members untick, reviewers clean up" on public.objective_checks
    for delete to authenticated using (member_id = auth.uid() or public.is_super_admin());

grant select, insert, update on public.level_requests to authenticated;
grant select, insert, update, delete on public.objective_checks to authenticated;
