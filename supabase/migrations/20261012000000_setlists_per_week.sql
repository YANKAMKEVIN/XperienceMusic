-- One setlist per service: the band can receive the next weeks' setlists ahead of time, and
-- publishing again for the same Sunday replaces that week's setlist instead of adding another.

-- Keep only the latest publication of each Sunday before enforcing it.
delete from public.setlists older
using public.setlists newer
where newer.service_date = older.service_date
  and (newer.published_at, newer.id) > (older.published_at, older.id);

alter table public.setlists add constraint setlists_service_date_key unique (service_date);
drop index if exists public.setlists_published_at;

create or replace function public.publish_setlist(
    p_rehearsal_date date,
    p_service_date date,
    p_message text,
    p_song_ids uuid[]
) returns uuid
    language plpgsql security invoker set search_path = ''
as $$
declare
    v_setlist_id uuid;
begin
    if not public.is_content_admin() then
        raise exception 'FORBIDDEN' using errcode = '42501';
    end if;
    if coalesce(array_length(p_song_ids, 1), 0) = 0 then
        raise exception 'INVALID_INPUT' using errcode = '22023';
    end if;

    insert into public.setlists (rehearsal_date, service_date, message, published_by, published_by_name)
    select p_rehearsal_date, p_service_date, nullif(trim(p_message), ''), auth.uid(), profile.display_name
    from public.profiles profile
    where profile.id = auth.uid()
    on conflict (service_date) do update set
        rehearsal_date = excluded.rehearsal_date,
        message = excluded.message,
        published_by = excluded.published_by,
        published_by_name = excluded.published_by_name,
        published_at = now()
    returning id into v_setlist_id;

    delete from public.setlist_songs where setlist_id = v_setlist_id;
    insert into public.setlist_songs (setlist_id, position, song_id)
    select v_setlist_id, song.position, song.id
    from unnest(p_song_ids) with ordinality as song (id, position);

    return v_setlist_id;
end;
$$;

-- Removing a week (a service cancelled, a setlist sent by mistake).
create function public.delete_setlist(p_service_date date) returns void
    language plpgsql security invoker set search_path = ''
as $$
begin
    if not public.is_content_admin() then
        raise exception 'FORBIDDEN' using errcode = '42501';
    end if;
    delete from public.setlists where service_date = p_service_date;
end;
$$;

grant execute on function public.delete_setlist(date) to authenticated;
