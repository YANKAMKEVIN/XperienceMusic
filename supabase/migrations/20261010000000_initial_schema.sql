-- XperienceMusic — initial schema.
--
-- Security model: every table has row level security. Access depends on the role stored in
-- public.profiles, and a profile only exists for people listed in public.invitations, so an
-- uninvited account can sign in but sees nothing.
--   MEMBER         reads everything, tracks their own progression
--   CONTENT_ADMIN  + setlists, songs, lyrics, chords, recordings
--   SUPER_ADMIN    + invitations, roles, reviewing singers' training

-- ---------------------------------------------------------------------------------------------
-- Members
-- ---------------------------------------------------------------------------------------------

create table public.invitations (
    email      text primary key check (email = lower(email)),
    role       text not null default 'MEMBER' check (role in ('MEMBER', 'CONTENT_ADMIN', 'SUPER_ADMIN')),
    invited_at timestamptz not null default now()
);

create table public.profiles (
    id           uuid primary key references auth.users (id) on delete cascade,
    email        text not null,
    role         text not null default 'MEMBER' check (role in ('MEMBER', 'CONTENT_ADMIN', 'SUPER_ADMIN')),
    -- Null until the member answers the onboarding questions.
    display_name text check (display_name is null or length(trim(display_name)) > 0),
    instrument   text check (instrument in ('SINGER', 'PIANO', 'BASS', 'DRUMS')),
    voice_part   text check (voice_part in ('SOPRANO', 'ALTO', 'TENOR', 'BASS')),
    level        text check (level in ('BEGINNER', 'CONFIRMED', 'PRO')),
    updated_at   timestamptz not null default now(),
    constraint profiles_onboarding_complete
        check (display_name is null or (instrument is not null and level is not null))
);

-- Role helpers. SECURITY DEFINER so policies on profiles can call them without recursing.
create function public.current_member_role() returns text
    language sql stable security definer set search_path = ''
as $$
    select role from public.profiles where id = auth.uid()
$$;

create function public.is_member() returns boolean
    language sql stable security definer set search_path = ''
as $$
    select public.current_member_role() is not null
$$;

create function public.is_content_admin() returns boolean
    language sql stable security definer set search_path = ''
as $$
    select coalesce(public.current_member_role() in ('CONTENT_ADMIN', 'SUPER_ADMIN'), false)
$$;

create function public.is_super_admin() returns boolean
    language sql stable security definer set search_path = ''
as $$
    select coalesce(public.current_member_role() = 'SUPER_ADMIN', false)
$$;

-- A profile is created when an invited email signs up, or when an existing account gets invited.
create function public.create_profile_for_new_user() returns trigger
    language plpgsql security definer set search_path = ''
as $$
begin
    insert into public.profiles (id, email, role)
    select new.id, lower(new.email), invitation.role
    from public.invitations invitation
    where invitation.email = lower(new.email)
    on conflict (id) do nothing;
    return new;
end;
$$;

create trigger on_auth_user_created
    after insert on auth.users
    for each row execute function public.create_profile_for_new_user();

create function public.create_profile_for_invitation() returns trigger
    language plpgsql security definer set search_path = ''
as $$
begin
    insert into public.profiles (id, email, role)
    select account.id, new.email, new.role
    from auth.users account
    where lower(account.email) = new.email
    on conflict (id) do nothing;
    return new;
end;
$$;

create trigger on_invitation_created
    after insert on public.invitations
    for each row execute function public.create_profile_for_invitation();

-- Only a super admin changes roles; nobody rewrites an id or email through the API.
create function public.guard_profile_update() returns trigger
    language plpgsql set search_path = ''
as $$
begin
    if new.id <> old.id or new.email <> old.email then
        raise exception 'FORBIDDEN' using errcode = '42501';
    end if;
    if new.role <> old.role and not public.is_super_admin() then
        raise exception 'FORBIDDEN' using errcode = '42501';
    end if;
    new.updated_at := now();
    return new;
end;
$$;

create trigger guard_profile_update
    before update on public.profiles
    for each row execute function public.guard_profile_update();

