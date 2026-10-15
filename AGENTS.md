# Agent instructions

@README.md

## Commits

Always use [Conventional Commits](https://www.conventionalcommits.org/) for commit messages:
`<type>(<optional scope>): <description>`, allowed types: feat, fix, docs, style, refactor, perf, test, chore, ci, build, revert.
Enforced by `.githooks/commit-msg` (`git config core.hooksPath .githooks`) and by CI.

## Code style

Java code is formatted with Spotless (`google-java-format`). It runs automatically on edit via the Claude Code hook; CI verifies it with `./mvnw spotless:check`.

## Language policy

All code must be in English: file names, classes, methods, variables, comments. The only exception is the content of customer-facing strings (validation/error messages shown to the end user), which may be in Spanish. Checked in CI with `scripts/check-language.py`.
