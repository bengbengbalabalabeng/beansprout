# Spec: tag-feeds

Rank 10 in `reports/Blog platform features for Beansprout.md`, and part of 0.4.0, "keep readers
around".

## Why

A reader who only wants the Java posts must follow the whole blog today. Hugo, Bear and Ghost give
each tag its own feed.

## Requirements

| ID | Requirement |
| --- | --- |
| TAGFEED-1 | The system shall serve an Atom feed for each tag at `{base}/tags/{tag}/feed.xml`, as `application/atom+xml`. |
| TAGFEED-2 | The system shall list the 20 newest published posts with that tag in the tag's feed. |
| TAGFEED-3 | The system shall title a tag's feed with the blog's title and the tag's name, such as "My Blog: Java". |
| TAGFEED-4 | The system shall give each entry of a tag's feed the same values as its entry in the main feed. |
| TAGFEED-5 | The system shall give a tag's feed a `self` link to its own address. |
| TAGFEED-6 | If no published post has the tag, and no post's alias is `{base}/tags/{tag}/feed.xml`, then the system shall answer it with 404. |
| TAGFEED-7 | Every theme shall link each tag page to its feed with `<link rel="alternate" type="application/atom+xml">`. |
| TAGFEED-8 | Every theme shall keep each tag page's link to the main feed. |
| TAGFEED-9 | When the static export runs, the system shall write each tag's feed to `{base}/tags/{tag}/feed.xml`. |
| TAGFEED-10 | The system shall treat a tag's feed as a blog response under `specs/caching.md`, with an ETag, 304s and `Cache-Control`. |

## Notes

- `{tag}` is the tag's slug, as on tag pages (`/blog/tags/spring-boot`).
- The route does not clash with post pictures. `BundleImageController` maps `{base}/{slug}/{*file}`,
  and Spring prefers the more specific `/tags/{tag}/feed.xml`, as it already does for
  `/tags/{tag}/page/{number}`.
- A tag feed that does not exist is an address like any other the blog does not serve. A post's
  alias there answers with its 301 (ALIAS-1). When the app also has a file at that alias's path,
  the file answers instead (ALIAS-11). A live tag's feed is a page the blog serves, so an alias at
  it is ignored with a warning (ALIAS-7).
- TAGFEED-9 needs no new export code. The export follows every `href` on a page, the head's
  `<link>` tags included (EXPORT-20), so it finds each tag feed through TAGFEED-7.

## The view contract

`TagPage` gains `feed`, the address of the tag's feed. TAGFEED-7 touches the layout or tag
template of all eight themes: Calling Card, Dateline, Linocut, Longhand, Marginalia, Onionskin, Plainsong
and Small Machines.

## Out of scope

- Tag feeds in the sitemap. Sitemaps list pages.
- A feed per year or per page of the archive.
- RSS 2.0, as for the main feed.
