#!/bin/bash
# SessionStart hook, only in Claude Code cloud sessions (CLAUDE_CODE_REMOTE=true): what the tests
# need that a cloud container does not bring up by itself. On a developer machine it does nothing;
# there the tests use `docker compose up -d`.
set -euo pipefail
[ "${CLAUDE_CODE_REMOTE:-}" = "true" ] || exit 0
cd "${CLAUDE_PROJECT_DIR:-.}"

DATABASE=srtommy

# The JDK of .java-version (Maven refuses to compile with an older one).
wanted="$(cat .java-version)"
current="$(java -version 2>&1 | sed -nE 's/.*version "([0-9]+).*/\1/p' | head -1)"
if [ "${current:-0}" -lt "$wanted" ]; then
  if [ ! -d "/usr/lib/jvm/java-$wanted-openjdk-amd64" ]; then
    apt-get update -q && apt-get install -y -q "openjdk-$wanted-jdk-headless"
  fi
fi
if [ -n "${CLAUDE_ENV_FILE:-}" ] && [ -d "/usr/lib/jvm/java-$wanted-openjdk-amd64" ]; then
  echo "export JAVA_HOME=/usr/lib/jvm/java-$wanted-openjdk-amd64" >> "$CLAUDE_ENV_FILE"
fi

# Postgres for the tests, with the user, password and database of docker-compose.yml. A cloud
# container keeps its disk but not its processes when a session resumes, so Postgres is started
# again each time; the role and the database are created only if they are missing.
if ! command -v pg_isready >/dev/null; then
  apt-get update -q && apt-get install -y -q postgresql
fi
if ! pg_isready -q -h 127.0.0.1; then
  service postgresql start >/dev/null
  for _ in $(seq 1 30); do pg_isready -q -h 127.0.0.1 && break; sleep 1; done
fi
pg_isready -q -h 127.0.0.1

as_postgres() { su postgres -c "$1"; }
if [ "$(as_postgres "psql -tAc \"select 1 from pg_roles where rolname = 'root'\"")" != "1" ]; then
  as_postgres "psql -qc \"create role root login superuser password 'somesecretpassword'\""
fi
if [ "$(as_postgres "psql -tAc \"select 1 from pg_database where datname = '$DATABASE'\"")" != "1" ]; then
  as_postgres "createdb -O root $DATABASE"
fi
