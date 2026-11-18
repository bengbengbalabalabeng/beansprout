# Spec: caching

Rank 7 in `reports/Blog platform features for Beansprout.md`: ETags and 304s on pages, the feed
and the sitemap.

## Why

Every request for a Beansprout page gets the whole page, even when nothing changed. Google says it
"strongly recommends" ETags and accepts a 304 with no body, which saves crawl budget and server
time. Posts are already in memory, so the system knows what a page holds before it renders it.

## Requirements

A blog response is an index page, the archive, a tag page, a post page, the feed, the sitemap or
robots.txt.

| ID | Requirement |
| --- | --- |
| CACHE-1 | The system shall give every blog response with status 200 a weak ETag. |
| CACHE-2 | When a request's `If-None-Match` holds the current ETag, the system shall answer 304 with no body. |
| CACHE-3 | The system shall give a 304 the same ETag the 200 would have. |
| CACHE-12 | The system shall give a 304 the same `Cache-Control` the 200 would have. |
| CACHE-4 | When the system answers a theme page with 304, it shall not render the theme. |
| CACHE-5 | When nothing about a page has changed, the system shall give it the same ETag on every request. |
| CACHE-6 | When a post's file changes and `beansprout.posts.reload` is on, the system shall give its post page a new ETag. |
| CACHE-13 | When a post's file changes and `beansprout.posts.reload` is on, the system shall give the feed a new ETag. |
| CACHE-7 | When a scheduled post's time passes, the system shall give a new ETag to the index, the archive, its tag pages, the feed and the sitemap, with no restart. |
| CACHE-8 | When the app restarts, the system shall give every blog response a new ETag. |
| CACHE-9 | When `beansprout.site-url` is not set, the system shall give the same path a different ETag for each host it is asked for. |
| CACHE-10 | The system shall set `Cache-Control: no-cache` on every blog response. |
| CACHE-11 | The system shall not give an ETag to a 404 or a redirect. |

## Design

- A theme page's ETag is a hash of the view record the theme would receive, plus a value picked at
  startup. Building the record is cheap, because the posts are in memory. Rendering is the costly
  part, and a 304 skips it.
- The startup value covers what the record cannot see: a new theme, new templates or new code.
  Each needs a restart, and a restart changes every ETag (CACHE-8).
- The record already holds the visible posts and the footer's year, so a post going live or a new
  year changes the ETag with no extra work.
- The feed, the sitemap and robots.txt have no theme. Their ETag is a hash of the body.
- `no-cache` means "store it, but ask first". With an ETag, asking costs one small request.
- Weak ETags, because a server that compresses responses changes the bytes, not the meaning.

## Out of scope

- A `max-age` setting. A reader could see a scheduled post late, and the 304 is already cheap.
  Add it when someone asks, for example for a CDN.
- `Last-Modified` on pages. A page changes when a scheduled post goes live, so no post's date is
  the page's date. The ETag carries that change.
- The same ETag across several copies of the app. Each copy picks its own startup value, so a
  crawler that reaches another copy gets a 200. That is slower, never wrong.
- Theme CSS and post pictures. Spring's resource handling and `BundleImageController` already send
  `Last-Modified` and answer 304.
- The static export. The host sets caching headers for files.
