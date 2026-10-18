alter table public.learning_attempts add column variant text not null default 'original';
alter table public.learning_attempts add column hints smallint not null default 0 check(hints between 0 and 3);

create table public.objective_recordings (
    member_id uuid not null references public.profiles(id) on delete cascade default auth.uid(),
    objective_id text not null check(objective_id in ('piano-c1','piano-c2','piano-c3','piano-c4','piano-c5')),
    audio_path text not null,
    duration_seconds integer not null check(duration_seconds between 3 and 300),
    submitted_at timestamptz not null default now(),
    primary key(member_id,objective_id),
    check(audio_path like 'submissions/' || member_id::text || '/objectives/%')
);
alter table public.objective_recordings enable row level security;
create policy "owners and reviewers hear objective recordings" on public.objective_recordings
    for select to authenticated using(public.is_member() and (member_id=auth.uid() or public.is_super_admin()));
create policy "members submit objective recordings" on public.objective_recordings
    for insert to authenticated with check(public.is_member() and member_id=auth.uid());
create policy "members replace unvalidated objective recordings" on public.objective_recordings
    for update to authenticated using(public.is_member() and member_id=auth.uid())
    with check(public.is_member() and member_id=auth.uid());
-- Prevent replacement of evidence after the responsible person validates the objective.
create function public.guard_objective_recording() returns trigger language plpgsql set search_path='' as $$
begin
    if tg_op='UPDATE' and (new.member_id<>old.member_id or new.objective_id<>old.objective_id) then
        raise exception 'FORBIDDEN' using errcode='42501';
    end if;
    if exists(select 1 from public.objective_checks where member_id=new.member_id and objective_id=new.objective_id and status='VALIDATED') then
        raise exception 'FORBIDDEN' using errcode='42501';
    end if;
    new.submitted_at:=now();
    return new;
end;
$$;
create trigger guard_objective_recording before insert or update on public.objective_recordings for each row execute function public.guard_objective_recording();
revoke all on public.objective_recordings from anon;
grant select,insert,update on public.objective_recordings to authenticated;

-- Owners cannot delete and re-upload a file whose objective is already validated.
-- Old unreferenced takes remain deletable when a new, unvalidated take replaces them.
drop policy "singers delete their voice" on storage.objects;
create policy "singers delete their voice" on storage.objects for delete to authenticated
using (
    bucket_id='audio' and name like 'submissions/' || auth.uid()::text || '/%'
    and not exists (
        select 1 from public.objective_recordings recording
        join public.objective_checks assessment
          on assessment.member_id=recording.member_id and assessment.objective_id=recording.objective_id
        where recording.audio_path=storage.objects.name and assessment.status='VALIDATED'
    )
);
