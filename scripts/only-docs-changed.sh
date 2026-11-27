#!/usr/bin/env bash
# Exits 0 when every file changed between <base> and HEAD is documentation (a *.md file or
# anything under docs/), 1 otherwise. CI and the pre-push hook use it to skip the tests, the
# build and the e2e of a change that only touches docs; the docs checks still run.
# An empty diff counts as code, so nothing is skipped by accident.
set -euo pipefail

base="${1:?usage: scripts/only-docs-changed.sh <base-ref>}"
changed="$(git diff --name-only "$base"...HEAD)"

[ -n "$changed" ] || exit 1
if printf '%s\n' "$changed" | grep -qvE '(\.md$|^docs/)'; then
  exit 1
fi
exit 0
