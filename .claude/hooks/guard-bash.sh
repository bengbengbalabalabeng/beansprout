#!/bin/bash
# PreToolUse guard for the Bash tool.
#
# 1. Denies destructive and history-rewriting commands.
# 2. Before any `git commit`: denies it if an existing test was changed, if a spec
#    and its implementation are in the same commit, or if ./mvnw verify is red.
#
# The tool call arrives as JSON on stdin. A deny is JSON on stdout with exit 0.
# Exit 0 with no output means "no opinion", the normal permission flow applies.
set -u

INPUT=$(cat)
COMMAND=$(printf '%s' "$INPUT" | jq -r '.tool_input.command // empty')
[ -z "$COMMAND" ] && exit 0

deny() {
  jq -n --arg reason "$1" '{
    hookSpecificOutput: {
      hookEventName: "PreToolUse",
      permissionDecision: "deny",
      permissionDecisionReason: $reason
    }
  }'
  exit 0
}

# --- 1. Destructive commands -------------------------------------------------

if printf '%s' "$COMMAND" | grep -Eq 'git +push +(-f|--force)|git +push +[^|;&]*--force|git +reset +--hard|git +rebase|git +filter-branch|git +clean +-[a-zA-Z]*f'; then
  deny "Blocked by guard-bash.sh: history-rewriting and destructive git commands are not allowed here."
fi

if printf '%s' "$COMMAND" | grep -Eq '(^|[;&| ])rm +-[a-zA-Z]*r'; then
  # Only rm -r on build output is allowed.
  if ! printf '%s' "$COMMAND" | grep -Eq '(^|[;&| ])rm +-[a-zA-Z]*r[a-zA-Z]* +(\./)?([a-z-]+/)?target/?( *$|[;&|])'; then
    deny "Blocked by guard-bash.sh: recursive rm is only allowed on target/. Use git to revert files."
  fi
fi

if printf '%s' "$COMMAND" | grep -Eq 'sudo |curl [^|]*\| *(ba)?sh|--dangerously'; then
  deny "Blocked by guard-bash.sh: sudo, curl-pipe-to-shell, and dangerous flags are not allowed."
fi

# --- 2. Commit gate ----------------------------------------------------------

# Matches `git commit` with any of git's global options in between: -C dir, -c key=val, --git-dir=x, -q.
if printf '%s' "$COMMAND" | grep -Eq '(^|[;&| ])git +((-C|-c|--git-dir|--work-tree) +[^ ]+ +|-[^ ]+ +)*commit\b'; then
  cd "${CLAUDE_PROJECT_DIR:-.}" || exit 0
  [ -f pom.xml ] || exit 0

  if git rev-parse --verify -q HEAD >/dev/null; then
    CHANGED_TESTS=$(git diff --name-only HEAD -- ':(glob)**/src/test/**')
    if [ -n "$CHANGED_TESTS" ]; then
      deny "Commit blocked: an existing test was changed. Existing tests are the spec. Revert it and fix the code:
$CHANGED_TESTS"
    fi
  fi

  # What this commit will contain: the index, plus tracked changes when -a is used.
  STAGED=$(git diff --cached --name-only)
  if printf '%s' "$COMMAND" | grep -Eq 'commit +([^|;&]* )?(-[a-zA-Z]*a[a-zA-Z]*|--all)( |$)'; then
    STAGED="$STAGED
$(git diff --name-only)"
  fi
  if printf '%s\n' "$STAGED" | grep -q '^specs/' && printf '%s\n' "$STAGED" | grep -Eq '^[^/]+/src/main/'; then
    deny "Commit blocked: a spec and its implementation go in separate commits. Commit specs/ first, then the code."
  fi

  if command -v /usr/libexec/java_home >/dev/null 2>&1; then
    JAVA_HOME="$(/usr/libexec/java_home -v 25+ 2>/dev/null || echo "${JAVA_HOME:-}")"
    export JAVA_HOME
  fi
  OUT=$(./mvnw -q verify 2>&1)
  if [ $? -ne 0 ]; then
    LINES=$(printf '%s\n' "$OUT" | grep -E '^\[ERROR\]' | grep -vE '^\[ERROR\] *$' | head -n 40)
    deny "Commit blocked: ./mvnw verify failed. Fix the build, then commit. The failing lines:
$LINES"
  fi
fi

exit 0
