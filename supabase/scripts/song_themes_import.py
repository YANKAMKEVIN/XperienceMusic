#!/usr/bin/env python3
"""Prépare le SQL qui pose les thèmes (étiquettes de Rephi / accords.app) sur les chants.

Le script ne se connecte à aucune base : il lit un fichier d'étiquettes et écrit un fichier SQL,
que Kevin relit puis exécute lui-même dans le SQL Editor de Supabase, après la migration
`20261121000000_song_themes.sql` (voir docs/supabase-setup.md).

Le dépôt ne contient pas de source d'étiquettes : le fichier d'entrée est donc paramétrable.

Entrée, au choix :
  - CSV (séparateur détecté : virgule, point-virgule ou tabulation), une ligne par chant ;
  - JSON : une liste d'objets, ou un objet {"songs": [...]} ;
  - JSON Lines (.jsonl) : un objet par ligne.
Chaque chant a une colonne/clé d'étiquettes (une liste JSON, ou un texte séparé par --tag-separator)
et de quoi le retrouver dans la base :
  - --match source (défaut) : le lien ou le code accords.app (« JEM414 », « accords.app/JEM414 »),
    cherché dans la section « Source » que l'import accords.app écrit en bas du chant ;
  - --match title : le titre, et l'auteur si la colonne est donnée (sans casse ni accents).

Par défaut les thèmes s'ajoutent à ceux déjà posés ; --replace les remplace.

Exemple :
  python3 supabase/scripts/song_themes_import.py etiquettes.csv \\
      --source-column lien --tags-column tags --output themes.sql
"""

from __future__ import annotations

import argparse
import csv
import io
import json
import re
import sys

from dataclasses import dataclass
from pathlib import Path

# A theme is a short label: anything longer is most likely a parsing mistake.
MAX_THEME_LENGTH = 40
SOURCE_CODE = re.compile(r"(?:accords\.app/)?([A-Za-z]+[0-9]+[A-Za-z0-9-]*)\s*/?\s*$")


@dataclass(frozen=True)
class SongThemes:
    """The themes of one song, and what finds it in the database."""

    key: str
    artist: str | None
    themes: tuple[str, ...]


def normalize_theme(raw: str) -> str | None:
    """« Adoration » and « adoration  » are the same theme; blanks and odd labels are dropped."""
    theme = " ".join(raw.strip().strip("#").split()).lower()
    if not theme or len(theme) > MAX_THEME_LENGTH:
        return None
    return theme


def split_tags(value, separator: str) -> list[str]:
    if value is None:
        return []
    if isinstance(value, list):
        return [str(v) for v in value]
    text = str(value).strip()
    if text.startswith("["):
        try:
            return [str(v) for v in json.loads(text)]
        except json.JSONDecodeError:
            pass
    return text.split(separator)


def source_code(value: str) -> str | None:
    """« https://accords.app/JEM414 » → « JEM414 »; None when it is not a song of accords.app."""
    match = SOURCE_CODE.search(value.strip().split("?")[0].split("#")[0])
    return match.group(1).upper() if match else None


def read_rows(path: Path) -> list[dict]:
    text = path.read_text(encoding="utf-8-sig")
    suffix = path.suffix.lower()
    if suffix == ".jsonl":
        return [json.loads(line) for line in text.splitlines() if line.strip()]
    if suffix == ".json":
        data = json.loads(text)
        rows = data.get("songs", []) if isinstance(data, dict) else data
        if not isinstance(rows, list):
            raise ValueError("JSON : une liste d'objets ou {\"songs\": [...]} est attendue")
        return rows
    dialect = csv.Sniffer().sniff(text[:4096], delimiters=",;\t")
    return list(csv.DictReader(io.StringIO(text), dialect=dialect))


def collect(rows: list[dict], args) -> tuple[list[SongThemes], list[str]]:
    """The songs with at least one theme kept, merged by key, and a warning per skipped row."""
    merged: dict[tuple[str, str | None], set[str]] = {}
    warnings: list[str] = []
    allowed = {normalize_theme(t) for t in args.only.split(",")} if args.only else None
    renames = dict(
        (normalize_theme(a), normalize_theme(b)) for a, b in (pair.split("=", 1) for pair in args.rename)
    )
    key_column = args.source_column if args.match == "source" else args.title_column
    for number, row in enumerate(rows, start=2 if args.input.suffix.lower() == ".csv" else 1):
        raw_key = str(row.get(key_column) or "").strip()
        key = source_code(raw_key) if args.match == "source" else raw_key
        if not key:
            warnings.append(f"ligne {number} : « {key_column} » vide ou illisible ({raw_key!r}), ignorée")
            continue
        themes = set()
        for tag in split_tags(row.get(args.tags_column), args.tag_separator):
            theme = normalize_theme(tag)
            theme = renames.get(theme, theme)
            if theme and (allowed is None or theme in allowed):
                themes.add(theme)
        if not themes:
            continue
        artist = (str(row.get(args.artist_column) or "").strip() or None) if args.artist_column else None
        merged.setdefault((key, artist), set()).update(themes)
    songs = [SongThemes(key, artist, tuple(sorted(themes))) for (key, artist), themes in merged.items()]
    return sorted(songs, key=lambda s: (s.key, s.artist or "")), warnings


