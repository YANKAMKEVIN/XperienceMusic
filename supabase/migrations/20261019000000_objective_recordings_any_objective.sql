-- Takes may back any objective of the programme, not only the first five piano modules: bass,
-- drums and singers will record too. The id still has to look like one (part, level, 1 to 7).
alter table public.objective_recordings drop constraint objective_recordings_objective_id_check;
alter table public.objective_recordings add constraint objective_recordings_objective_id_check
    check (objective_id ~ '^(piano|bass|drums|singer)-[cp][1-7]$');

-- An update that keeps the same file (e.g. a retried request) must not fail on the history key.
create or replace function public.archive_objective_recording() returns trigger
    language plpgsql security definer set search_path = ''
as $$
begin
    insert into public.objective_recording_history (audio_path, member_id, objective_id, duration_seconds, submitted_at)
    values (new.audio_path, new.member_id, new.objective_id, new.duration_seconds, new.submitted_at)
    on conflict (audio_path) do nothing;
    return new;
end;
$$;
