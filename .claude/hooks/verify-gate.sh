#!/bin/bash
# The Stop gate. The agent cannot finish while ./mvnw verify is red.
#
# Claude Code runs this when the agent wants to stop. The hook input arrives as
# JSON on stdin. Exit 2 with a message on stderr means "you are not done" and
# the message goes back to the agent. Exit 0 lets it stop.
set -u

INPUT=$(cat)
cd "${CLAUDE_PROJECT_DIR:-.}" || exit 0
[ -f pom.xml ] || exit 0
git rev-parse --verify -q HEAD >/dev/null || exit 0   # no baseline commit yet

TESTS=':(glob)**/src/test/**'
INPUTS=(':(glob)**/src/main/**' ':(glob)**/pom.xml' 'pom.xml')

# 1. Existing tests are the spec. They are not the agent's to change. New test files are fine.
CHANGED_TESTS=$(git diff --name-only HEAD -- "$TESTS")
if [ -n "$CHANGED_TESTS" ]; then
  {
    echo "Gate: you changed an existing test. Existing tests are the spec."
    echo "Revert the test and fix the code instead. If you think the spec is wrong, stop and say why."
    echo "$CHANGED_TESTS"
  } >&2
  exit 2
fi

# 2. Nothing to gate when the build inputs are untouched. A plan, an answer, a
#    question back to the user, a spec, or a new failing test can all stop freely.
SESSION_ID=$(printf '%s' "$INPUT" | jq -r '.session_id // "unknown"')
mkdir -p target
COUNT_FILE="target/stop-gate.${SESSION_ID}.count"
if git diff --quiet HEAD -- "${INPUTS[@]}" \
   && [ -z "$(git ls-files --others --exclude-standard -- "${INPUTS[@]}")" ]; then
  rm -f "$COUNT_FILE"
  exit 0
fi

# 3. Loop guard. Three blocks per session, then the agent may stop and must
#    report Done: no. Claude Code has its own cap, but it resets on every tool
#    call, so it bounds loops, not cost.
COUNT=$(cat "$COUNT_FILE" 2>/dev/null || echo 0)
if [ "$COUNT" -ge 3 ]; then
  rm -f "$COUNT_FILE"
  exit 0
fi

# 4. The gate itself. Prefer a JDK 25+ when the Mac tool is there, else trust JAVA_HOME.
if command -v /usr/libexec/java_home >/dev/null 2>&1; then
  JAVA_HOME="$(/usr/libexec/java_home -v 25+ 2>/dev/null || echo "${JAVA_HOME:-}")"
  export JAVA_HOME
fi
if ./mvnw -q verify > target/stop-gate.log 2>&1; then
  rm -f "$COUNT_FILE"
  exit 0
fi

echo $((COUNT + 1)) > "$COUNT_FILE"
{
  echo "Gate: ./mvnw verify failed (block $((COUNT + 1)) of 3). You are not done."
  echo "Fix the build. Do not edit or delete existing tests. Do not touch .claude/, verification/ or AGENTS.md."
  echo "The failing lines:"
  grep -E '^\[ERROR\]' target/stop-gate.log | grep -vE '^\[ERROR\] *$' | head -n 40
  echo "Full log: target/stop-gate.log"
} >&2
exit 2
