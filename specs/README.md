# Specs

Every feature starts here, before any code. One file per feature, such as `specs/posts.md`.
Dan approves the spec, then it is committed on its own. The code comes in a later commit.

The build reads these files. `DefinitionOfDoneTests` fails until every requirement has a test
that names its ID in `@DisplayName`, and fails if a test names an ID that no spec defines.

## Format

```markdown
# Spec: <feature>

## Why

One or two sentences. What is missing today.

## Requirements

| ID | Requirement |
| --- | --- |
| POSTS-1 | When a Markdown file has `title` and `date` frontmatter, the system shall publish it as a post. |
| POSTS-2 | If a post has `draft: true`, then the system shall not list it or serve it. |

## Out of scope

- What this feature does not do, so nobody builds it by accident.
```

## Rules for requirements

- An ID is a short prefix in capitals, a dash and a number: `POSTS-1`. One prefix per spec.
- Never reuse or renumber an ID. Retire it by deleting the row.
- Write each one in EARS form, so it can be tested:
  - "The system shall ..." for something always true.
  - "When ..., the system shall ..." for a response to an event.
  - "If ..., then the system shall ..." for an unwanted case.
- One behavior per row. If a row needs "and", split it.
