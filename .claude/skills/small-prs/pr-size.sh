#!/usr/bin/env bash
# Counts the lines a reviewer reads in the current branch against a base
# (default origin/main): added + deleted, without lockfiles, generated code and
# copied specs. Only committed changes count. Exits 1 when the total is above
# PR_SIZE_LIMIT (default 300).
set -euo pipefail

base="${1:-origin/main}"
limit="${PR_SIZE_LIMIT:-300}"

# Files nobody reviews line by line.
ignored='(^|/)(pnpm-lock\.yaml|package-lock\.json|yarn\.lock|uv\.lock)$|(^|/)openapi/[^/]*\.json$|(^|/)generated/|\.snap$|(^|/)mvnw(\.cmd)?$|(^|/)\.mvn/'

merge_base="$(git merge-base "$base" HEAD)"

total=0
report=""
while IFS=$'\t' read -r added deleted path; do
  [ -z "$path" ] && continue
  if [[ "$path" =~ $ignored ]]; then
    report+="$(printf '%6s  %s (ignorado)' '-' "$path")"$'\n'
    continue
  fi
  # Binary files show "-": they count as zero lines.
  [ "$added" = "-" ] && added=0
  [ "$deleted" = "-" ] && deleted=0
  lines=$((added + deleted))
  total=$((total + lines))
  report+="$(printf '%6d  %s' "$lines" "$path")"$'\n'
done < <(git diff --numstat "$merge_base" HEAD)

printf '%s' "$report" | sort -rn
echo "Total: $total líneas cambiadas (límite $limit) contra $base."

if [ "$total" -gt "$limit" ]; then
  echo "El PR es largo: pártelo o apílalo (ver .claude/skills/small-prs/SKILL.md)." >&2
  exit 1
fi
