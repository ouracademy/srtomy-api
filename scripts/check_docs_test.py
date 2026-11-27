"""Tests of scripts/check-docs.py: python3 -m unittest discover -s scripts -p 'check_docs_test.py'"""

import importlib.util
import os
import subprocess
import tempfile
import unittest

spec = importlib.util.spec_from_file_location(
    "check_docs", os.path.join(os.path.dirname(__file__), "check-docs.py")
)
check_docs = importlib.util.module_from_spec(spec)
spec.loader.exec_module(check_docs)

REPO = check_docs.Repo(
    ["src/app.ts", "src/tools/search.ts", "docs/guide.md", "docs/adr/README.md", "package.json"],
    {"langsmith", "@testing-library/user-event"},
    ignored=lambda path: path.startswith("dist"),
    name="srtomy-agent",
)


def problems(text, doc="docs/guide.md"):
    return check_docs.check({doc: text}, REPO)


class References(unittest.TestCase):
    def test_accepts_paths_that_exist_whole_partial_or_bare(self):
        self.assertEqual(problems("`src/app.ts`, `tools/search.ts`, `search.ts`, `src/tools/`"), [])

    def test_accepts_relative_paths_from_the_doc(self):
        self.assertEqual(problems("[x](../guide.md) `../guide.md`", doc="docs/adr/README.md"), [])

    def test_refuses_a_path_that_does_not_exist(self):
        self.assertEqual(
            problems("Las llamadas pasan por `src/services/api.ts`."),
            ["docs/guide.md:1: `src/services/api.ts` no existe en el repo"],
        )

    def test_accepts_build_output_dependency_subpaths_and_placeholders(self):
        self.assertEqual(problems("`dist/main.js` `langsmith/anonymizer` `foo.ts` `specs/NNN-*`"), [])

    def test_counts_lines_after_a_code_block(self):
        self.assertEqual(
            problems("```\na\nb\n```\n`src/missing.ts`"),
            ["docs/guide.md:5: `src/missing.ts` no existe en el repo"],
        )

    def test_skips_code_blocks_other_repos_and_ignored_names(self):
        text = (
            "```\nsrc/missing.ts\n```\n"
            "En srtomy-ui, `app/page.tsx`.\n"
            "<!-- check-docs ignore: SPEC.md, app/(x)/y.ts (no se crean) -->\nNo escribas `SPEC.md` ni `app/(x)/y.ts`.\n"
        )
        self.assertEqual(problems(text), [])


class Packages(unittest.TestCase):
    def test_refuses_a_package_that_is_not_a_dependency(self):
        self.assertEqual(
            problems("Usa `@hello-pangea/dnd`."),
            ["docs/guide.md:1: `@hello-pangea/dnd` no es una dependencia de package.json"],
        )

    def test_accepts_naming_a_dependency_for_what_it_does(self):
        self.assertEqual(problems("Prefiere `@testing-library/user-event` para tipear."), [])


class Repetition(unittest.TestCase):
    SENTENCE = "Todo cambio de lógica trae su prueba, junto al archivo que prueba, y el hook la pide."

    def test_refuses_a_sentence_repeated_in_two_docs(self):
        found = check_docs.check({"a.md": self.SENTENCE, "b.md": self.SENTENCE}, REPO)
        self.assertEqual(len(found), 1)
        self.assertIn("b.md: repite una frase de a.md", found[0])

    def test_accepts_short_or_distinct_sentences(self):
        self.assertEqual(check_docs.check({"a.md": "Corre `pnpm check`.", "b.md": "Corre `pnpm check`."}, REPO), [])


class Ignored(unittest.TestCase):
    """git_ignored asks the real git, in a throwaway repo."""

    def setUp(self):
        self.cwd = os.getcwd()
        self.dir = tempfile.TemporaryDirectory()
        os.chdir(self.dir.name)
        subprocess.run(["git", "init", "-q"], check=True)
        with open(".gitignore", "w") as f:
            f.write("dist/\nnode_modules\n")

    def tearDown(self):
        os.chdir(self.cwd)
        self.dir.cleanup()

    def test_a_folder_rule_matches_a_folder_that_is_not_on_disk(self):
        self.assertTrue(check_docs.git_ignored("dist/"))
        self.assertTrue(check_docs.git_ignored("dist/main.js"))

    def test_a_path_under_an_ignored_folder(self):
        self.assertTrue(check_docs.git_ignored("node_modules/ai/docs"))

    def test_a_path_that_is_not_ignored(self):
        self.assertFalse(check_docs.git_ignored("src/app.ts"))


if __name__ == "__main__":
    unittest.main()
