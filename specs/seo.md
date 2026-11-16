# Spec: seo

The top three items from `reports/Blog platform features for Beansprout.md`: the tags search engines
and social sites read, an honest updated date, and structured data.

## Why

Search engines and social sites read a page's `<head>`, not its body. Today every Beansprout page
has a title and nothing else there. A shared link shows no picture or summary, Google has no
canonical address or structured data, and a post edited later still says it was last changed on
the day it went out.

## Requirements

| ID | Requirement |
| --- | --- |
| SEO-1 | The system shall give every page it serves with status 200 a canonical link to its own absolute address. |
| SEO-2 | The system shall give every page a meta description: the summary on a post, `beansprout.description` on the index when it is set, and a sentence naming the blog on every other page. |
| SEO-3 | The system shall give every page it serves with status 200 the Open Graph tags `og:title`, `og:description`, `og:url`, `og:site_name` and `og:type`. |
| SEO-4 | The system shall set `og:type` to `article` on post pages and `website` on every other page. |
| SEO-5 | When a post has a picture, the system shall give its page `og:image` as an absolute address and `twitter:card` as `summary_large_image`. |
| SEO-6 | When a page has no post picture and `beansprout.photo` is set, the system shall use the photo as `og:image` with `twitter:card` as `summary`. |
| SEO-7 | The system shall give each post page `article:published_time`, `article:modified_time` and one `article:tag` per tag. |
| SEO-8 | When a post has `updated` frontmatter, the system shall use it as the post's modified date in its page, its structured data, the feed and the sitemap. |
| SEO-9 | When a post has no `updated` frontmatter, the system shall use the post's date as its modified date. |
| SEO-10 | If `updated` is not a date the system can read, then the system shall skip the post and log a warning that names it. |
| SEO-11 | When a post's `updated` date is a later day than its date, the system shall show "Updated" and that day on the post page. |
| SEO-12 | The system shall give each post page `BlogPosting` JSON-LD with its headline, description, URL, publish date and modified date. |
| SEO-13 | When a post has a picture, the system shall add it to the JSON-LD as `image`. |
| SEO-14 | When `beansprout.author` is set, the system shall add the author to the JSON-LD as a `Person`. |
| SEO-15 | The system shall escape the JSON-LD so no value can end the `<script>` element. |
| SEO-16 | The system shall build absolute addresses from `beansprout.site-url`, or from the request's scheme, host and port when it is not set. |

## Frontmatter

```markdown
---
title: Testcontainers without the waiting
date: 2026-03-14
updated: 2026-04-02          # optional, same shapes as date
---
```

A blank `updated:` line means the post was not updated.

## Out of scope

- Generated social images. A post without a picture uses the writer's photo, or none.
- `fediverse:creator`, `rel=me` and other identity tags.
- Structured data on pages other than posts, such as `Blog` or `BreadcrumbList`.
- `noindex` on the not-found page. Its 404 status already keeps it out of search. For the same
  reason it has no canonical link and no `og:url`.
