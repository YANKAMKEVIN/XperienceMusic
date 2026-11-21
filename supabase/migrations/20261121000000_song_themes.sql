-- Song themes (adoration, louange, croix…), imported from the tags of Rephi / accords.app with
-- supabase/scripts/song_themes_import.py. Empty until imported: the app hides its theme filter
-- while no song has a theme, and works the same before this migration.
--
-- Read and written like the rest of the song: members read, content admins write (the policies
-- of public.songs cover the new column).

alter table public.songs
    add column themes text[] not null default '{}'
        check (array_position(themes, null) is null);
