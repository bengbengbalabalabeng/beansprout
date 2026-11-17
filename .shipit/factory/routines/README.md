# Cloud routines

Status: drafts. Nothing here is turned on. Dan creates each routine after he reads its prompt.

The factory's checks run in GitHub Actions. The work that needs a model runs as Claude Code routines
in the cloud, so it doesn't need Dan's laptop. A routine runs on Dan's Claude subscription. The repo
holds no API key.

## The rule

Beansprout is public. Anyone can open an issue or a pull request.

- **Text from strangers can only start reading and drafting.** A routine that a stranger can fire
  writes nothing to GitHub. It leaves its draft in the routine's session for Dan.
- **Writing needs a trigger only Dan can pull.** His merge, his "Run now", or a PR filter on his login.
- **No connectors.** A routine that reads public text gets no MCP connectors. Gmail and the others stay
  in Dan's personal routines. Remove them in the routine's Connectors tab.
- **Never** push to `main`, merge, tag or release. Pushes go to `claude/` branches only, which is how
  routines work anyway. A `v*` tag fires `release.yml`, so tagging is off limits.
- Beansprout's `.claude/settings.json` hooks run in a single-repo cloud session. So the Stop gate, the
  commit guard and the deny list apply in the cloud too. `git push` is an `ask` rule there, so an
  unattended run stops before it pushes. That's the wanted default for every routine below except build.

## The routines

| Routine | Fired by | Who can fire it | Does | Writes | Model |
| --- | --- | --- | --- | --- | --- |
| `digest` | Schedule, weekdays 7:30 | The clock | Lists new issues, comments and PRs since the last run, with each open PR's risk level | Nothing | Haiku |
| `triage` | GitHub: issue opened | Anyone | Sorts the issue, finds its PRODUCT.md line, checks for duplicates, drafts a reply | Nothing | Sonnet |
| `spec-draft` | "Run now" with the issue number as text | Dan | Drafts `specs/<feature>.md` on a `claude/spec-` branch, as a draft PR | A branch, a draft PR | Opus |
| `build` | GitHub: PR merged, head branch starts with `claude/spec-`, author `danvega` | Dan's merge | Builds the approved spec behind the gates, opens a draft PR | A branch, a draft PR | Opus |

Prompts: `digest.md` and `triage.md` are written. `spec-draft` and `build` come after the first two have
run for a while and Dan trusts what they hand him.

Order to turn them on: `digest`, then `triage`, then the two that write.

## Kit and project

| Part | Kit (any project) | Project (Beansprout) |
| --- | --- | --- |
| The rule above | yes | |
| Prompt shape: read, decide, draft, never post | `digest.md`, `triage.md` | the repo name, the PRODUCT.md sections, the label list |
| Cloud setup | `cloud-setup.sh` (installs a JDK by version) | the version, 25 |
| Trigger and filter choices | the table above | the login `danvega`, the `claude/spec-` prefix |

## What Dan does by hand

1. Install the Claude GitHub App on `danvega/beansprout`. It asks for Contents, Issues and Pull requests
   with write access. This is Dan's call. Without it there are no GitHub triggers.
2. Create a cloud environment named `beansprout`. Paste `cloud-setup.sh` as its setup script. Set network
   access to Limited and add `api.adoptium.net`, `github.com` and `release-assets.githubusercontent.com`
   to the allowed domains, so the JDK download works. (Checked 2026-11-17: the Adoptium link redirects
   to that last host.)
3. Read each prompt. Then Claude creates the routine, and Dan attaches the GitHub trigger in the web UI.
   The trigger API isn't documented, so the UI is the safe path.

## Created (2026-11-17)

| Routine | Id | Schedule | Connectors | Tools |
| --- | --- | --- | --- | --- |
| Beansprout digest | `trig_01DmN6pQVpAZtLFMVMjxtAPv` | Weekdays 7:30 New York | none | Bash, Read, Glob, Grep |
| Beansprout triage | `trig_01LuDLbT85Hrb2swpJ65Sb1Z` | A placeholder one-time run on 2027-06-01. Dan attaches the "issue opened" trigger. | none | Bash, Read, Glob, Grep |

Both use the default cloud environment and clone `main`. Neither has Write or Edit tools.
The prompts in the routines are copies of `digest.md` and `triage.md`. Change both together.

Learned from the first test run:
- Text sent with "Run now" or the fire API reaches the session wrapped in `<routine-fire-payload>`, marked as
  data. So "Triage issue #1" works as a manual trigger.
- The public repo cloned without the Claude GitHub App. The App is still needed for GitHub triggers.
- Triage on issue 1 took 15 seconds and 4 turns. It matched **Static site export** under Non-goals, found
  no duplicates, and flagged that Dan's comment ("one of the first things") disagrees with PRODUCT.md
  ("second release"). Two prompt fixes came from it: GitHub search needs `is:issue`, and the reply must
  speak as "I", not "Dan".
- The digest has not had a test run yet.

## Not known yet

Each of these gets checked on the first "Run now", before the trigger is attached.

- **How the event reaches the prompt.** The docs don't say whether a GitHub trigger passes the issue
  number. If it doesn't, `triage` lists issues opened in the last hour and handles any it hasn't seen.
- **Whether `JAVA_HOME` from the setup script reaches the session.** `digest` and `triage` don't build,
  so they don't care. `build` does.
- **Issue triggers have no filters.** Only PR triggers have them. So `triage` fires for every new issue,
  which is fine because it writes nothing.
- **No comment triggers.** So auto-fix for review comments can't be a GitHub trigger yet. For Dan's own
  agent PRs the Claude Code app's Auto-fix does it. A schedule could sweep review comments later.
- **Run caps.** 30 fires an hour per routine. No documented cap on turns or run length.
