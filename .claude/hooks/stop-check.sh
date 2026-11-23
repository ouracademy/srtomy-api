#!/usr/bin/env bash
# Stop hook: before the agent may finish a turn with uncommitted changes, the
# code must compile, be formatted and pass ArchitectureTests, and a changed
# production class must come with a changed test. On failure it
# exits 2, which keeps the agent working with the report in front of it. CI
# runs the same checks inside `./mvnw verify`, so whatever slips past here is
# stopped there.
#
# Budget: about 30-60 s (compile + one test class). The full suite stays in CI.
set -uo pipefail

project_dir="${CLAUDE_PROJECT_DIR:-$(pwd)}"
cd "$project_dir" || exit 0

# Already continuing because of this hook: let it stop, or it loops forever.
stop_hook_active="$(python3 -c '
import json, sys
try:
    print("true" if json.load(sys.stdin).get("stop_hook_active") is True else "false")
except Exception:
    print("false")
')"
[ "$stop_hook_active" = "true" ] && exit 0

# Only Java or build changes need the check.
changed="$(git status --porcelain 2>/dev/null | grep -E '\.java$|pom\.xml$' || true)"
[ -z "$changed" ] && exit 0

# Asked once: when a change truly needs no test, the agent says why and the next stop passes.
companion=""
if ! companion="$(python3 .claude/hooks/companion-tests.py 2>&1)"; then
  echo "$companion" >&2
  exit 2
fi

# -o: offline, so the hook never waits on the network; the dependencies are
# already in ~/.m2 after the first build.
if output="$(./mvnw -q -o -B spotless:check test \
  -Dtest=ArchitectureTests -Dsurefire.failIfNoSpecifiedTests=false -Djacoco.skip=true 2>&1)"; then
  exit 0
fi

{
  echo "No termines todavía: compilación, formato o ArchitectureTests fallaron."
  echo
  echo "$output" | grep -E '^\[ERROR\]|Architecture Violation|^(Class|Method|Field|Constructor) <' | head -60
  echo
  echo "Si es formato: ./mvnw spotless:apply. Si es una regla de ArchitectureTests, corrige el código."
  echo "No agregues excepciones (PENDING_MIGRATION_EXCEPTIONS, ENTITIES_WITH_RAW_IDS) ni relajes"
  echo "una regla para que pase. Si la regla no tiene sentido aquí, explícaselo al usuario."
} >&2
exit 2
