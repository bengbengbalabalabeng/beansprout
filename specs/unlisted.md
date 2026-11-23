# Spec: unlisted

From the second tier of `reports/Blog platform features for Beansprout.md`, and part of 0.3.1,
"keep readers around". Docusaurus has the same flag.

## Why

Some posts are for a link you share, such as a talk's notes or a page for one client, not for the
blog's front page. Today a post is either a draft, which nobody can read, or listed everywhere.

## Requirements

```markdown
---
title: Notes for the Porto keynote
date: 2026-11-10
unlisted: true
---
```

| ID | Requirement |
| --- | --- |
| UNLISTED-1 | When a post has `unlisted: true`, the system shall serve its page at its address with 200. |
| UNLISTED-2 | The system shall leave unlisted posts out of the index and its numbered pages. |
| UNLISTED-3 | The system shall leave unlisted posts out of tag pages. |
| UNLISTED-4 | The system shall leave unlisted posts out of the archive. |
| UNLISTED-5 | The system shall leave unlisted posts out of the feed. |
| UNLISTED-6 | The system shall leave unlisted posts out of tag feeds. |
| UNLISTED-7 | The system shall leave unlisted posts out of the sitemap. |
| UNLISTED-8 | The system shall leave unlisted posts out of related posts. |
| UNLISTED-9 | The system shall leave unlisted posts out of other posts' newer and older links. |
| UNLISTED-10 | The system shall give an unlisted post's own page no newer or older links. |
| UNLISTED-11 | The system shall leave unlisted posts out of tag counts, so a tag only unlisted posts use has no page. |
| UNLISTED-12 | The system shall leave unlisted posts out of the MCP `list_posts` and `find_posts_by_tag` tools. |
| UNLISTED-13 | When an agent asks the MCP `read_post` tool for an unlisted post by its slug, the system shall return it. |
| UNLISTED-14 | The system shall give an unlisted post's page a robots value of `noindex`. |
| UNLISTED-15 | When a page has a robots value, every theme shall render it as `<meta name="robots" content="...">`. |
| UNLISTED-16 | When the static export runs, the system shall write every live unlisted post. |
| UNLISTED-17 | If `unlisted` is not `true` or `false`, then the system shall skip the post. |
| UNLISTED-18 | While an unlisted post is a draft or dated in the future, the system shall not serve it. |
| UNLISTED-19 | If `unlisted` is not `true` or `false`, then the system shall log a warning that names the post. |

## Notes

- `noindex` is a `<meta>` tag, not an `X-Robots-Tag` header, so it works in the static export too.
  A static host sends no headers Beansprout chooses.
- UNLISTED-16 needs export code. The export writes the pages in the sitemap (EXPORT-16), and an
  unlisted post is not in it. A link from another post is not enough: the crawl leaves out any
  page that is not in the sitemap, with a warning.
- An unlisted post keeps its aliases (ALIAS-1). Its address is the point of it.

## The view contract

`Meta` gains `robots`: `noindex` on an unlisted post's page, empty everywhere else. UNLISTED-15
touches the layout of all eight themes: Calling Card, Dateline, Linocut, Longhand, Marginalia, Onionskin, Plainsong
and Small Machines.

## Out of scope

- A password or any other lock. Anyone with the address can read the post.
- `noindex` on other pages.
- An unlisted list for the writer. `grep unlisted posts/` does it.
