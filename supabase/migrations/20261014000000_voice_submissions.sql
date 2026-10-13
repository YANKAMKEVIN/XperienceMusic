-- "Ta voix sur le chant de la semaine": a singer records their part on a song and sends it; the
-- super admin listens and answers VALIDATED or TO_REWORK with a comment. One submission per singer
-- and song: sending again replaces it and asks for a new review.

create table public.voice_submissions (
    id               uuid primary key default gen_random_uuid(),
    member_id        uuid not null default auth.uid() references public.profiles (id) on delete cascade,
    song_id          uuid not null references public.songs (id) on delete cascade,
    audio_path       text not null,
    duration_seconds integer not null check (duration_seconds > 0),
    status           text not null default 'SUBMITTED' check (status in ('SUBMITTED', 'VALIDATED', 'TO_REWORK')),
    feedback         text check (feedback is null or length(trim(feedback)) > 0),
    submitted_at     timestamptz not null default now(),
    reviewed_at      timestamptz,
    unique (member_id, song_id)
);

create index voice_submissions_to_review on public.voice_submissions (status, submitted_at);

alter table public.voice_submissions enable row level security;

create policy "singers see their submissions, reviewers see all" on public.voice_submissions
    for select to authenticated using (member_id = auth.uid() or public.is_super_admin());

-- A singer only ever sends work for review; the verdict belongs to the super admin.
create policy "singers send their voice" on public.voice_submissions
    for insert to authenticated
    with check (member_id = auth.uid() and status = 'SUBMITTED' and feedback is null and reviewed_at is null);
create policy "singers resend, reviewers answer" on public.voice_submissions
    for update to authenticated
    using (member_id = auth.uid() or public.is_super_admin())
    with check (
        public.is_super_admin()
        or (member_id = auth.uid() and status = 'SUBMITTED' and feedback is null and reviewed_at is null)
    );
create policy "singers withdraw, reviewers clean up" on public.voice_submissions
    for delete to authenticated using (member_id = auth.uid() or public.is_super_admin());

grant select, insert, update, delete on public.voice_submissions to authenticated;

-- Their audio lives in the audio bucket under submissions/<member id>/: the singer writes their own
-- folder; only they and the super admin can listen.
drop policy "members listen to audio" on storage.objects;
create policy "members listen to audio" on storage.objects
    for select to authenticated using (
        bucket_id = 'audio' and public.is_member() and (
            name not like 'submissions/%'
            or name like 'submissions/' || auth.uid()::text || '/%'
            or public.is_super_admin()
        )
    );
create policy "singers upload their voice" on storage.objects
    for insert to authenticated
    with check (bucket_id = 'audio' and public.is_member() and name like 'submissions/' || auth.uid()::text || '/%');
create policy "singers delete their voice" on storage.objects
    for delete to authenticated
    using (bucket_id = 'audio' and name like 'submissions/' || auth.uid()::text || '/%');
