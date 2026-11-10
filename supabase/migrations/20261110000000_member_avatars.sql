-- Member avatars: either one of the avatars shipped with the app ("preset:SINGER_GIRL_1") or a
-- photo the member uploaded ("photo:<their id>/<file>.jpg"). Null shows their initials.

alter table public.profiles
    add column avatar text check (avatar is null or avatar ~ '^(preset|photo):.+$');

-- Photos live in their own private bucket, read by the band, each member writing only their folder.
insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values ('avatars', 'avatars', false, 1048576, array['image/jpeg'])
on conflict (id) do nothing;

create policy "members see avatars" on storage.objects
    for select to authenticated using (bucket_id = 'avatars' and public.is_member());
create policy "members upload their avatar" on storage.objects
    for insert to authenticated
    with check (bucket_id = 'avatars' and public.is_member() and name like auth.uid()::text || '/%');
create policy "members delete their avatar" on storage.objects
    for delete to authenticated
    using (bucket_id = 'avatars' and name like auth.uid()::text || '/%');
