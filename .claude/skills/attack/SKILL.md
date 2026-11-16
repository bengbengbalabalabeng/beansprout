---
name: attack
description: Adversarial review of a change. Three attackers try to prove it wrong, a judge filters their findings, and the valid ones come back to the author.
argument-hint: [base ref, default HEAD]
---
Adversarial review. The session that wrote a change does not get to grade it, so other sessions do.

Diff range: `$ARGUMENTS`. If empty, review the uncommitted work against `HEAD`. If that diff is empty, review the
last commit: `HEAD~1`.

1. Compute the range once and show `git diff --stat <range>`. Pass the same range to every agent, unchanged.
2. In one message, launch three `attacker` subagents in parallel with the angles `rules`, `inputs`, and `tests`.
   Each gets the range, its angle, and this line: "Report findings only. Do not fix anything."
3. When all three have returned, launch one `judge` subagent with the three reports pasted verbatim and the range.
4. Print the judge's output unchanged, under the heading "Attack verdict". Do not soften it, re-grade it, or summarize it away.
5. For each VALID finding: fix it, or push back with evidence (a test you ran, a Rules line, a command and its output).
   Then run `./mvnw -q verify`, then run `/attack` again. Stop when the verdict is PASS.
   Suggestions are not required work. List them for the user and move on.
