-- Content administrators manage the repertoire, never personal voice submissions.
drop policy "content admins upload audio" on storage.objects;
drop policy "content admins replace audio" on storage.objects;
drop policy "content admins delete audio" on storage.objects;
create policy "content admins upload audio" on storage.objects for insert to authenticated
    with check (bucket_id = 'audio' and public.is_content_admin() and name not like 'submissions/%');
create policy "content admins replace audio" on storage.objects for update to authenticated
    using (bucket_id = 'audio' and public.is_content_admin() and name not like 'submissions/%')
    with check (bucket_id = 'audio' and public.is_content_admin() and name not like 'submissions/%');
create policy "content admins delete audio" on storage.objects for delete to authenticated
    using (bucket_id = 'audio' and public.is_content_admin() and name not like 'submissions/%');
-- Super admins retain moderation access to personal files.
create policy "reviewers manage voice files" on storage.objects for all to authenticated
    using (bucket_id = 'audio' and public.is_super_admin())
    with check (bucket_id = 'audio' and public.is_super_admin());

-- Recording writes must use the atomic RPCs. Triggers also protect direct API writes.
-- Reparenting is unnecessary for the application and would bypass the insertion limit.
create function public.prevent_recording_reparent() returns trigger
    language plpgsql set search_path = '' as $$
begin
    if new.song_id is distinct from old.song_id then
        raise exception 'RECORDING_REPARENT_FORBIDDEN' using errcode = '23514';
    end if;
    return new;
end;
$$;
create trigger prevent_recording_reparent before update on public.recordings
    for each row execute function public.prevent_recording_reparent();

-- Deferred: the RPCs temporarily clear the old reference before assigning the next one.
create function public.check_recording_reference() returns trigger
    language plpgsql set search_path = '' as $$
declare
    song uuid;
    total integer;
    refs integer;
begin
    if tg_op = 'DELETE' then song := old.song_id; else song := new.song_id; end if;
    perform 1 from public.songs where id = song for update;
    select count(*), count(*) filter (where is_reference) into total, refs
        from public.recordings where song_id = song;
    if total > 3 or (total > 0 and refs <> 1) then
        raise exception 'INVALID_RECORDING_REFERENCE' using errcode = '23514';
    end if;
    return null;
end;
$$;
create constraint trigger check_recording_reference
    after insert or update or delete on public.recordings
    deferrable initially deferred for each row execute function public.check_recording_reference();
