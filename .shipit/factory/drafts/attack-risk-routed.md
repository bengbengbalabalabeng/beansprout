# Draft: route `/attack` by risk

Status: a proposal for Dan. Nothing here is applied. `.claude/` is protected, so Dan applies it by hand.

## What changes

`/attack` runs `.shipit/factory/bin/risk` on its range first. The level picks the panel:

| Risk | Attackers | Judge |
| --- | --- | --- |
| low | one `rules` attacker on Haiku | none. Any finding reruns the review at medium. |
| medium | `rules`, `inputs` and `tests` on Haiku (today's panel) | Sonnet |
| high | the medium panel, plus a second `inputs` attacker on Sonnet | Sonnet |

The judge always runs on Sonnet or stronger. Low skips the judge only while the attacker finds nothing.
A finding at low is not graded by the author. It sends the change to the medium panel, so the rule
"the session that wrote a change does not get to grade it" still holds.

Dan can force a depth: `/attack --depth high HEAD~3`. Use it when the risk rules miss something.

## Files to change

Only one file changes: `.claude/skills/attack/SKILL.md`. The agent files stay as they are.
`attacker.md` keeps `model: haiku`, and the high panel passes `model: sonnet` on the one extra
Agent call, which overrides the frontmatter for that call. `judge.md` keeps `model: sonnet`.

If per-call model overrides ever go away, add the fallback agent below instead and launch it by name.

### New text for `.claude/skills/attack/SKILL.md`

Replace the whole file with this:

````markdown
---
name: attack
description: Adversarial review of a change. Attackers try to prove it wrong, a judge filters their findings, and the valid ones come back to the author. The risk of the change picks how many attackers run.
argument-hint: [--depth low|medium|high] [base ref, default HEAD]
---
Adversarial review. The session that wrote a change does not get to grade it, so other sessions do.

Arguments: `$ARGUMENTS`. An optional `--depth low|medium|high` comes first, then an optional base ref.
With no base ref, review the uncommitted work against `HEAD`. If that diff is empty, review the last commit: `HEAD~1`.

1. Compute the range once and show `git diff --stat <range>`. Pass the same range to every agent, unchanged.
2. Run `.shipit/factory/bin/risk --worktree <range>` and show its output. Its first line is `## Risk: low`,
   `## Risk: medium` or `## Risk: high`. That is the depth, unless `--depth` was given. If the script exits
   non-zero, say so, show its error, and use medium.
3. Launch the attackers for that depth in one message, in parallel. Each gets the range, its angle, the risk
   output from step 2, and this line: "Report findings only. Do not fix anything."
   - low: one `attacker` with the angle `rules`.
   - medium: three `attacker` subagents with the angles `rules`, `inputs` and `tests`.
   - high: the three medium attackers, plus a fourth `attacker` with the angle `inputs` and `model: sonnet`.
     Tell the fourth to start with the files the risk output lists under "High".
4. At low: if the attacker reports "No findings.", the verdict is PASS. Print its report under the heading
   "Attack verdict (low risk, no judge)" and stop. If it reports any finding, do not judge it yourself.
   Say "Low-risk attacker found something. Rerunning at medium." and go back to step 3 at medium.
5. At medium and high: when all attackers have returned, launch one `judge` subagent with every report pasted
   verbatim and the range. The judge runs on Sonnet. Never pass it a weaker model.
6. Print the judge's output unchanged, under the heading "Attack verdict (<depth> risk)". Do not soften it,
   re-grade it, or summarize it away.
7. For each VALID finding: fix it, or push back with evidence (a test you ran, a Rules line, a command and its output).
   Then run `./mvnw -q verify`, then run `/attack` again with the same arguments. Stop when the verdict is PASS.
   Suggestions are not required work. List them for the user and move on.
````

### Fallback only: `.claude/agents/attacker-sonnet.md`

Not needed while the Agent tool takes a `model` override. If it stops taking one, add this file and launch
`attacker-sonnet` in step 3 for the fourth attacker instead of passing `model: sonnet`.

````markdown
---
name: attacker-sonnet
description: The attacker on a stronger model, for high-risk changes. Same job and rules as attacker. Launched by /attack.
model: sonnet
tools: Read, Grep, Glob, Bash
maxTurns: 30
---
Follow `.claude/agents/attacker.md` exactly. Read it first. It is your whole brief: the checklist, the angles,
the commands you may run and the report format. Never modify a file.
````

## How to apply

```bash
# 1. Read this draft. 2. Copy the SKILL.md block above over .claude/skills/attack/SKILL.md.
# 3. Try it on a low change and a high one:
.shipit/factory/bin/risk --worktree HEAD~1
```

Then run `/attack HEAD~1` on a docs-only commit (expect one attacker, no judge) and on a commit that
touches `BeansproutProperties.java` (expect four attackers and the judge).

## Eval idea: which model finds what

PRODUCT.md says the first `/attack` took five rounds and found 13 valid issues, each fixed with a failing test
first. Those fixes all landed in one commit, `4c31aad`, so there is no commit before each fix to check out.
The eval rebuilds them instead:

1. **Seed the bugs.** For each of the 13 findings, make a branch from `4c31aad` that undoes that one fix in
   `src/main` and keeps its test out of the tree. The fix tests are mostly in `FrontmatterEdgeTests`,
   `PostFileEdgeTests`, `UnknownKeyTests`, `RoundFourTests`, `FeedCharacterTests` and `SitemapEscapingTests`.
   A branch is valid when its fix test fails on it. That test is the ground truth.
2. **Run each panel.** On every branch, run the attacker angles on Haiku and on Sonnet, and the full low,
   medium and high panels. Record each finding.
3. **Score it.** A hit is a VALID finding that names the seeded bug. Count hits, misses and invalid findings
   per model and angle. Note what `risk` scored each branch, since a low score that hides a real bug is the
   costly miss.
4. **Decide with numbers.** If Haiku `inputs` misses most seeded frontmatter bugs and Sonnet finds them, the
   high panel's extra attacker earns its cost. If low-risk branches never hid a bug, the single attacker is
   enough. Rerun the eval whenever the attacker prompt or the risk rules change, as a dry run before the change
   lands.

This needs a few dollars of model calls and a script to drive the panels headless. It is not built.
