#!/usr/bin/env python3
"""Checks the Markdown docs of this repo (the same script lives in srtomy-api, srtomy-ui and
srtomy-agent). Only what needs no judgement:

1. Every path a doc names in `code` exists in the repo, or is ignored by git (build output,
   generated code), or is a subpath of a declared dependency (`langsmith/anonymizer`).
2. Every package a doc names in `code` (`@scope/name`) is a dependency in package.json (only
   where there is a package.json).
3. A doc does not repeat, word for word, a sentence of another doc.

Whether a doc says again, in other words, what another doc or a config file already says is a
judgement call: that rule is in AGENTS.md ("Docs"), for whoever writes or reviews.

Not checked: fenced code blocks, specs/ and .specify/ (they record history), lines that name
another repo (srtomy-api, srtomy-ui, srtomy-agent, ouracademy/…), and placeholders such as
`foo.ts`, `NNN-*` or `<name>`. A name a doc mentions on purpose without it existing (a file it
says not to create) goes in a comment in that doc, with the reason:
<!-- check-docs ignore: SPEC.md, utils/ (files this doc says not to create) -->

Usage: python3 scripts/check-docs.py   (exits 1 and lists the problems)
"""

import json
import os
import re
import subprocess
import sys

REPOS = ("srtomy-api", "srtomy-ui", "srtomy-agent")
SKIP_DIRS = ("specs/", ".specify/")
CODE_SPAN = re.compile(r"`([^`\n]+)`")
FENCE = re.compile(r"^(```|~~~).*?^\1", re.S | re.M)
IGNORE = re.compile(r"<!--\s*check-docs ignore:(.*?)-->", re.S)
PACKAGE = re.compile(r"^@[\w.-]+/[\w.-]+$")
EXTENSIONS = (
    "md|ts|tsx|mts|mjs|cjs|js|json|yml|yaml|sh|java|xml|toml|py|sql|properties|html|css"
)
FILE_NAME = re.compile(r"^[\w.()\[\]-]+\.(" + EXTENSIONS + r")$")
PATH = re.compile(r"^\.{0,2}[\w.()\[\]@-]*(/[\w.()\[\]@*-]*)+$")
PLACEHOLDER = re.compile(r"(^|[/.])(foo|Foo|bar|NNN\w*)([/.-]|$)|[<>{}…]")
MIN_SENTENCE = 80


def git_lines(*args):
    out = subprocess.run(["git", *args], capture_output=True, text=True, check=True).stdout
    return [line for line in out.split("\n") if line]


class Repo:
    """What exists in the repo: tracked files and their folders, and the declared dependencies."""

    def __init__(self, tracked, dependencies, ignored=lambda path: False, name=""):
        self.files = set(tracked)
        self.paths = set(tracked)
        for path in tracked:
            parts = path.split("/")
            self.paths.update("/".join(parts[:i]) for i in range(1, len(parts)))
        self.stems = {re.sub(r"\.[^/.]+$", "", path) for path in tracked}
        self.dependencies = set(dependencies)
        self.ignored = ignored
        self.name = name

    def exists(self, token, doc):
        path = token.rstrip("/")
        if path.startswith(("./", "../")):
            path = os.path.normpath(os.path.join(os.path.dirname(doc), path))
        if path in self.paths or path in self.stems:
            return True
        if "*" in path:
            rx = re.compile(
                "^" + re.escape(path).replace(r"\*\*", ".*").replace(r"\*", "[^/]*") + "$"
            )
            return any(rx.match(p) for p in self.paths)
        # A partial path or a bare file name names something somewhere in the repo.
        suffix = "/" + path
        return any(p.endswith(suffix) for p in self.paths) or any(
            s.endswith(suffix) for s in self.stems
        )

    def is_dependency_path(self, token):
        parts = token.split("/")
        name = "/".join(parts[:2]) if token.startswith("@") else parts[0]
        return name in self.dependencies


def sentences(text):
    for piece in re.split(r"(?<=[.!?])\s+|\n", text):
        piece = piece.strip(" \t-*|>#")
        if len(piece) >= MIN_SENTENCE:
            yield piece


