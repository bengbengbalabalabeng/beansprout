#!/bin/bash
# Tests for bin/risk. Each test builds a small git repo in a temp folder, makes a change on a
# branch, and runs risk against main. Bash 3.2 compatible, like the script itself.
#
# Run all factory tests with: .shipit/factory/test/run
set -u

HERE=$(cd "$(dirname "$0")" && pwd)
FACTORY=$(cd "$HERE/.." && pwd)
RISK="${RISK_UNDER_TEST:-$FACTORY/bin/risk}"   # override to test a changed copy
PROJECT="$FACTORY/risk.txt"

# Keep git away from the user's own config, hooks and signing.
SCRATCH=$(mktemp -d "${TMPDIR:-/tmp}/risk-test.XXXXXX")
trap 'rm -rf "$SCRATCH"' EXIT
export HOME="$SCRATCH/home" GIT_CONFIG_NOSYSTEM=1
export GIT_AUTHOR_NAME=test GIT_AUTHOR_EMAIL=test@example.invalid
export GIT_COMMITTER_NAME=test GIT_COMMITTER_EMAIL=test@example.invalid
mkdir -p "$HOME"

PASSED=0
FAILED=0
CURRENT=""
OUT=""
CODE=0

fail() {
  echo "  FAIL $CURRENT: $1"
  printf '%s\n' "$OUT" | sed 's/^/    | /' | head -n 40
  FAILED=$((FAILED + 1))
  CURRENT=""
}

pass() {
  [ -n "$CURRENT" ] || return 0
  echo "  ok   $CURRENT"
  PASSED=$((PASSED + 1))
}

# --- Fixture helpers ---------------------------------------------------------

put() { # put <path> [content]: write a file, making its folders
  mkdir -p "$(dirname "$1")"
  printf '%s\n' "${2:-content of $1}" > "$1"
}

commit() { git add -A && git commit -q -m "$1"; }

# A repo shaped like Beansprout: a parent pom, modules, a theme, specs, docs and tests.
beansprout_repo() {
  REPO="$SCRATCH/$1"
  mkdir -p "$REPO" && cd "$REPO" || exit 1
  git init -q . && git symbolic-ref HEAD refs/heads/main
  put pom.xml '<project><modules><module>beansprout-core</module></modules></project>'
  put README.md "# Beansprout"
  put PRODUCT.md "# Product"
  put docs/deploy.md "Deploy it."
  put specs/posts.md "| POSTS-1 | a post |"
  put beansprout-core/pom.xml '<project><dependencies>
<dependency><artifactId>commonmark</artifactId></dependency>
</dependencies></project>'
  put beansprout-core/src/main/java/dev/danvega/beansprout/view/Post.java "record Post() {}"
  put beansprout-core/src/main/java/dev/danvega/beansprout/content/Posts.java "class Posts {}"
  put beansprout-core/src/test/java/dev/danvega/beansprout/content/PostsTests.java "class PostsTests {}"
  put beansprout-spring-boot-autoconfigure/pom.xml '<project></project>'
  put beansprout-spring-boot-autoconfigure/src/main/java/dev/danvega/beansprout/autoconfigure/BeansproutProperties.java "record P() {}"
  put beansprout-spring-boot-autoconfigure/src/main/java/dev/danvega/beansprout/web/BlogController.java "class C {}"
  put themes/beansprout-theme-longhand/pom.xml '<project></project>'
  put themes/beansprout-theme-longhand/src/main/jte/post.jte "<p>post</p>"
  put .github/workflows/verify.yml "name: verify"
  commit "base"
  git checkout -q -b change
}

# A bare repo with a small kit config, for the generic behavior.
kit_repo() {
  REPO="$SCRATCH/$1"
  mkdir -p "$REPO" && cd "$REPO" || exit 1
  git init -q . && git symbolic-ref HEAD refs/heads/main
  put app/build.gradle "plugins {}"
  put app/src/main/App.kt "fun main() {}"
  put lib/build.gradle "plugins {}"
  put lib/src/main/Lib.kt "object Lib"
  put notes/a.txt "a"
  commit "base"
  git checkout -q -b change
}

