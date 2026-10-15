#!/usr/bin/env python3
"""Reads a Claude Code PreToolUse hook payload from stdin and, if the Bash
command being run is one or more `git commit ... -m "..."` invocations,
prints each commit subject (first line of each -m/--message value) on its
own line to stdout. Prints nothing if there's no git commit to check.

Kept separate from the shell script so command parsing goes through
shlex (handles quoting/heredocs correctly) instead of a naive regex over
the raw command string.
"""
import json
import shlex
import sys


def split_on_control_operators(command: str) -> list[list[str]]:
    """Split a shell command string into a list of token-lists, one per
    subcommand, breaking on &&, ||, ;, |, and newlines."""
    lexer = shlex.shlex(command, posix=True, punctuation_chars=True)
    lexer.whitespace_split = True
    tokens = list(lexer)

    control_operators = {"&&", "||", ";", "|", "&", "\n"}
    subcommands: list[list[str]] = [[]]
    for token in tokens:
        if token in control_operators:
            subcommands.append([])
        else:
            subcommands[-1].append(token)
    return [sub for sub in subcommands if sub]


def extract_subjects(tokens: list[str]) -> list[str]:
    if "git" not in tokens or "commit" not in tokens:
        return []
    if tokens[0] != "git":
        return []

    subjects = []
    i = 0
    while i < len(tokens):
        token = tokens[i]
        value = None
        if token in ("-m", "--message"):
            if i + 1 < len(tokens):
                value = tokens[i + 1]
                i += 1
        elif token.startswith("--message="):
            value = token[len("--message=") :]
        elif token.startswith("-m") and len(token) > 2:
            value = token[2:]

        if value is not None:
            first_line = value.splitlines()[0] if value else ""
            if first_line:
                subjects.append(first_line)
        i += 1

    return subjects


def main() -> int:
    try:
        payload = json.load(sys.stdin)
    except (json.JSONDecodeError, ValueError):
        return 0

    if payload.get("tool_name") != "Bash":
        return 0

    command = (payload.get("tool_input") or {}).get("command") or ""
    if not command:
        return 0

    try:
        subcommands = split_on_control_operators(command)
    except ValueError:
        # Unbalanced quotes or similar; don't block on something we can't parse.
        return 0

    for tokens in subcommands:
        for subject in extract_subjects(tokens):
            print(subject)

    return 0


if __name__ == "__main__":
    sys.exit(main())
