#!/usr/bin/env bash
# Prints (to stdout) the git revision range covering only the commits this
# push/PR actually introduces, suitable for `git log <range>` or
# `gitleaks git --log-opts="<range>"`. Shared by the `commit-lint` and
# `security` CI jobs so neither ever rescans the whole repo history (which
# would mean a single old commit blocks every future PR forever,
# regardless of what that PR changes).
set -euo pipefail

if [ "${GITHUB_EVENT_NAME:-}" = "pull_request" ]; then
  echo "origin/${GITHUB_BASE_REF}..HEAD"
  exit 0
fi

before="${GITHUB_EVENT_BEFORE:-}"
case "$before" in
  "" | 0000000000000000000000000000000000000000*)
    # First push of a new branch: there's no "before" commit to diff from.
    echo "${GITHUB_SHA}"
    ;;
  *)
    echo "${before}..${GITHUB_SHA}"
    ;;
esac
