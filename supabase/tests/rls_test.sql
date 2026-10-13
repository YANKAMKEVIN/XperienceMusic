-- Behaviour checks for the schema. Run after supabase_stubs.sql and the migrations.
\set ON_ERROR_STOP on

-- Fixtures (as the database owner, like the dashboard would).
insert into public.invitations (email, role) values
    ('admin@band.test', 'SUPER_ADMIN'), ('content@band.test', 'CONTENT_ADMIN'), ('member@band.test', 'MEMBER');
insert into auth.users (id, email) values
    ('00000000-0000-0000-0000-00000000000a', 'Admin@band.test'),
    ('00000000-0000-0000-0000-00000000000c', 'content@band.test'),
    ('00000000-0000-0000-0000-00000000000d', 'member@band.test'),
    ('00000000-0000-0000-0000-00000000000e', 'stranger@else.test');
update public.profiles set display_name = 'Admin', instrument = 'SINGER', level = 'PRO';
insert into public.songs (id, title, key_tonic) values ('10000000-0000-0000-0000-000000000001', 'À toi la gloire', 2);

do $$ begin
    assert (select count(*) from public.profiles) = 3, 'only invited accounts get a profile';
    assert (select role from public.profiles where email = 'admin@band.test') = 'SUPER_ADMIN', 'email match ignores case';
end $$;

create function pg_temp.act_as(p_user text) returns void language sql as $$
    select set_config('request.jwt.claim.sub', p_user, false)
$$;

set role authenticated;

-- A stranger signs in but sees nothing.
select pg_temp.act_as('00000000-0000-0000-0000-00000000000e');
do $$ begin
    assert (select count(*) from public.songs) = 0, 'stranger cannot read songs';
    assert (select count(*) from public.profiles) = 0, 'stranger cannot read profiles';
end $$;

-- A member reads but cannot write.
select pg_temp.act_as('00000000-0000-0000-0000-00000000000d');
do $$ begin
    assert (select count(*) from public.songs) = 1, 'member reads songs';
    begin
        perform public.add_recording(gen_random_uuid(), '10000000-0000-0000-0000-000000000001', 'x', current_date, 10, 'p', false);
        assert false, 'member must not add recordings';
    exception when insufficient_privilege then null;
    end;
    begin
        update public.profiles set role = 'SUPER_ADMIN' where id = auth.uid();
        assert false, 'member must not promote themself';
    exception when insufficient_privilege then null;
    end;
    update public.songs set title = 'hacked';
    assert (select title from public.songs) = 'À toi la gloire', 'member updates are filtered out by RLS';
    insert into public.exercise_progress (exercise_id, status) values ('b-breath', 'SUBMITTED');
    begin
        insert into public.exercise_progress (exercise_id, status) values ('b-scale', 'TO_REWORK');
        assert false, 'only reviewers set TO_REWORK';
    exception when insufficient_privilege then null;
    end;
end $$;

-- A content admin manages recordings within the 3-version rule.
select pg_temp.act_as('00000000-0000-0000-0000-00000000000c');
do $$
declare
    song constant uuid := '10000000-0000-0000-0000-000000000001';
begin
    perform public.add_recording('20000000-0000-0000-0000-000000000001', song, 'A', '2026-01-01', 200, 'a.m4a', false);
    perform public.add_recording('20000000-0000-0000-0000-000000000002', song, 'B', '2026-02-01', 200, 'b.m4a', false);
    perform public.add_recording('20000000-0000-0000-0000-000000000003', song, 'C', '2026-03-01', 200, 'c.m4a', false);
    assert (select id from public.recordings where is_reference) = '20000000-0000-0000-0000-000000000001',
        'first recording becomes the reference';
    begin
        perform public.add_recording(gen_random_uuid(), song, 'D', '2026-04-01', 200, 'd.m4a', false);
        assert false, 'a 4th recording must be refused';
    exception when raise_exception then
        assert sqlerrm = 'RECORDING_LIMIT_REACHED';
    end;

    -- Replacing the reference hands the reference to the new recording.
    perform public.add_recording('20000000-0000-0000-0000-000000000004', song, 'D', '2026-04-01', 200, 'd.m4a', false,
        '20000000-0000-0000-0000-000000000001');
    assert (select count(*) from public.recordings) = 3, 'replace keeps 3 recordings';
    assert (select id from public.recordings where is_reference) = '20000000-0000-0000-0000-000000000004',
        'the replacement becomes the reference';

    perform public.set_reference_recording('20000000-0000-0000-0000-000000000002');
    assert (select id from public.recordings where is_reference) = '20000000-0000-0000-0000-000000000002';

    assert public.delete_recording('20000000-0000-0000-0000-000000000002') = 'b.m4a', 'delete returns the file path';
    assert (select id from public.recordings where is_reference) = '20000000-0000-0000-0000-000000000004',
        'deleting the reference promotes the most recent recording';

    perform public.publish_setlist('2026-10-13', '2026-10-18', '  ', array[song]);
    assert (select count(*) from public.setlist_songs) = 1;
    assert (select message from public.setlists) is null, 'blank messages are stored as null';

    -- Weeks ahead are kept side by side; publishing a Sunday again replaces it.
    perform public.publish_setlist('2026-10-20', '2026-10-25', 'Semaine suivante', array[song]);
    perform public.publish_setlist('2026-10-13', '2026-10-18', 'Corrigée', array[song, song]);
    assert (select count(*) from public.setlists) = 2, 'one setlist per service date';
    assert (select message from public.setlists where service_date = '2026-10-18') = 'Corrigée';
    assert (select count(*) from public.setlist_songs s join public.setlists l on l.id = s.setlist_id
            where l.service_date = '2026-10-18') = 2, 'republishing replaces the songs';
    perform public.delete_setlist('2026-10-25');
    assert (select count(*) from public.setlists) = 1, 'a week can be removed';

    begin
        insert into public.invitations (email) values ('new@band.test');
        assert false, 'content admins cannot invite';
    exception when insufficient_privilege then null;
    end;
