# Spec: feeds

Goal 3 in PRODUCT.md: feeds and search engines are covered.

## Why

Readers follow blogs in feed readers, and search engines find pages through sitemaps. Without
both, a blog is hard to follow and hard to find.

## Requirements

| ID | Requirement |
| --- | --- |
| FEEDS-1 | The system shall serve an Atom feed of the 20 newest published posts at `{base}/feed.xml`, as `application/atom+xml`. |
| FEEDS-2 | Each feed entry shall carry the post's title, its absolute address as link and id, its date, its summary and its body as HTML. |
| FEEDS-3 | The system shall build absolute addresses from `beansprout.site-url`, or from the request's scheme, host and port when it is not set. |
| FEEDS-4 | The system shall serve a sitemap at `{base}/sitemap.xml` that lists the index pages, every published post, every tag page and the archive. |
| FEEDS-5 | The system shall give each post in the sitemap a `lastmod` of its date. |
| FEEDS-6 | If a post is a draft or dated in the future, then the system shall leave it out of the feed and the sitemap. |
| FEEDS-7 | The system shall XML-escape every value in the feed and the sitemap. |
| FEEDS-8 | Every page shall link to the feed with `<link rel="alternate" type="application/atom+xml">`. |

## Out of scope

- RSS 2.0. Every current feed reader reads Atom.
- Merging into a sitemap the app already has. The app can list `{base}/sitemap.xml` in its sitemap index.
- Feeds per tag.
