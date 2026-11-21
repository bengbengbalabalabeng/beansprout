# Spec: other-frontmatter

Part of 0.3.1, "bring your blog with you". Read the frontmatter that Jekyll and Hugo posts already
use, so a folder of old posts mostly just works.

## Why

A Jekyll or Hugo post moved into Beansprout today loses its description, picture and edit date,
and a Jekyll post with its date only in the file name is skipped.

## Requirements

### Other names for keys Beansprout has

| ID | Requirement |
| --- | --- |
| FM-1 | When a post has `description` and no `summary`, the system shall use `description` as its summary. |
| FM-2 | When a post has both `summary` and `description`, the system shall use `summary`. |
| FM-3 | When a post's `image` is a single value and it has no `picture`, the system shall use `image` as its picture. |
| FM-4 | If a post's `image` has indented lines under it, then the system shall ignore `image` and keep the post. |
| FM-5 | When a post has both `picture` and `image`, the system shall use `picture`. |
| FM-6 | When a post has `lastmod` and no `updated`, the system shall use `lastmod` as its modified date. |
| FM-7 | If `lastmod` is not a date the system can read, then the system shall skip the post and log a warning that names it. |
| FM-8 | When a post has `canonical_url` and no `canonical`, the system shall use `canonical_url` as its canonical address. |

### Jekyll file names

A Jekyll post is named like `2026-03-14-hello.md`. The same rule applies to a bundle folder named
like `2026-03-14-hello/`.

| ID | Requirement |
| --- | --- |
| FM-9 | When a post has no `date` and its file or folder name starts with a date and a dash, the system shall use that date as its date. |
| FM-10 | When a post has no `slug` and its file or folder name starts with a date and a dash, the system shall leave the date out of its slug. |
| FM-11 | If a post has no `date` and its name starts with a day that does not exist, such as `2026-02-30-`, then the system shall skip the post and log a warning that names it. |

### Hugo's TOML frontmatter

A Hugo post can open with a `+++` line instead of `---`. Only the simple shapes the YAML reader
takes are read.

| ID | Requirement |
| --- | --- |
| FM-12 | When a file starts with a `+++` line, the system shall read the lines up to the next `+++` line as its frontmatter. |
| FM-13 | The system shall read a TOML line `key = "text"` or `key = 'text'` as that key's value. |
| FM-14 | The system shall read a TOML date or date and time, such as `date = 2026-03-14T09:30:00Z`, as that key's value. |
| FM-15 | The system shall read `true` and `false` as the values of `draft` and `unlisted`. |
| FM-16 | The system shall read a TOML array of strings, such as `tags = ["java", "spring"]`, as a list. |
| FM-17 | The system shall ignore every line from a table header, such as `[params]`, to the end of the frontmatter. |
| FM-18 | If a TOML post has no `title`, or no date the system can find, then the system shall skip it and log a warning that names it. |
| FM-19 | The system shall ignore a TOML line whose key it does not read, whatever its value, such as `weight = 10` or `toc = true`. |
| FM-20 | If a TOML line is not `key = value`, a comment or a table header, then the system shall skip the post and log a warning that names the line. |

### A tested migration

The sample's tests read both folders, so the migration guide's claims are checked.

| ID | Requirement |
| --- | --- |
| FM-21 | When the posts are a folder of Jekyll-shaped posts, with dated names, `description`, `image` and `redirect_from`, the system shall publish every one of them. |
| FM-22 | When the posts are a folder of Hugo-shaped posts, with TOML frontmatter, `lastmod` and `aliases`, the system shall publish every one of them. |

## A URL change for current users

FM-10 changes an address. A post saved today as `posts/2026-03-14-hello.md` lives at
`/blog/2026-03-14-hello`. After this spec it lives at `/blog/hello`. `specs/aliases.md` (ALIAS-12)
answers the old address with a 301, so links and search rankings carry over. A post with its own
`slug:` line keeps its address.

## How it fits the reader

- `Frontmatter.KNOWN_KEYS` gains `description`, `image`, `lastmod`, `canonical_url`, `aliases`,
  `redirect_from`, `author`, `canonical` and `unlisted`. The last five belong to other specs.
- Today an indented line under a known key is an error. FM-4 keeps nested `image:` blocks (as
  Jekyll themes write them) from turning into errors once `image` is known.
- TOML is read by a second small reader beside the YAML one, into the same key and value shapes.
  No new dependency.

## Out of scope

- Reading Hugo's nested tables, inline tables, multi-line strings or numbers. None of them hold
  a key Beansprout reads. A top-level key with one of these values is ignored (FM-19).
- JSON frontmatter (Hugo's `{` form).
- Jekyll's `categories`, `permalink` and `layout`, and Hugo's `url`, `weight` and `series`.
  They stay unknown and ignored.
- Jekyll's space-separated `tags: java spring`. Beansprout reads that as one tag, since a tag
  like `spring boot` has a space in it. The migration guide shows the list forms.
- Converting the files. Beansprout reads them as they are.