end $$;

-- Members cannot remove a week.
select pg_temp.act_as('00000000-0000-0000-0000-00000000000d');
do $$ begin
    begin
        perform public.delete_setlist('2026-10-18');
        assert false, 'members must not delete setlists';
    exception when insufficient_privilege then null;
    end;
    assert (select count(*) from public.setlists) = 1;
end $$;

-- Members record their own practice days only.
select pg_temp.act_as('00000000-0000-0000-0000-00000000000d');
do $$ begin
    insert into public.practice_days (day) values ('2026-10-12');
    insert into public.practice_days (day) values ('2026-10-12') on conflict do nothing;
    assert (select count(*) from public.practice_days) = 1, 'one row per day';
    begin
        insert into public.practice_days (member_id, day) values ('00000000-0000-0000-0000-00000000000c', '2026-10-12');
        assert false, 'members cannot record practice for someone else';
    exception when insufficient_privilege then null;
    end;
end $$;

-- Voice submissions: singers send and resend, only the super admin judges.
select pg_temp.act_as('00000000-0000-0000-0000-00000000000d');
do $$ begin
    insert into public.voice_submissions (song_id, audio_path, duration_seconds)
    values ('10000000-0000-0000-0000-000000000001', 'submissions/00000000-0000-0000-0000-00000000000d/a.wav', 42);
    begin
        update public.voice_submissions set status = 'VALIDATED';
        assert false, 'singers cannot validate themselves';
    exception when insufficient_privilege or check_violation then null;
    end;
    assert (select status from public.voice_submissions) = 'SUBMITTED';
    insert into storage.objects (bucket_id, name) values ('audio', 'submissions/00000000-0000-0000-0000-00000000000d/a.wav');
    begin
        insert into storage.objects (bucket_id, name) values ('audio', 'submissions/00000000-0000-0000-0000-00000000000c/b.wav');
        assert false, 'singers write only their own folder';
    exception when insufficient_privilege then null;
    end;
end $$;

select pg_temp.act_as('00000000-0000-0000-0000-00000000000c');
do $$ begin
    assert (select count(*) from public.voice_submissions) = 0, 'content admins do not see submissions';
    assert (select count(*) from storage.objects where name like 'submissions/%') = 0, 'nor hear them';
end $$;

select pg_temp.act_as('00000000-0000-0000-0000-00000000000a');
do $$ begin
    assert (select count(*) from storage.objects where name like 'submissions/%') = 1, 'the reviewer hears them';
    update public.voice_submissions set status = 'TO_REWORK', feedback = 'Attention au refrain', reviewed_at = now();
    assert (select status from public.voice_submissions) = 'TO_REWORK';
end $$;

-- The super admin invites and changes roles.
select pg_temp.act_as('00000000-0000-0000-0000-00000000000a');
do $$ begin
    insert into public.invitations (email, role) values ('stranger@else.test', 'MEMBER');
    update public.profiles set role = 'CONTENT_ADMIN' where email = 'member@band.test';
    assert (select count(*) from public.profiles) = 4, 'inviting an existing account creates its profile';
    assert (select role from public.profiles where email = 'member@band.test') = 'CONTENT_ADMIN';
    assert (select count(*) from public.exercise_progress) = 1, 'reviewers see every member''s progress';
end $$;

reset role;
\echo 'ALL RLS CHECKS PASSED'
