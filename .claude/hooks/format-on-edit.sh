#!/usr/bin/env bash
# PostToolUse hook (matcher: Edit|Write). Formats whatever .java file was
# just touched with Spotless (google-java-format), scoped to that single
# file via spotlessFiles (matched as a regex against the absolute path).
# Fails silently so a formatter error never blocks the agent; CI's
# `spotless:check` (bound to the `verify` phase) is the real gate.
set -euo pipefail

cd "${CLAUDE_PROJECT_DIR:-.}"

file_path="$(cat | python3 -c '
import json, sys
try:
    data = json.load(sys.stdin)
    fp = (data.get("tool_input") or {}).get("file_path") or (data.get("tool_input") or {}).get("path") or ""
    print(fp, end="")
except Exception:
    pass
')"

if [ -z "$file_path" ] || [ ! -f "$file_path" ]; then
  exit 0
fi

case "$file_path" in
  *.java) ;;
  *) exit 0 ;;
esac

# Escape regex metacharacters in the path (spotlessFiles matches via
# String#matches against the absolute path) before handing it to Maven.
escaped_path="$(python3 -c '
import re, sys
print(re.escape(sys.argv[1]))
' "$file_path")"

./mvnw -q -o com.diffplug.spotless:spotless-maven-plugin:apply -DspotlessFiles="$escaped_path" >/dev/null 2>&1 || true

exit 0
