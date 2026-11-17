# The Beansprout factory

The factory checks a change and tells Dan what to read. It automates the known, not the thinking.
Specs and priority stay with Dan.

Four pieces are here:

1. **Risk analyzer.** Path rules score a change low, medium or high and list the files to read.
2. **Contributor parity.** A pull request gets the same signals as an agent's change, in CI.
3. **Verifier.** A crawl of the sample app from its sitemap, with an evidence report.
4. **Risk-routed review.** A draft that lets the risk level pick how deep `/attack` goes.

Risk is a reading signal, not a gate. It never fails a build. The gates are still `./mvnw verify` and the hooks.

## Kit and project

The kit works in any repo with bash 3.2 or newer, git and standard tools. The project part is Beansprout's own settings.

| Piece | Kit (reusable) | Project (Beansprout) |
| --- | --- | --- |
| Risk analyzer | `bin/risk`, `test/run`, `test/risk_test.sh` | `risk.txt` |
| Contributor parity | the steps in `.github/workflows/factory.yml`, `test/workflow_test.sh` | `ruleset-main.json` (the `verify` check name) |
| Verifier | the crawl idea: sitemap, then every internal link | `SitemapCrawl`, `CrawlReport` and `SitemapCrawlTests` in `beansprout-sample`, the steps in `verify.yml` |
| Risk-routed review | the low, medium and high panels | `drafts/attack-risk-routed.md` |

A new project copies `bin/` and `test/`, writes its own `risk.txt`, and adds the workflow steps.
The kit tests build their own fixture repos. One of them uses a Gradle config to show nothing is Maven only.

## Risk analyzer

```bash
.shipit/factory/bin/risk                 # this branch against origin/main
.shipit/factory/bin/risk HEAD~3          # the last three commits
.shipit/factory/bin/risk --worktree HEAD # uncommitted and untracked work
.shipit/factory/bin/risk --level main    # just the level: low, medium or high
.shipit/factory/test/run                 # the tests for all of it
```

It reads `git diff base...HEAD` and prints Markdown: the overall level, then each reason with its files.
It exits 0 at every level. It exits 2 for a bad config and 3 for a git error.

`risk.txt` has three kinds of lines:

```text
high   **/*Properties.java   Public beansprout.* property names     # a path rule: level, glob, reason
check  high  changed-test    An existing test was changed or deleted # a computed check
set    size-limit  800                                              # a setting
```

- Each file goes to the first rule at the highest level that matches it. A file no rule matches gets `default-level`.
- The highest level found is the overall level.
- Globs: `**/` is any number of folders, `**` is anything, `*` is anything but a slash.
- The computed checks are `multi-module`, `changed-test`, `new-dependency`, `mixed-commit` and `size`.
  A check only runs when the config names it. The settings they use are at the bottom of `risk.txt`.

The rules come from AGENTS.md, "Judgment: Dan reads these", plus the release, the build and the gate files.
Two rules used to live only in the hooks: no changed existing tests, and a spec and its code in separate
commits. They are now `changed-test` and `mixed-commit`, so a contributor PR gets them too.
In CI they are signals, not blocks, because a real fix may need to change a test.

## Contributor parity

`.github/workflows/factory.yml` runs on every pull request with `contents: read`. It runs the risk tests,
then scores the PR against its base and writes the result to the job summary. It has no
`pull_request_target`, no comments, no labels and no secrets.

The risk script and rules come from the base branch, not the PR. So a PR cannot lower its own score
by editing `risk.txt`. The first PR that adds the factory has no base copy, so it runs its own.

`verify.yml` still runs `./mvnw -B verify`. It now also puts the crawl summary in the job summary and
uploads `beansprout-sample/target/crawl/` as the `crawl-evidence` artifact.

**The ruleset is off until Dan runs it.** `ruleset-main.json` makes `verify` a required check on `main`.
Repository admins can bypass it, so Dan can still push to `main`. To apply it:

```bash
gh api --method POST repos/danvega/beansprout/rulesets --input .shipit/factory/ruleset-main.json
```

Then check it under Settings, Rules, Rulesets. `integration_id` 15368 is GitHub Actions, so only a check
from Actions counts. `actor_id` 5 is the admin role.

## Verifier

```bash
./mvnw -q -pl beansprout-sample -am test -Dtest=SitemapCrawlTests -Dsurefire.failIfNoSpecifiedTests=false
```

`SitemapCrawlTests` starts the sample on a random port and reads `/blog/sitemap.xml`. Every URL in it must
answer 200. Every `href` and `src` on those pages that points into the app must answer 2xx. Links to other
hosts are counted, not fetched. It runs in every `./mvnw verify`.

The evidence goes to `beansprout-sample/target/crawl/`:

- `crawl-report.md`: every page, every internal target, the failures, and the gaps a static export would hit.
- `crawl-summary.md`: the short table for the CI job summary.

A second test proves the crawl can fail. It points the app at `src/test/resources/test-posts-broken/`,
a post with a dead link and a missing picture, and checks the crawl reports both.
To see the main test go red on purpose, add `-Dcrawl.posts=classpath:test-posts-broken/` to the command above.

The report lists what is linked from the pages but missing from `Pages.urls()`: stylesheets from jars, the
feed, the app's own pictures, post bundle pictures, the sitemap and the 404 page. A static export
(danvega/beansprout#1) would have to write those too. When export lands, the crawl gets one more assertion:
each exported file exists and matches the live response. Export needs Dan's spec first, so it is not built.

## Risk-routed review

`drafts/attack-risk-routed.md` has the exact new text for `.claude/skills/attack/SKILL.md`. It is a draft
because `.claude/` is protected. Low runs one Haiku `rules` attacker and no judge. Medium is today's
panel. High adds a second `inputs` attacker on Sonnet. The judge is always Sonnet or stronger.
The draft ends with an eval idea for checking which model finds what.

## Off until Dan says yes

- PR comments and labels. The workflow writes only to the job summary.
- The ruleset. The file is here. Nobody has run it.
- Routines and cloud triggers for ticket triage.
- Auto-fix for review comments, and auto-merge.
- The decision-model layer (Spring AI `DecisionModel`, with these path rules as its baseline).
- The `/attack` change. It is a draft.

## Notes

- CI needs `fetch-depth: 0`. A shallow clone has no merge base, and `risk` exits 3.
- The commit hook's mixed-commit check matches `^[^/]+/src/main/`, so it misses theme modules under `themes/`.
  Commit `e7bee3c` changed `specs/pages.md` and moved theme `src/main` files together. `risk` flags it.
  The hook is protected, so this is left for Dan.

## Cloud routines (item 6, drafts)

`routines/` holds the prompts and the cloud setup for work that runs off Dan's laptop: a weekday
`digest` and an issue `triage` that write nothing to GitHub, then `spec-draft` and `build` later.
None of them is turned on. `routines/README.md` has the rules, the order and what Dan sets up by hand.
