#!/usr/bin/env bash
# Shared Conventional Commits validation, sourced by:
#   - .githooks/commit-msg (real git hook, for human commits)
#   - .claude/hooks/validate-commit-msg.sh (Claude Code hook, for Claude's own commits)
#   - .github/workflows/ci.yml, job `commit-lint` (safety net for commits made
#     through the GitHub UI or any other path that skips both hooks)
#
# https://www.conventionalcommits.org/
CONVENTIONAL_COMMIT_REGEX='^(feat|fix|docs|style|refactor|perf|test|chore|ci|build|revert)(\([a-z0-9/_-]+\))?!?: .{1,100}$'

# validate_commit_subject <subject line>
# Prints an error to stderr and returns 1 if the subject does not follow
# Conventional Commits; returns 0 otherwise.
validate_commit_subject() {
  local subject="$1"

  # Merge commits and fixup/squash commits are not expected to follow the
  # convention; let them through.
  case "$subject" in
    "Merge "* | "Revert \""* | fixup!\ * | squash!\ *)
      return 0
      ;;
  esac

  if [[ "$subject" =~ $CONVENTIONAL_COMMIT_REGEX ]]; then
    return 0
  fi

  echo "Commit message does not follow Conventional Commits: \"$subject\"" >&2
  echo "Expected format: <type>(<optional scope>): <description>" >&2
  echo "Allowed types: feat, fix, docs, style, refactor, perf, test, chore, ci, build, revert" >&2
  echo "See https://www.conventionalcommits.org/" >&2
  return 1
}
