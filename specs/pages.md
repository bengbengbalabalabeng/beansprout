# Spec: pages

Goals 2, 6 and the start of 5 and 8 in PRODUCT.md: the blog pages, rendered by the Longhand theme in JTE.

## Why

Posts exist but nobody can read them. This spec serves the index, post, tag and archive pages
with no controller or template written by the app.

## Routes

| Page | Address |
| --- | --- |
| Index | `{base}` and `{base}/page/{n}` |
| Post | `{base}/{slug}` |
| Tag | `{base}/tags/{tag}` and `{base}/tags/{tag}/page/{n}` |
| Archive | `{base}/archive` |

`{base}` is `beansprout.base-path`, `/blog` unless set.

## Requirements

| ID | Requirement |
| --- | --- |
| PAGES-1 | The system shall serve the index at `{base}`, with the newest `beansprout.page-size` posts as cards. The page size defaults to 10. |
| PAGES-2 | When there are more posts than one page holds, the system shall serve page n at `{base}/page/{n}` with links to the newer and the older page. |
| PAGES-3 | When a request asks for page 1 at `{base}/page/1`, the system shall redirect permanently to `{base}`. |
| PAGES-4 | If a page number is not a whole number from 1 to the last page, then the system shall answer 404. |
| PAGES-5 | When a request asks for `{base}/{slug}` of a published post, the system shall serve the post page with its title, date, reading time, tags and body. |
| PAGES-6 | The post page shall link to the next newer and the next older published post, and say so when there is none. |
| PAGES-7 | The system shall serve a page for each tag at `{base}/tags/{tag}`, with that tag's posts newest first, paginated like the index. |
| PAGES-8 | The system shall group tags by their slug, so `Spring Boot` and `spring boot` are one tag, shown with the spelling of its newest post. |
| PAGES-9 | The system shall show every tag with its count of published posts, most used first. |
| PAGES-10 | The system shall serve the archive at `{base}/archive`, with every published post grouped by year, newest year first. |
| PAGES-11 | If a request asks for a post, tag or page that does not exist or is hidden, then the system shall answer 404 with the theme's not-found page. |
| PAGES-12 | When there are no posts, the system shall serve the index with status 200 and no cards. |
| PAGES-13 | The system shall show `beansprout.title` as the blog's name on every page. It defaults to `Blog`. |
| PAGES-14 | The system shall HTML-escape every value it shows, except the post body. |
| PAGES-15 | When `beansprout.web.enabled` is false, the system shall register no blog pages, and the posts service shall still be a bean the app can call. |
| PAGES-16 | If no theme is on the classpath, then the system shall start and register no blog pages. |
| PAGES-17 | The system shall render the pages with the Longhand theme, written in JTE, when `beansprout-theme-longhand` is on the classpath. |
| PAGES-18 | The system shall hand a theme only view records, whose values are strings, booleans, lists or other view records. |
| PAGES-19 | The system shall serve the Longhand stylesheet with the theme, so the pages need no other file from the app. |

## Out of scope

- Thymeleaf and FreeMarker versions of the theme (the rest of goal 5).
- Replacing one page of a theme, and choosing a theme with a property (the rest of goal 8).
- Search, subscribe, about and author pages. Longhand has them, Beansprout has no data for them yet.
- A writer's photo and a lead picture per post.