def sql_text(value: str) -> str:
    return "'" + value.replace("'", "''") + "'"


# The letters the search of the app folds (SearchSongsUseCase.kt), folded the same way here and in SQL.
ACCENTED = "àâäçéèêëîïôöùûüÿ’"
PLAIN = "aaaceeeeiioouuuy'"


def folded(sql_expression: str) -> str:
    """Lower case without accents, in SQL: the same folding as `fold`, without the extension `unaccent`."""
    return "translate(lower(" + sql_expression + "), " + sql_text(ACCENTED) + ", " + sql_text(PLAIN) + ")"


def fold(text: str) -> str:
    """Lower case without accents, spaces squeezed: how a title is compared."""
    return " ".join(text.split()).lower().translate(str.maketrans(ACCENTED, PLAIN))


def to_sql(songs: list[SongThemes], match: str, replace: bool, input_name: str) -> str:
    lines = [
        "-- Thèmes des chants, préparés par supabase/scripts/song_themes_import.py",
        f"-- depuis {input_name} ({len(songs)} chants). Ne touche qu'à la colonne songs.themes.",
        "-- À exécuter APRÈS la migration 20261121000000_song_themes.sql, dans le SQL Editor.",
        "begin;",
        "",
        "create temporary table imported_themes (key text not null, artist text, themes text[] not null) on commit drop;",
        "",
    ]
    if songs:
        lines.append("insert into imported_themes (key, artist, themes) values")
        values = []
        for song in songs:
            themes = "array[" + ", ".join(sql_text(t) for t in song.themes) + "]::text[]"
            key = sql_text(song.key if match == "source" else fold(song.key))
            artist = sql_text(fold(song.artist)) if song.artist else "null"
            values.append(f"    ({key}, {artist}, {themes})")
        lines.append(",\n".join(values) + ";")
        lines.append("")
    if match == "source":
        # The accords.app import ends the chart with the link of its page: « accords.app/JEM414 ».
        condition = (
            "s.chart ~* ('accords\\.app/' || i.key || '([^A-Za-z0-9]|$)')"
        )
    else:
        title = folded("regexp_replace(trim(s.title), '\\s+', ' ', 'g')")
        artist = folded("regexp_replace(trim(coalesce(s.artist, '')), '\\s+', ' ', 'g')")
        condition = f"{title} = i.key and (i.artist is null or {artist} = i.artist)"
    new_themes = (
        "i.themes"
        if replace
        else "(select coalesce(array_agg(distinct t order by t), '{}') from unnest(s.themes || i.themes) t)"
    )
    lines += [
        "update public.songs s",
        f"set themes = {new_themes}",
        "from imported_themes i",
        f"where {condition};",
        "",
        "-- What matched: songs with themes, and the imported entries that found no song.",
        "select count(*) as songs_with_themes from public.songs where themes <> '{}';",
        "select i.key, i.artist from imported_themes i",
        f"where not exists (select 1 from public.songs s where {condition});",
        "",
        "commit;",
        "",
    ]
    return "\n".join(lines)


def parse_args(argv: list[str]):
    parser = argparse.ArgumentParser(
        description="Prépare le SQL des thèmes des chants (sans jamais se connecter à la base).",
    )
    parser.add_argument("input", type=Path, help="fichier d'étiquettes : .csv, .json ou .jsonl")
    parser.add_argument("--output", type=Path, help="fichier SQL à écrire (sinon la sortie standard)")
    parser.add_argument("--match", choices=["source", "title"], default="source")
    parser.add_argument("--source-column", default="source", help="colonne du lien ou du code accords.app")
    parser.add_argument("--title-column", default="title")
    parser.add_argument("--artist-column", help="colonne de l'auteur, pour départager les titres (--match title)")
    parser.add_argument("--tags-column", default="tags")
    parser.add_argument("--tag-separator", default=",", help="séparateur des étiquettes dans un texte (défaut : virgule)")
    parser.add_argument("--only", help="ne garder que ces thèmes, séparés par des virgules")
    parser.add_argument(
        "--rename", action="append", default=[], metavar="ANCIEN=NOUVEAU",
        help="renommer une étiquette (répétable), par exemple --rename louanges=louange",
    )
    parser.add_argument("--replace", action="store_true", help="remplacer les thèmes au lieu de les ajouter")
    return parser.parse_args(argv)


def main(argv: list[str] | None = None) -> int:
    args = parse_args(sys.argv[1:] if argv is None else argv)
    songs, warnings = collect(read_rows(args.input), args)
    for warning in warnings:
        print(warning, file=sys.stderr)
    sql = to_sql(songs, args.match, args.replace, args.input.name)
    if args.output:
        args.output.write_text(sql, encoding="utf-8")
        themes = sorted({t for s in songs for t in s.themes})
        print(f"{len(songs)} chants, {len(themes)} thèmes : {', '.join(themes)}", file=sys.stderr)
        print(f"SQL écrit dans {args.output}", file=sys.stderr)
    else:
        sys.stdout.write(sql)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
