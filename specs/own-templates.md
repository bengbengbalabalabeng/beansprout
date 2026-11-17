# Spec: own templates

A guide for an app that wants Beansprout's blog in its own look, without one of the themes.
Dan asked for it on 16 November 2026.

## Why

The code already lets an app render the pages itself, but nothing says how, and no test holds
the promise. A guide that names `ThemeRenderer` and the page records makes them public API, so
a test has to back each thing the guide says.

## How it works

There are two ways, and `docs/own-templates.md` shows both.

1. **Your templates, Beansprout's pages.** The app defines a `ThemeRenderer` bean. Beansprout
   keeps the routes, the 404s, the feed and the sitemap. For each page it hands the renderer a
   template name and a view record, and serves the HTML that comes back.
2. **Your pages too.** The app leaves out the theme jar and writes its own controllers, calling
   `Pages` or `Posts`. PAGES-15, PAGES-16 and FEEDS-1 already cover this.

| Template name | View record |
| --- | --- |
| `index` | `IndexPage` (also for `/page/{n}`) |
| `post` | `PostPage` |
| `tag` | `TagPage` (also for `/tags/{tag}/page/{n}`) |
| `archive` | `ArchivePage` |
| `not-found` | `NotFoundPage` |

## Requirements

| ID | Requirement |
| --- | --- |
| OWN-1 | When an app defines its own `ThemeRenderer` bean and no theme is on the classpath, the system shall serve the blog pages with it. |
| OWN-2 | When an app defines its own `ThemeRenderer` bean and a theme is on the classpath, the system shall serve the blog pages with the app's renderer. |
| OWN-3 | The system shall pass the renderer the template name and the view record from the table above. |
| OWN-4 | If a page does not exist, then the system shall answer 404 with what the app's renderer returns for `not-found`. |

## Out of scope

- Replacing one page of a theme and keeping the rest. That is the open question on overrides.
- Helpers for a given engine, such as a ready-made Thymeleaf renderer. The guide shows the few
  lines an app writes.
- Reactive (WebFlux) apps. The pages are servlet only.
