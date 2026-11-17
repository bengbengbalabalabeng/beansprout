#!/bin/bash
# Tests the "Score the change" step of .github/workflows/factory.yml, run as CI would run it.
# The step must score a PR with the base branch's script and rules, so a PR cannot lower its
# own score. Bash 3.2 compatible.
set -u

HERE=$(cd "$(dirname "$0")" && pwd)
FACTORY=$(cd "$HERE/.." && pwd)
ROOT=$(cd "$FACTORY/../.." && pwd)
WORKFLOW="${WORKFLOW_UNDER_TEST:-$ROOT/.github/workflows/factory.yml}"   # override to test a changed copy

SCRATCH=$(mktemp -d "${TMPDIR:-/tmp}/workflow-test.XXXXXX")
trap 'rm -rf "$SCRATCH"' EXIT
export HOME="$SCRATCH/home" GIT_CONFIG_NOSYSTEM=1
export GIT_AUTHOR_NAME=test GIT_AUTHOR_EMAIL=test@example.invalid
export GIT_COMMITTER_NAME=test GIT_COMMITTER_EMAIL=test@example.invalid
mkdir -p "$HOME"

PASSED=0
FAILED=0
check() { # check <name> <command...>
  local name=$1; shift
  if "$@"; then echo "  ok   $name"; PASSED=$((PASSED + 1))
  else echo "  FAIL $name"; sed 's/^/    | /' "$SUMMARY" | head -n 30; FAILED=$((FAILED + 1)); fi
}

echo "workflow: factory.yml, Score the change"

# The step's shell, cut out of the workflow: the block under `run: |` in that step.
STEP="$SCRATCH/step.sh"
awk '
  /- name: Score the change/ { found = 1; next }
  found && /^      - / { exit }
  found && /run: \|/ { body = 1; next }
  found && body { sub(/^          /, ""); print }
' "$WORKFLOW" > "$STEP"
[ -s "$STEP" ] || { echo "  FAIL could not find the step in $WORKFLOW"; exit 1; }

REPO="$SCRATCH/repo"
mkdir -p "$REPO/.shipit/factory/bin" "$REPO/app/src/main" && cd "$REPO" || exit 1
git init -q . && git symbolic-ref HEAD refs/heads/main
echo "fun main() {}" > app/src/main/Api.kt
echo "# notes" > notes.md
git add -A && git commit -q -m base

export RUNNER_TEMP="$SCRATCH/runner" GITHUB_STEP_SUMMARY="$SCRATCH/summary.md"
SUMMARY=$GITHUB_STEP_SUMMARY
mkdir -p "$RUNNER_TEMP"

run_step() { : > "$SUMMARY"; BASE_SHA=$(git rev-parse main) bash -e "$STEP" > /dev/null 2>&1; }

# 1. The PR that adds the factory: the base has none, so the PR's own copy runs.
git checkout -q -b add-factory
cp "$FACTORY/bin/risk" .shipit/factory/bin/risk
printf 'high  app/src/main/**  The public API\nlow  **.md  Notes\n' > .shipit/factory/risk.txt
echo "fun main() { println() }" > app/src/main/Api.kt
git add -A && git commit -q -m "add the factory"
run_step
check "the first factory PR runs its own rules" grep -q "Script and rules from this pull request" "$SUMMARY"
check "and still scores the change" grep -qx "## Risk: high" "$SUMMARY"

# 2. A later PR edits the rules to lower its own score. The base rules still run.
git checkout -q main && git merge -q --ff-only add-factory
git checkout -q -b sneaky
printf 'low  **  Nothing to see\n' > .shipit/factory/risk.txt
echo "fun main() { println(1) }" > app/src/main/Api.kt
git add -A && git commit -q -m "lower my own score"
run_step
check "a PR cannot lower its own score" grep -qx "## Risk: high" "$SUMMARY"
check "the summary says the base rules ran" grep -q "Script and rules from the base branch" "$SUMMARY"
check "the PR's own rules did not run" sh -c "! grep -q 'Nothing to see' '$SUMMARY'"

# 3. A docs-only PR is low.
git checkout -q main && git checkout -q -b docs
echo "# more notes" > notes.md
git add -A && git commit -q -m "notes"
run_step
check "a docs-only PR is low" grep -qx "## Risk: low" "$SUMMARY"

echo
echo "workflow: $PASSED passed, $FAILED failed"
[ "$FAILED" -eq 0 ]
