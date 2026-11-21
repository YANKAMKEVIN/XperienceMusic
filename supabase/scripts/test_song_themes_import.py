"""Tests of song_themes_import.py: python3 -m unittest discover -s supabase/scripts"""

import contextlib
import io
import tempfile
import unittest
from pathlib import Path

import song_themes_import as tool


class SongThemesImportTest(unittest.TestCase):
    def run_tool(self, name: str, content: str, *args: str) -> str:
        with tempfile.TemporaryDirectory() as directory:
            source = Path(directory) / name
            source.write_text(content, encoding="utf-8")
            output = io.StringIO()
            with contextlib.redirect_stdout(output), contextlib.redirect_stderr(io.StringIO()):
                tool.main([str(source), *args])
            return output.getvalue()

    def test_csv_with_accords_links_merges_and_normalizes_the_themes(self):
        sql = self.run_tool(
            "tags.csv",
            "source;tags\nhttps://accords.app/jem414;Adoration, Louange\naccords.app/JEM414/;louange,  Croix\n",
        )
        self.assertIn("('JEM414', null, array['adoration', 'croix', 'louange']::text[])", sql)
        self.assertIn("accords\\.app/", sql)
        self.assertIn("begin;", sql)
        self.assertIn("commit;", sql)

    def test_json_by_title_folds_accents_and_keeps_the_artist(self):
        sql = self.run_tool(
            "tags.json",
            '{"songs": [{"titre": "À  toi la gloire", "auteur": "E. Budry", "tags": ["Adoration"]}]}',
            "--match", "title", "--title-column", "titre", "--artist-column", "auteur",
        )
        self.assertIn("('a toi la gloire', 'e. budry', array['adoration']::text[])", sql)
        self.assertIn("translate(lower(", sql)

    def test_only_and_rename_filter_the_labels(self):
        sql = self.run_tool(
            "tags.jsonl",
            '{"source": "JEM1", "tags": "louanges|noël|Adoration"}\n',
            "--tag-separator", "|", "--rename", "louanges=louange", "--only", "louange,adoration",
        )
        self.assertIn("array['adoration', 'louange']::text[]", sql)
        self.assertNotIn("noël", sql)

    def test_quotes_are_escaped(self):
        sql = self.run_tool("tags.csv", "source,tags\nJEM2,l'amour de Dieu\n")
        self.assertIn("'l''amour de dieu'", sql)

    def test_rows_without_a_key_or_a_theme_are_skipped(self):
        sql = self.run_tool("tags.csv", "source,tags\n,adoration\nJEM3,\n")
        self.assertNotIn("insert into imported_themes", sql)

    def test_by_default_themes_are_added_and_replace_overwrites(self):
        added = self.run_tool("tags.csv", "source,tags\nJEM4,adoration\n")
        replaced = self.run_tool("tags.csv", "source,tags\nJEM4,adoration\n", "--replace")
        self.assertIn("unnest(s.themes || i.themes)", added)
        self.assertIn("set themes = i.themes", replaced)

    def test_source_code(self):
        self.assertEqual("JEM414", tool.source_code("https://accords.app/JEM414?x=1"))
        self.assertEqual("JEM414", tool.source_code("JEM414"))
        self.assertIsNone(tool.source_code("https://accords.app/"))


if __name__ == "__main__":
    unittest.main()
