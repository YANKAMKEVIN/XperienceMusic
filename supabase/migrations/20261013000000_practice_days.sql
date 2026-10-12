-- The days each member practised (an exercise or the daily warm-up), for the streak shown in
-- "Ma progression". One row per member and day; the app computes the streak.

create table public.practice_days (
    member_id uuid not null default auth.uid() references public.profiles (id) on delete cascade,
    day       date not null,
    primary key (member_id, day)
);

alter table public.practice_days enable row level security;

create policy "members see their practice days, reviewers see all" on public.practice_days
    for select to authenticated using (member_id = auth.uid() or public.is_super_admin());

create policy "members record their own practice" on public.practice_days
    for insert to authenticated with check (member_id = auth.uid());

grant select, insert on public.practice_days to authenticated;