run_risk() { # run_risk [args...]: capture output and exit code
  OUT=$(bash "$RISK" "$@" 2>&1)
  CODE=$?
}

test_case() { pass; CURRENT=$1; }

expect_code() { [ "$CODE" -eq "$1" ] || fail "exit code $CODE, wanted $1"; }

expect_level() {
  [ -n "$CURRENT" ] || return 0
  expect_code 0
  [ -n "$CURRENT" ] || return 0
  printf '%s\n' "$OUT" | head -n 1 | grep -qx "## Risk: $1" || fail "level is not $1"
}

expect_text() {
  [ -n "$CURRENT" ] || return 0
  printf '%s\n' "$OUT" | grep -qF -- "$1" || fail "missing: $1"
}

expect_no_text() {
  [ -n "$CURRENT" ] || return 0
  printf '%s\n' "$OUT" | grep -qF -- "$1" && fail "unexpected: $1"
  return 0
}

# A reason's heading must sit under the given level's heading.
expect_under() { # expect_under <High|Medium|Low> <reason>
  [ -n "$CURRENT" ] || return 0
  local section
  section=$(printf '%s\n' "$OUT" | awk -v want="**$2**" '
    /^### / { s = $2; sub(":", "", s) }
    $0 == want { print s; exit }')
  [ "$section" = "$1" ] || fail "'$2' is under '${section:-nothing}', wanted $1"
}

# --- Project rules: Beansprout's risk.txt ------------------------------------

echo "risk: project rules (risk.txt)"

test_case "a docs-only change stays low"
beansprout_repo docs
put docs/deploy.md "Deploy it to Railway."
put docs/new-blog.md "Start a blog."
put README.md "# Beansprout, now with more words"
commit "docs"
run_risk --config "$PROJECT" main
expect_level low
expect_under Low "Docs"
expect_text '`docs/new-blog.md`'
expect_no_text "### High"
expect_no_text "### Medium"

test_case "a *Properties.java change is high"
beansprout_repo props
put beansprout-spring-boot-autoconfigure/src/main/java/dev/danvega/beansprout/autoconfigure/BeansproutProperties.java "record P(String title) {}"
commit "props"
run_risk --config "$PROJECT" main
expect_level high
expect_under High "Public beansprout.* property names"
expect_text "BeansproutProperties.java"

test_case "an auto-configuration and its imports file are high"
beansprout_repo autoconf
put beansprout-spring-boot-autoconfigure/src/main/java/dev/danvega/beansprout/autoconfigure/BeansproutMcpAutoConfiguration.java "class A {}"
put beansprout-spring-boot-autoconfigure/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports "x.A"
commit "autoconf"
run_risk --config "$PROJECT" main
expect_level high
expect_under High "Auto-configuration: what an app gets with one dependency"
expect_text "AutoConfiguration.imports"

test_case "the view package, the web package and mcp are high"
beansprout_repo contract
put beansprout-core/src/main/java/dev/danvega/beansprout/view/Post.java "record Post(String title) {}"
put beansprout-spring-boot-autoconfigure/src/main/java/dev/danvega/beansprout/web/BlogController.java "class C { }"
put beansprout-spring-boot-autoconfigure/src/main/java/dev/danvega/beansprout/mcp/DraftTool.java "class D {}"
commit "contract"
run_risk --config "$PROJECT" main
expect_level high
expect_under High "The view contract that every theme builds on"
expect_under High "Routes and controllers: public URLs"
expect_under High "MCP: the server must never publish"

test_case "the release files are high"
beansprout_repo release
put .github/workflows/release.yml "name: release"
put RELEASING.md "Release it."
put beansprout-bom/pom.xml "<project/>"
commit "release"
run_risk --config "$PROJECT" main
expect_level high
expect_under High "The release to Maven Central"
expect_text '`.github/workflows/release.yml`'
expect_text '`RELEASING.md`'

test_case "gate files are high"
beansprout_repo gates
put AGENTS.md "Rules."
put .claude/hooks/verify-gate.sh "exit 0"
put verification/src/test/java/Judges.java "class J {}"
put .shipit/factory/risk.txt "low docs/** Docs"
put .github/workflows/verify.yml "name: verify, changed"
commit "gates"
run_risk --config "$PROJECT" main
expect_level high
expect_under High "A gate or the factory itself"
expect_text '`.claude/hooks/verify-gate.sh`'
expect_text '`.github/workflows/verify.yml`'

test_case "a spec, PRODUCT.md, a theme template and a pom without dependencies list under medium"
beansprout_repo medium
put specs/posts.md "| POSTS-1 | a post, changed |"
put PRODUCT.md "# Product, changed"
put themes/beansprout-theme-longhand/src/main/jte/post.jte "<p>post, changed</p>"
put beansprout-core/pom.xml '<project><dependencies>
<dependency><artifactId>commonmark</artifactId></dependency>
</dependencies><name>core</name></project>'
commit "medium"
run_risk --config "$PROJECT" main
expect_level high   # three modules touched: core, theme, and the change is real
expect_under Medium "Specs: the requirements Dan approves"
expect_under Medium "Product scope and decisions"
expect_under Medium "Theme templates and styles"
expect_under Medium "Build files"
expect_no_text "A new \`<dependency>\`"

test_case "a single medium file is medium overall"
beansprout_repo medium-only
put specs/posts.md "| POSTS-1 | a post, changed |"
commit "spec"
run_risk --config "$PROJECT" main
expect_level medium

test_case "code no rule names gets the default level"
beansprout_repo default
put beansprout-core/src/main/java/dev/danvega/beansprout/content/Posts.java "class Posts { int n; }"
commit "code"
run_risk --config "$PROJECT" main
expect_level medium
expect_under Medium "Code no rule names"

test_case "a new test file is low"
beansprout_repo newtest
put beansprout-core/src/test/java/dev/danvega/beansprout/content/MoreTests.java "class MoreTests {}"
commit "new test"
run_risk --config "$PROJECT" main
expect_level low
expect_under Low "New tests and test fixtures"
expect_no_text "An existing test was changed"

test_case "a modified existing test is high"
beansprout_repo modtest
put beansprout-core/src/test/java/dev/danvega/beansprout/content/PostsTests.java "class PostsTests { /* weaker */ }"
commit "weaken a test"
run_risk --config "$PROJECT" main
expect_level high
expect_under High "An existing test was changed or deleted. Is the old behavior still checked?"
expect_text "PostsTests.java\` (modified)"

test_case "a deleted existing test is high"
beansprout_repo deltest
git rm -q beansprout-core/src/test/java/dev/danvega/beansprout/content/PostsTests.java
commit "delete a test"
run_risk --config "$PROJECT" main
expect_level high
expect_text "PostsTests.java\` (deleted)"

test_case "a test moved out of src/test is high"
beansprout_repo movetest
mkdir -p beansprout-core/attic
git mv beansprout-core/src/test/java/dev/danvega/beansprout/content/PostsTests.java beansprout-core/attic/PostsTests.java
commit "hide a test"
run_risk --config "$PROJECT" main
expect_level high
expect_text "(moved out to \`beansprout-core/attic/PostsTests.java\`)"

test_case "more than one Maven module is high"
beansprout_repo modules
put beansprout-core/src/main/java/dev/danvega/beansprout/content/Posts.java "class Posts { int n; }"
put themes/beansprout-theme-longhand/src/main/jte/post.jte "<p>post, changed</p>"
commit "two modules"
run_risk --config "$PROJECT" main
expect_level high
expect_under High "The change touches more than one Maven module"
expect_text '`beansprout-core/`'
expect_text '`themes/beansprout-theme-longhand/`'

test_case "files at the repo root are in no module"
beansprout_repo root-files
put README.md "# changed"
put beansprout-core/src/main/java/dev/danvega/beansprout/content/Posts.java "class Posts { int n; }"
commit "one module and the root"
run_risk --config "$PROJECT" main
expect_no_text "more than one Maven module"

test_case "a new <dependency> in a pom is high"
beansprout_repo dep
put beansprout-core/pom.xml '<project><dependencies>
<dependency><artifactId>commonmark</artifactId></dependency>
<dependency><artifactId>jsoup</artifactId></dependency>
</dependencies></project>'
commit "add jsoup"
run_risk --config "$PROJECT" main
expect_level high
expect_under High "A new \`<dependency>\` in a pom. AGENTS.md says ask first."
expect_text '`beansprout-core/pom.xml` (1 added)'

test_case "a commit with a spec and src/main together is medium"
beansprout_repo mixed
put specs/posts.md "| POSTS-1 | a post, changed |"
put beansprout-core/src/main/java/dev/danvega/beansprout/content/Posts.java "class Posts { int n; }"
commit "spec and code at once"
run_risk --config "$PROJECT" main
expect_level medium
expect_under Medium "A commit holds both a spec and its implementation. AGENTS.md wants them apart."
expect_text "spec and code at once"

test_case "a spec and its code in separate commits is fine"
beansprout_repo separate
put specs/posts.md "| POSTS-1 | a post, changed |"
commit "spec"
put beansprout-core/src/main/java/dev/danvega/beansprout/content/Posts.java "class Posts { int n; }"
commit "code"
run_risk --config "$PROJECT" main
expect_level medium
expect_no_text "A commit holds both"

test_case "a theme module's src/main counts for the mixed-commit check"
beansprout_repo mixed-theme
put specs/posts.md "| POSTS-1 | a post, changed |"
put themes/beansprout-theme-longhand/src/main/jte/post.jte "<p>post, changed</p>"
commit "spec and theme at once"
run_risk --config "$PROJECT" main
expect_text "spec and theme at once"

test_case "a diff over the size limit is medium"
beansprout_repo big
i=0; while [ $i -lt 900 ]; do echo "line $i"; i=$((i + 1)); done > docs/big.md
commit "a long doc"
run_risk --config "$PROJECT" main
expect_level medium
expect_under Medium "The diff is large"
expect_text "900 lines changed, limit 800"

test_case "the highest level wins"
beansprout_repo highest
put docs/deploy.md "changed"
put specs/posts.md "| POSTS-1 | changed |"
put beansprout-spring-boot-autoconfigure/src/main/java/dev/danvega/beansprout/autoconfigure/BeansproutProperties.java "record P(int n) {}"
commit "all three"
run_risk --config "$PROJECT" main
expect_level high
expect_under High "Public beansprout.* property names"
expect_under Medium "Specs: the requirements Dan approves"
expect_under Low "Docs"

# --- Kit behavior: a small config for a non-Java project ---------------------

pass; CURRENT=""
echo "risk: kit behavior"

KIT="$SCRATCH/kit.txt"
cat > "$KIT" <<'EOF'
# A Gradle project, to show nothing in the kit is Maven only.
high    **/*.gradle            Build logic
high    lib/src/main/*.kt      The library's API
low     notes/**               Notes
check   high  multi-module     More than one module
check   medium size            Big
set default-level low
set default-reason Everything else
set module-files build.gradle
set size-limit 5
set list-limit 2
EOF

test_case "a glob star does not cross a slash"
kit_repo star
put lib/src/main/deep/Inner.kt "object Inner"
commit "deep"
run_risk --config "$KIT" main
expect_level low
expect_under Low "Everything else"

test_case "a double star matches zero folders"
kit_repo dstar
put lib/build.gradle "plugins { id 'x' }"
commit "build"
run_risk --config "$KIT" main
expect_level high
expect_under High "Build logic"

test_case "module files come from the config"
kit_repo gradle-modules
put app/src/main/App.kt "fun main() { }"
put lib/src/main/deep/Inner.kt "object Inner"
commit "two modules"
run_risk --config "$KIT" main
expect_level high
expect_under High "More than one module"
expect_text '`app/`'
expect_text '`lib/`'

test_case "an unconfigured check stays off"
kit_repo nocheck
mkdir -p app/src/test && put app/src/test/AppTest.kt "test"
commit "add"
put app/src/test/AppTest.kt "test, weaker"
commit "change a test"
run_risk --config "$KIT" main
expect_no_text "existing test"

test_case "list-limit caps a long list"
kit_repo listlimit
put notes/b.txt b; put notes/c.txt c; put notes/d.txt d
commit "notes"
run_risk --config "$KIT" main
expect_text "- and 1 more"

test_case "--level prints only the level"
kit_repo levelonly
put lib/build.gradle "plugins { id 'y' }"
commit "build"
run_risk --config "$KIT" --level main
expect_code 0
[ "$OUT" = "high" ] || fail "printed '$OUT'"

test_case "--worktree counts uncommitted and untracked files"
kit_repo worktree
put lib/build.gradle "plugins { id 'z' }"
put notes/new.txt "untracked"
run_risk --config "$KIT" main
expect_level low
expect_text "No changes in this range."
run_risk --config "$KIT" --worktree main
expect_level high
expect_text '`lib/build.gradle`'
expect_text '`notes/new.txt`'

test_case "--worktree counts the uncommitted work as a commit for the mixed check"
beansprout_repo worktree-mixed
put specs/posts.md "| POSTS-1 | changed |"
put beansprout-core/src/main/java/dev/danvega/beansprout/content/New.java "class New {}"
run_risk --config "$PROJECT" --worktree main
expect_text "uncommitted work"

test_case "paths with spaces and accents are listed as written"
kit_repo names
put "notes/My Café Notes.txt" "hi"
commit "names"
run_risk --config "$KIT" main
expect_text '`notes/My Café Notes.txt`'

test_case "an empty range is low"
kit_repo empty
run_risk --config "$KIT" main
expect_level low
expect_text "0 commits, 0 files"

# --- Exit codes: risk never fails on risk, only on a broken setup ------------

pass; CURRENT=""
echo "risk: exit codes"

test_case "a high risk change still exits 0"
kit_repo exit-high
put lib/build.gradle "plugins { id 'w' }"
commit "build"
run_risk --config "$KIT" main
expect_code 0

test_case "a line with an unknown level exits 2"
kit_repo badlevel
printf 'urgent  **  Everything\n' > "$SCRATCH/bad1.txt"
run_risk --config "$SCRATCH/bad1.txt" main
expect_code 2
expect_text "bad1.txt:1:"

test_case "an unknown check exits 2"
kit_repo badcheck
printf 'check high vibes Bad vibes\n' > "$SCRATCH/bad2.txt"
run_risk --config "$SCRATCH/bad2.txt" main
expect_code 2
expect_text "unknown check 'vibes'"

test_case "a rule with no reason exits 2"
kit_repo noreason
printf '# fine\nhigh  **/*.gradle\n' > "$SCRATCH/bad3.txt"
run_risk --config "$SCRATCH/bad3.txt" main
expect_code 2
expect_text "bad3.txt:2:"

test_case "a size-limit that is not a number exits 2"
kit_repo badsize
printf 'set size-limit lots\n' > "$SCRATCH/bad4.txt"
run_risk --config "$SCRATCH/bad4.txt" main
expect_code 2

test_case "a missing config exits 2"
kit_repo noconfig
run_risk --config "$SCRATCH/none.txt" main
expect_code 2

test_case "an unknown base ref exits 3"
kit_repo badref
run_risk --config "$KIT" no-such-branch
expect_code 3
expect_text "unknown base ref"

test_case "outside a git repo exits 3"
mkdir -p "$SCRATCH/not-a-repo" && cd "$SCRATCH/not-a-repo" || exit 1
run_risk --config "$KIT" main
expect_code 3

pass
echo
echo "risk: $PASSED passed, $FAILED failed"
[ "$FAILED" -eq 0 ]
