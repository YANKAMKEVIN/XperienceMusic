-- Who serves each Friday and Sunday. An admin opens a month; during it every member says, for each
-- service, "je sers" or "pas dispo". Admins adjust the answers, then validate the month: from then on
-- only they change it. Each member's part (singer and voice, piano, bass, drums) is their profile's.

create table public.service_months (
    month        date primary key check (extract(day from month) = 1),
    -- Members are reminded until this date; null leaves the month open without a deadline.
    answer_by    date,
    validated_at timestamptz,
    opened_by    uuid references public.profiles (id) on delete set null default auth.uid(),
    opened_at    timestamptz not null default now()
);

create table public.service_signups (
    service_date date not null check (extract(isodow from service_date) in (5, 7)),
    member_id    uuid not null default auth.uid() references public.profiles (id) on delete cascade,
    serves       boolean not null,
    updated_at   timestamptz not null default now(),
    primary key (service_date, member_id)
);

-- True while members can still answer for the month of p_date.
create function public.service_month_is_open(p_date date) returns boolean
    language sql stable security definer set search_path = ''
as $$
    select exists (
        select 1 from public.service_months
        where month = date_trunc('month', p_date)::date and validated_at is null
    )
$$;

alter table public.service_months enable row level security;
alter table public.service_signups enable row level security;

create policy "members read the planning months" on public.service_months
    for select to authenticated using (public.is_member());
create policy "admins open and validate months" on public.service_months
    for all to authenticated using (public.is_content_admin()) with check (public.is_content_admin());

create policy "members read the planning" on public.service_signups
    for select to authenticated using (public.is_member());
create policy "members answer for themselves while the month is open" on public.service_signups
    for insert to authenticated with check (
        public.is_content_admin()
        or (public.is_member() and member_id = auth.uid() and public.service_month_is_open(service_date))
    );
create policy "members change their answer while the month is open" on public.service_signups
    for update to authenticated
    using (public.is_content_admin() or (member_id = auth.uid() and public.service_month_is_open(service_date)))
    with check (
        public.is_content_admin()
        or (member_id = auth.uid() and public.service_month_is_open(service_date))
    );
create policy "admins remove answers" on public.service_signups
    for delete to authenticated using (public.is_content_admin());

grant select, insert, update, delete on public.service_months to authenticated;
grant select, insert, update, delete on public.service_signups to authenticated;
