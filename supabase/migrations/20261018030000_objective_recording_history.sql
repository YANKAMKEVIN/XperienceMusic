-- Every submitted take is preserved. Only the latest remains the evaluation evidence.
create table public.objective_recording_history (
    audio_path text primary key,
    member_id uuid not null references public.profiles(id) on delete cascade,
    objective_id text not null,
    duration_seconds integer not null check(duration_seconds between 3 and 300),
    submitted_at timestamptz not null,
    check(audio_path like 'submissions/' || member_id::text || '/objectives/%')
);
insert into public.objective_recording_history
select audio_path,member_id,objective_id,duration_seconds,submitted_at from public.objective_recordings;
alter table public.objective_recording_history enable row level security;
create policy "owners and reviewers read take history" on public.objective_recording_history
for select to authenticated using(public.is_member() and (member_id=auth.uid() or public.is_super_admin()));
revoke all on public.objective_recording_history from anon,authenticated;
grant select on public.objective_recording_history to authenticated;
create function public.archive_objective_recording() returns trigger language plpgsql security definer set search_path='' as $$
begin
    insert into public.objective_recording_history(audio_path,member_id,objective_id,duration_seconds,submitted_at)
    values(new.audio_path,new.member_id,new.objective_id,new.duration_seconds,new.submitted_at);
    return new;
end;
$$;
create trigger archive_objective_recording after insert or update on public.objective_recordings
for each row execute function public.archive_objective_recording();
-- Submitted history is immutable for members, including its underlying audio.
drop policy "singers delete their voice" on storage.objects;
create policy "singers delete their voice" on storage.objects for delete to authenticated
using(bucket_id='audio' and name like 'submissions/' || auth.uid()::text || '/%'
    and not exists(select 1 from public.objective_recording_history h where h.audio_path=storage.objects.name));
