#!/usr/bin/env python3
"""Heuristic check: fails if any .java file under src/main/java (identifiers,
comments, file/directory names) contains Spanish-specific characters
(accented vowels, ñ, ¿, ¡) outside of string literals.

Not a real Java parser - good enough to catch the real-world case (a
variable name or comment written in Spanish), not meant to be bulletproof
against pathological input (e.g. doesn't special-case Java text blocks
`\"\"\"...\"\"\"` or escaped quotes inside strings with full correctness).

Customer-facing string *content* is allowed to be in Spanish (see
AGENTS.md); this script strips string literals before scanning so it
doesn't flag them.
"""
import pathlib
import re
import sys

SPANISH_CHARS = re.compile(r"[áéíóúÁÉÍÓÚñÑ¿¡]")
STRING_LITERAL = re.compile(r'"(?:\\.|[^"\\])*"')
ROOT = pathlib.Path(__file__).resolve().parent.parent
JAVA_SRC = ROOT / "src" / "main" / "java"


def strip_string_literals(line: str) -> str:
    return STRING_LITERAL.sub('""', line)


def check_file(path: pathlib.Path) -> list[str]:
    findings = []

    if SPANISH_CHARS.search(path.name):
        findings.append(f"{path}: file name contains Spanish-specific characters")

    try:
        text = path.read_text(encoding="utf-8")
    except UnicodeDecodeError:
        findings.append(f"{path}: not valid UTF-8, skipped content check")
        return findings

    for lineno, raw_line in enumerate(text.splitlines(), start=1):
        code_only = strip_string_literals(raw_line)
        if SPANISH_CHARS.search(code_only):
            findings.append(f"{path}:{lineno}: {raw_line.strip()}")

    return findings


def main() -> int:
    if not JAVA_SRC.is_dir():
        print(f"warning: {JAVA_SRC} not found, nothing to check", file=sys.stderr)
        return 0

    all_findings: list[str] = []
    for path in sorted(JAVA_SRC.rglob("*.java")):
        all_findings.extend(check_file(path))

    if all_findings:
        print("Spanish text found in code (outside of string literals):", file=sys.stderr)
        for finding in all_findings:
            print(f"  {finding}", file=sys.stderr)
        print(
            "\nAll code (file names, identifiers, comments) must be in English. "
            "Only customer-facing string content may be in Spanish. See AGENTS.md.",
            file=sys.stderr,
        )
        return 1

    print("check-language: OK")
    return 0


if __name__ == "__main__":
    sys.exit(main())
