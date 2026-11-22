# Spec: aliases

Rank 4 in `reports/Blog platform features for Beansprout.md`, and part of 0.3.1, "bring your blog
with you".

## Why

A blog moved from Jekyll, Hugo or WordPress has old addresses that other sites link to. Without a
real redirect, those links answer 404 and the posts lose their search ranking.

## Requirements

An alias is an old path, such as `/2019/03/14/hello.html`, written in a post's frontmatter. It can
be anywhere on the site, not only under the blog.

```markdown
---
title: Hello
date: 2019-03-14
aliases: [/2019/03/14/hello.html, /posts/hello/]
---
```

| ID | Requirement |
| --- | --- |
| ALIAS-1 | When a post has `aliases`, the system shall answer a GET for each alias with a 301 to the post's address. |
| ALIAS-2 | When a post has `redirect_from`, the system shall treat each of its paths as an alias. |
| ALIAS-3 | The system shall read `aliases` and `redirect_from` as a `[a, b]` list, a `- item` list or a single value. |
| ALIAS-4 | The system shall match an alias with or without a trailing slash. |
| ALIAS-5 | The system shall answer an alias under the app's context path, like every other address. |
| ALIAS-6 | If an alias does not start with `/`, then the system shall ignore it and log a warning that names the post. |
| ALIAS-7 | If an alias is a page the blog serves, such as a live post's address, then the system shall ignore it and log a warning that names both. |
| ALIAS-8 | If two posts give the same alias, then the system shall redirect it to the post whose file comes first by path and log a warning that names both. |
| ALIAS-9 | While a post is a draft or dated in the future, the system shall not answer its aliases. |
| ALIAS-10 | When the app maps an alias's path itself, the system shall let the app's mapping answer. |
| ALIAS-11 | When the app has a static file at an alias's path, the system shall let the static file answer. |
| ALIAS-12 | When a post's slug left a date out of its name (FM-10), the system shall treat `{base}/` plus the full name as an alias. |
| ALIAS-13 | When the static export runs, the system shall write a page at each live alias that sends the reader to the post with a meta refresh and a canonical link. |
| ALIAS-14 | The system shall write an alias's page at the file its path names: `/a/b.html` to `a/b.html`, `/a/b` to `a/b.html` and `/a/b/` to `a/b/index.html`. |
| ALIAS-15 | The system shall leave aliases out of the sitemap. |

## How it is served

- The same way as robots.txt (`BeansproutRobotsAutoConfiguration`): a handler mapping ordered
  after the app's controllers and before its static files. It waits for Spring MVC's request
  mappings, or the DispatcherServlet stops adding its defaults and every page is lost.
- ALIAS-10 and ALIAS-11 come from that order. The static check is the robots.txt one, per path.
- The redirect keeps no query string. A 301, not a 308: every alias is a GET from an old link.
- `Posts` already settles duplicate slugs by path (POSTS-6). ALIAS-8 uses the same rule.

## Out of scope

- Relative aliases, as Hugo allows (`aliases: [old-name]`, resolved against the post's folder).
  ALIAS-6 warns about them, and the migration guide shows the absolute form.
- Aliases with `?` or `#` in them.
- Redirects for tag pages, the feed or pages that are not posts.
- A redirect file for a static host, such as Netlify's `_redirects`. ALIAS-13 works on every host.
