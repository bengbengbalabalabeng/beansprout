# Spec: canonical

Part of 0.3.1, "bring your blog with you".

## Why

A post that first appeared on dev.to, Medium or another blog competes with that copy in search.
Its page should point search engines at the first copy.

## Requirements

```markdown
---
title: Testcontainers without the waiting
date: 2026-03-14
canonical: https://dev.to/jane/testcontainers-without-the-waiting
---
```

| ID | Requirement |
| --- | --- |
| CANON-1 | When a post has `canonical`, the system shall use it as the post page's canonical link. |
| CANON-2 | When a post has `canonical`, the system shall use it as the post page's `og:url`. |
| CANON-3 | When a post has `canonical`, the system shall use it as the `url` and `mainEntityOfPage` of the post's JSON-LD. |
| CANON-4 | When a post has `canonical`, the system shall leave the post out of the sitemap. |
| CANON-5 | When a post has `canonical`, the system shall keep it in the feed. |
| CANON-6 | If `canonical` is not an absolute `http` or `https` address, then the system shall skip the post. |
| CANON-7 | When a post has no `canonical`, the system shall use the post's own address, as it does today. |
| CANON-8 | If the system skips a post for its `canonical`, then the system shall log a warning that names the post. |
| CANON-9 | When a post has `canonical`, Small Machines shall label the post with its own slug, not the canonical's. |
| CANON-10 | When a post has `canonical`, the system shall keep it in the index. |
| CANON-11 | When a post has `canonical`, the system shall keep it on its tag pages. |
| CANON-12 | When a post has `canonical`, the system shall keep it in the archive. |

## Notes

- Ghost leaves cross-posted posts out of its sitemap too. A sitemap that lists a page whose
  canonical link points elsewhere sends Google two signals that disagree.
- The themes render `page.meta().canonical()` for both the canonical link and `og:url`, so
  CANON-1 and CANON-2 need no theme change.
- Small Machines built its window label, like `moved.post`, from `meta.canonical`. `view.Post`
  gains `url`, the post's own address on the blog. The label keeps the post's own slug (CANON-9).
- `canonical_url`, the jekyll-seo-tag name, comes with `specs/other-frontmatter.md`.

## Out of scope

- A canonical override on tag pages, the index or the archive.
- Checking that the other address answers. Beansprout never fetches it.
