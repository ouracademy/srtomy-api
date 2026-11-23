"""Lists the uncommitted production classes that no uncommitted test touches.

Used by the Stop hook (stop-check.sh): a changed class under src/main/java needs a changed test
under src/test/java that has its name (FooTests, FooIntegrationTests for Foo) or mentions it. The
level of that test is the test-pyramid skill's call. Exits 1 and prints the classes when some
have none.
"""

import re
import subprocess
import sys
from pathlib import Path

MAIN = "src/main/java/"
TEST = "src/test/java/"
# Wiring and data that carry no logic of their own.
EXEMPT = re.compile(r"(package-info|module-info|Application)\.java$")


def uncommitted() -> list[str]:
    status = subprocess.run(
        ["git", "status", "--porcelain", "--untracked-files=all"],
        capture_output=True,
        text=True,
        check=True,
    ).stdout
    paths = []
    for line in status.splitlines():
        if len(line) <= 3 or "D" in line[:2]:
            continue
        paths.append(line[3:].split(" -> ")[-1])
    return paths


def missing(changed: list[str], read) -> list[str]:
    tests = [p for p in changed if p.startswith(TEST) and p.endswith(".java")]
    contents = [read(p) for p in tests]
    result = []
    for path in changed:
        if not (path.startswith(MAIN) and path.endswith(".java")) or EXEMPT.search(path):
            continue
        name = Path(path).stem
        named = any(Path(t).stem.startswith(name) for t in tests)
        mentioned = any(re.search(rf"\b{re.escape(name)}\b", c) for c in contents)
        if not named and not mentioned:
            result.append(path)
    return result


if __name__ == "__main__":
    found = missing(uncommitted(), lambda p: Path(p).read_text(encoding="utf-8"))
    if found:
        print("Estas clases cambiaron y ninguna prueba cambiada las toca:")
        for path in found:
            print(f"  {path}")
        print("Agrega o cambia su prueba en el nivel que pide el skill test-pyramid")
        print("(.claude/skills/test-pyramid/SKILL.md). Si de verdad no hace falta (solo un")
        print("comentario, un renombre), dile a la persona por qué.")
        sys.exit(1)