alter table public.invitations enable row level security;
alter table public.profiles enable row level security;

create policy "super admins manage invitations" on public.invitations
    for all to authenticated
    using (public.is_super_admin()) with check (public.is_super_admin());

create policy "members see the band" on public.profiles
    for select to authenticated using (public.is_member());

create policy "members edit their own profile" on public.profiles
    for update to authenticated
    using (id = auth.uid() or public.is_super_admin())
    with check (id = auth.uid() or public.is_super_admin());

create policy "super admins remove members" on public.profiles
    for delete to authenticated using (public.is_super_admin());

-- ---------------------------------------------------------------------------------------------
-- Songs
-- ---------------------------------------------------------------------------------------------

create table public.songs (
    id         uuid primary key default gen_random_uuid(),
    title      text not null check (length(trim(title)) > 0),
    artist     text,
    key_tonic  smallint not null check (key_tonic between 0 and 11),
    key_mode   text not null default 'MAJOR' check (key_mode in ('MAJOR', 'MINOR')),
    -- ChordPro-style chart, chords stored as Nashville numbers.
    chart      text not null default '',
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

-- At most 3 per song (one reference + two alternatives), enforced by a trigger below.
create table public.recordings (
    id               uuid primary key default gen_random_uuid(),
    song_id          uuid not null references public.songs (id) on delete cascade,
    title            text not null check (length(trim(title)) > 0),
    recorded_on      date not null,
    duration_seconds integer not null check (duration_seconds > 0),
    storage_path     text not null,
    is_reference     boolean not null default false,
    created_at       timestamptz not null default now()
);

create index recordings_song_id on public.recordings (song_id);
create unique index recordings_one_reference_per_song on public.recordings (song_id) where is_reference;

-- Learning tracks per voice; they do not count towards the 3 recordings.
create table public.voice_tracks (
    id           uuid primary key default gen_random_uuid(),
    song_id      uuid not null references public.songs (id) on delete cascade,
    voice_part   text not null check (voice_part in ('SOPRANO', 'ALTO', 'TENOR', 'BASS')),
    storage_path text not null,
    unique (song_id, voice_part)
);

create table public.song_links (
    id       uuid primary key default gen_random_uuid(),
    song_id  uuid not null references public.songs (id) on delete cascade,
    label    text not null,
    url      text not null check (url ~* '^https?://'),
    position integer not null default 0
);

create index song_links_song_id on public.song_links (song_id);

create function public.touch_updated_at() returns trigger
    language plpgsql set search_path = ''
as $$
begin
    new.updated_at := now();
    return new;
end;
$$;

create trigger touch_songs_updated_at
    before update on public.songs
    for each row execute function public.touch_updated_at();

create function public.enforce_recording_limit() returns trigger
    language plpgsql set search_path = ''
as $$
begin
    -- Serialize concurrent inserts on the same song.
    perform 1 from public.songs where id = new.song_id for update;
    if (select count(*) from public.recordings where song_id = new.song_id) >= 3 then
        raise exception 'RECORDING_LIMIT_REACHED' using errcode = 'P0001';
    end if;
    return new;
end;
$$;

create trigger enforce_recording_limit
    before insert on public.recordings
    for each row execute function public.enforce_recording_limit();

alter table public.songs enable row level security;
alter table public.recordings enable row level security;
alter table public.voice_tracks enable row level security;
alter table public.song_links enable row level security;

create policy "members read songs" on public.songs
    for select to authenticated using (public.is_member());
create policy "content admins write songs" on public.songs
    for all to authenticated using (public.is_content_admin()) with check (public.is_content_admin());

create policy "members read recordings" on public.recordings
    for select to authenticated using (public.is_member());
create policy "content admins write recordings" on public.recordings
    for all to authenticated using (public.is_content_admin()) with check (public.is_content_admin());

create policy "members read voice tracks" on public.voice_tracks
    for select to authenticated using (public.is_member());
create policy "content admins write voice tracks" on public.voice_tracks
    for all to authenticated using (public.is_content_admin()) with check (public.is_content_admin());

create policy "members read links" on public.song_links
    for select to authenticated using (public.is_member());
create policy "content admins write links" on public.song_links
    for all to authenticated using (public.is_content_admin()) with check (public.is_content_admin());

-- Recording operations, each atomic. SECURITY INVOKER: the policies above still apply.

create function public.add_recording(
    p_id uuid,
    p_song_id uuid,
    p_title text,
    p_recorded_on date,
    p_duration_seconds integer,
    p_storage_path text,
    p_make_reference boolean,
    p_replacing_id uuid default null
) returns void
    language plpgsql security invoker set search_path = ''
as $$
begin
    if not public.is_content_admin() then
        raise exception 'FORBIDDEN' using errcode = '42501';
    end if;
    perform 1 from public.songs where id = p_song_id for update;
    if not found then
        raise exception 'NOT_FOUND' using errcode = 'P0002';
    end if;

    if p_replacing_id is not null then
        delete from public.recordings where id = p_replacing_id and song_id = p_song_id;
        if not found then
            raise exception 'NOT_FOUND' using errcode = 'P0002';
        end if;
    end if;

    -- A song with recordings always has a reference.
    if not exists (select 1 from public.recordings where song_id = p_song_id and is_reference) then
        p_make_reference := true;
    end if;
    if p_make_reference then
        update public.recordings set is_reference = false where song_id = p_song_id and is_reference;
    end if;

    insert into public.recordings (id, song_id, title, recorded_on, duration_seconds, storage_path, is_reference)
    values (p_id, p_song_id, p_title, p_recorded_on, p_duration_seconds, p_storage_path, p_make_reference);
end;
$$;

-- Returns the storage path of the deleted file so the client can remove it from the bucket.
create function public.delete_recording(p_recording_id uuid, p_new_reference_id uuid default null)
    returns text
    language plpgsql security invoker set search_path = ''
as $$
declare
    v_song_id uuid;
    v_path text;
begin
    if not public.is_content_admin() then
        raise exception 'FORBIDDEN' using errcode = '42501';
    end if;
    select song_id into v_song_id from public.recordings where id = p_recording_id;
    if not found then
        raise exception 'NOT_FOUND' using errcode = 'P0002';
    end if;
    perform 1 from public.songs where id = v_song_id for update;

    delete from public.recordings where id = p_recording_id returning storage_path into v_path;

    if not exists (select 1 from public.recordings where song_id = v_song_id and is_reference) then
        update public.recordings
        set is_reference = true
        where id = coalesce(
            (select id from public.recordings where id = p_new_reference_id and song_id = v_song_id),
            (select id from public.recordings where song_id = v_song_id order by recorded_on desc, created_at desc limit 1)
        );
    end if;
    return v_path;
end;
$$;

create function public.set_reference_recording(p_recording_id uuid) returns void
    language plpgsql security invoker set search_path = ''
as $$
declare
    v_song_id uuid;
begin
    if not public.is_content_admin() then
        raise exception 'FORBIDDEN' using errcode = '42501';
    end if;
    select song_id into v_song_id from public.recordings where id = p_recording_id;
    if not found then
        raise exception 'NOT_FOUND' using errcode = 'P0002';
    end if;
    perform 1 from public.songs where id = v_song_id for update;
    -- Two statements: the partial unique index is checked row by row.
    update public.recordings set is_reference = false where song_id = v_song_id and is_reference;
    update public.recordings set is_reference = true where id = p_recording_id;
end;
$$;

-- ---------------------------------------------------------------------------------------------
-- Setlists
-- ---------------------------------------------------------------------------------------------

create table public.setlists (
    id                uuid primary key default gen_random_uuid(),
    rehearsal_date    date not null,
    service_date      date not null,
    message           text,
    published_by      uuid references public.profiles (id) on delete set null,
    published_by_name text not null,
    published_at      timestamptz not null default now(),
    check (service_date >= rehearsal_date)
);

create index setlists_published_at on public.setlists (published_at desc);

create table public.setlist_songs (
    setlist_id uuid not null references public.setlists (id) on delete cascade,
    position   integer not null check (position > 0),
    song_id    uuid not null references public.songs (id) on delete cascade,
    primary key (setlist_id, position)
);

alter table public.setlists enable row level security;
alter table public.setlist_songs enable row level security;

create policy "members read setlists" on public.setlists
    for select to authenticated using (public.is_member());
create policy "content admins write setlists" on public.setlists
    for all to authenticated using (public.is_content_admin()) with check (public.is_content_admin());

create policy "members read setlist songs" on public.setlist_songs
    for select to authenticated using (public.is_member());
create policy "content admins write setlist songs" on public.setlist_songs
    for all to authenticated using (public.is_content_admin()) with check (public.is_content_admin());

create function public.publish_setlist(
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
    returning id into v_setlist_id;

    insert into public.setlist_songs (setlist_id, position, song_id)
    select v_setlist_id, song.position, song.id
    from unnest(p_song_ids) with ordinality as song (id, position);

    return v_setlist_id;
end;
$$;

-- ---------------------------------------------------------------------------------------------
-- Progression (singers for now). Exercises are app content; only statuses live here.
-- ---------------------------------------------------------------------------------------------

create table public.exercise_progress (
    member_id   uuid not null default auth.uid() references public.profiles (id) on delete cascade,
    exercise_id text not null,
    status      text not null check (status in ('TODO', 'SUBMITTED', 'VALIDATED', 'TO_REWORK')),
    updated_at  timestamptz not null default now(),
    primary key (member_id, exercise_id)
);

alter table public.exercise_progress enable row level security;

create policy "members see their progress, reviewers see all" on public.exercise_progress
    for select to authenticated using (member_id = auth.uid() or public.is_super_admin());

-- Members record their own attempts; only a reviewer sends an exercise back with TO_REWORK.
create policy "members update their progress" on public.exercise_progress
    for insert to authenticated
    with check (
        (member_id = auth.uid() and status in ('TODO', 'SUBMITTED', 'VALIDATED')) or public.is_super_admin()
    );
create policy "members change their progress" on public.exercise_progress
    for update to authenticated
    using (member_id = auth.uid() or public.is_super_admin())
    with check (
        (member_id = auth.uid() and status in ('TODO', 'SUBMITTED', 'VALIDATED')) or public.is_super_admin()
    );

create trigger touch_exercise_progress_updated_at
    before update on public.exercise_progress
    for each row execute function public.touch_updated_at();

-- ---------------------------------------------------------------------------------------------
-- API surface: nothing for anonymous visitors.
-- ---------------------------------------------------------------------------------------------

-- Explicit grants: the project may be created with "automatically expose new tables" off.
-- Row level security still decides which rows each member sees or changes.
revoke all on all tables in schema public from anon;
grant usage on schema public to authenticated;
grant select, insert, update, delete on all tables in schema public to authenticated;
revoke execute on all functions in schema public from public, anon;
grant execute on function
    public.current_member_role(),
    public.is_member(),
    public.is_content_admin(),
    public.is_super_admin(),
    public.add_recording(uuid, uuid, text, date, integer, text, boolean, uuid),
    public.delete_recording(uuid, uuid),
    public.set_reference_recording(uuid),
    public.publish_setlist(date, date, text, uuid[])
    to authenticated;

-- ---------------------------------------------------------------------------------------------
-- Audio storage: private bucket, read by members, written by content admins.
-- ---------------------------------------------------------------------------------------------

insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values ('audio', 'audio', false, 52428800, array['audio/mpeg', 'audio/mp4', 'audio/aac', 'audio/x-m4a', 'audio/ogg', 'audio/webm', 'audio/wav'])
on conflict (id) do nothing;

create policy "members listen to audio" on storage.objects
    for select to authenticated using (bucket_id = 'audio' and public.is_member());
create policy "content admins upload audio" on storage.objects
    for insert to authenticated with check (bucket_id = 'audio' and public.is_content_admin());
create policy "content admins replace audio" on storage.objects
    for update to authenticated using (bucket_id = 'audio' and public.is_content_admin());
create policy "content admins delete audio" on storage.objects
    for delete to authenticated using (bucket_id = 'audio' and public.is_content_admin());
