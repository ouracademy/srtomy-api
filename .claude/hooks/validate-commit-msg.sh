#!/usr/bin/env bash
# PreToolUse hook (matcher: Bash). Blocks `git commit` tool calls whose
# message(s) don't follow Conventional Commits, so Claude fixes it and
# retries instead of creating a bad commit. Reads the hook JSON payload
# from stdin (same payload format used by Bash tool calls).
#
# This is a second line of defense on top of the real git hook
# (.githooks/commit-msg, for humans) and the CI `commit-lint` job (for
# anything that slips past both hooks); all three share the same
# Conventional Commits rule from .githooks/lib/validate-message.sh.
set -euo pipefail

cd "${CLAUDE_PROJECT_DIR:-.}"

payload="$(cat)"

subjects="$(printf '%s' "$payload" | python3 "$CLAUDE_PROJECT_DIR/.claude/hooks/extract_commit_subjects.py" 2>/dev/null || true)"

if [ -z "$subjects" ]; then
  exit 0
fi

# shellcheck source=../../.githooks/lib/validate-message.sh
source "$CLAUDE_PROJECT_DIR/.githooks/lib/validate-message.sh"

failed=0
while IFS= read -r subject; do
  [ -z "$subject" ] && continue
  if ! validate_commit_subject "$subject"; then
    failed=1
  fi
done <<< "$subjects"

if [ "$failed" -eq 1 ]; then
  exit 2
fi

exit 0