def check(docs, repo):
    """docs: {path: text}. Returns the problems, one line each."""
    problems = []
    seen = {}
    for doc, raw in sorted(docs.items()):
        # <!-- check-docs ignore: a.md, b/ (why) --> : the reason in parentheses is for the reader.
        ignored = {
            x.strip()
            for m in IGNORE.finditer(raw)
            for x in re.sub(r"\s\(.*\)\s*$", "", m.group(1), flags=re.S).split(",")
        }
        def blank(m):
            return "\n" * m.group(0).count("\n")

        text = FENCE.sub(blank, IGNORE.sub(blank, raw))
        for number, line in enumerate(text.split("\n"), 1):
            others = [r for r in REPOS if r != repo.name]
            if "ouracademy/" in line or any(r in line for r in others):
                continue
            for match in CODE_SPAN.finditer(line):
                token = match.group(1).strip()
                if token in ignored:
                    continue
                where = f"{doc}:{number}"
                if PACKAGE.match(token) or (repo.dependencies and token in repo.dependencies):
                    if PACKAGE.match(token) and repo.dependencies and token not in repo.dependencies:
                        problems.append(f"{where}: `{token}` no es una dependencia de package.json")
                    continue
                if " " in token or "://" in token or token.startswith(("/", "-", "@/", "~", "#")):
                    continue
                if PLACEHOLDER.search(token) or (token.startswith(".") and "/" not in token):
                    continue
                if not (FILE_NAME.match(token) or PATH.match(token)):
                    continue
                if repo.exists(token, doc) or repo.ignored(token) or repo.is_dependency_path(token):
                    continue
                problems.append(f"{where}: `{token}` no existe en el repo")
        for sentence in sentences(text):
            if sentence in seen and seen[sentence] != doc:
                problems.append(
                    f"{doc}: repite una frase de {seen[sentence]} («{sentence[:60]}…»); dila una vez y enlaza"
                )
            seen.setdefault(sentence, doc)
    return problems


def load_repo():
    tracked = git_lines("ls-files")
    dependencies = set()
    if os.path.exists("package.json"):
        with open("package.json", encoding="utf-8") as f:
            manifest = json.load(f)
        for key in ("dependencies", "devDependencies", "peerDependencies"):
            dependencies.update(manifest.get(key, {}))

    folders = sorted({p.rsplit("/", 1)[0] for p in tracked if "/" in p})
    name = os.path.basename(os.getcwd())
    return Repo(tracked, dependencies, lambda token: git_ignored(token, folders), name)


def git_ignored(token, folders=()):
    """Ignored by git (build output, generated code, node_modules), whole or under a folder.

    Each candidate is asked as a file and as a folder (`dist/`): a `.gitignore` rule such as
    `dist/` only matches a folder, and git cannot tell a folder that is not on disk (a fresh
    checkout has no build output) from a file. Each prefix is asked on its own too, since past an
    ignored folder git may refuse the path (pnpm symlinks under node_modules).
    """
    path = token.rstrip("/")
    parts = path.split("/")
    prefixes = ["/".join(parts[:i]) for i in range(1, len(parts) + 1)]
    candidates = prefixes + [f"{folder}/{path}" for folder in folders]
    candidates += [c + "/" for c in candidates]
    result = subprocess.run(
        ["git", "check-ignore", "--no-index", "--stdin"],
        input="\n".join(candidates),
        capture_output=True,
        text=True,
    )
    if result.stdout.strip():
        return True
    for prefix in prefixes:
        for candidate in (prefix, prefix + "/"):
            check = subprocess.run(["git", "check-ignore", "-q", "--no-index", candidate])
            if check.returncode == 0:
                return True
    return False




def main():
    os.chdir(git_lines("rev-parse", "--show-toplevel")[0])
    repo = load_repo()
    docs = {}
    for path in sorted(repo.files):
        if path.endswith(".md") and not path.startswith(SKIP_DIRS) and "/speckit-" not in path:
            with open(path, encoding="utf-8") as f:
                docs[path] = f.read()
    problems = check(docs, repo)
    for problem in problems:
        print(problem)
    if problems:
        print(f"\n{len(problems)} problema(s) en la documentación (ver scripts/check-docs.py).")
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
