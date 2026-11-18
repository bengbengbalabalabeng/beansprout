# Spec: static export

Static site export, from danvega/beansprout#1. Dan approved it, with every choice as recommended,
on 17 November 2026.

## Why

Some people only want a blog. Today they still need a JVM host to serve it. With an export,
they write and preview in the app, then publish plain files to GitHub Pages, Netlify or
Cloudflare Pages.

`Pages.urls()` names every blog page, but a crawl of the sample found what a list of pages
misses: stylesheets from jars, the app's own pictures, pictures in post bundles, the feed, the
sitemap and the not-found page. This spec covers all of them.

## How it looks

```
./mvnw spring-boot:run -Dspring-boot.run.arguments=--beansprout.export.enabled=true
java -jar app.jar --beansprout.export.enabled=true --beansprout.export.output=site
```

The app starts on a free port, fetches each blog address from itself, writes the files and
stops. For the sample:

```
target/blog/
  .beansprout-export            marks a folder this export may replace
  .nojekyll                     GitHub Pages serves files as they are
  index.html                    sends a reader at / to /blog
  404.html                      the theme's not-found page
  blog.html                     /blog
  blog/archive.html             /blog/archive
  blog/hello-beansprout.html    /blog/hello-beansprout
  blog/tags/jte.html            /blog/tags/jte
  blog/feed.xml                 /blog/feed.xml
  blog/sitemap.xml              /blog/sitemap.xml
  blog/posts-with-their-pictures/cover.png
  beansprout/code.css           from the starter's jar
  beansprout/longhand/longhand.css
  images/terminal-logs.svg      from the app's static/ folder
```

A GitHub project site lives under a path, like `https://dan.github.io/notes/`. For that, export
with the app's context path set to the same path:

```
java -jar app.jar --beansprout.export.enabled=true \
  --server.servlet.context-path=/notes --beansprout.site-url=https://dan.github.io
```

Every link then starts with `/notes`, and the files are written without it.

## Choices for Dan

Dan approved the recommendation in every row (17 November 2026). The requirements below follow
them.

1. **Command shape.** Options: a Maven plugin goal, a property on the app itself, or a separate
   CLI. Recommended: the property, `--beansprout.export.enabled=true`. It works the same from
   Maven, Gradle, a jar or CI, and needs no new module. A plugin leaves Gradle out, and a CLI
   would need the app's classpath anyway to find its theme and static files.
2. **Output folder default.** Options: `target/blog`, `site/`, `dist/`. Recommended:
   `target/blog`. `mvn clean` removes it and git already ignores `target/`. Not `target/site`,
   which `mvn site` uses. Gradle users set `beansprout.export.output=build/blog`.
3. **How the export renders.** Options: fetch each address from the running app over HTTP, or
   call `Pages` and the theme in-process. Recommended: over HTTP, on a free port on 127.0.0.1.
   Every file then has the exact bytes a reader gets live, including what Spring serves from
   jars and `static/`, and the verifier checks the same path.
4. **File layout.** Options: `blog/hello.html` or `blog/hello/index.html`. Recommended:
   `blog/hello.html`. All three hosts serve it at `/blog/hello`, which matches the canonical
   link. A folder with `index.html` makes hosts redirect to `/blog/hello/`. Not checked yet:
   how each host treats `blog/x.html` next to a `blog/x/` folder of bundle pictures. The first
   build step deploys a test site to each host. If one fails, switch to folders.
5. **Where it ships.** Options: in the starter, or its own module and starter. Recommended: in
   `beansprout-spring-boot-autoconfigure`, package `dev.danvega.beansprout.export`. It needs no
   new dependency: the JDK's HTTP client and file API are enough. It is off unless asked for.
6. **Scheduled posts.** Options: a daily CI rebuild in the docs, or nothing. Recommended: a new
   `docs/static-export.md` with a GitHub Actions job that exports and deploys on every push and
   once a day. A post dated tomorrow then goes live within a day of its date. Netlify and
   Cloudflare Pages get their build command and output folder.
7. **`beansprout.site-url` is required.** Without it, the feed, the sitemap and every canonical
   link would point at `http://localhost:<port>`. Recommended: the export refuses to start
   without it.
8. **A folder that already has files.** Options: always empty it, never empty it, or empty it
   only if an earlier export made it. Recommended: the last. Deleted posts then disappear from
   the site. A folder like `docs/` that holds other work is never touched.
9. **Links to app pages outside the blog.** A theme or a post may link to `/about`, served by
   the app's own controller. Recommended: leave it out and log a warning. The export covers
   the blog, not the app.
10. **The site root.** With the blog at `/blog`, `https://dan.github.io/` would be empty.
    Options: a small `index.html` that sends readers to the blog, nothing, or allow an empty
    base path (a change to routes and `Links`). Recommended: the small redirect page.
11. **The 404 page names an address.** Live, it says "Nothing lives at /blog/xyz". A static
    `404.html` cannot know the address. Recommended: render it with no address, and each theme
    leaves that line out when the address is empty. This touches all five themes.
12. **Links in a post's body ignore the context path, today, live too.** A `picture:` gets it,
    but `[see](/blog/other)` or `![](/images/x.png)` in the Markdown does not. Under a context
    path, live or exported, those links break. Recommended: fix it here (EXPORT-30), because a
    GitHub project site is where people will hit it first. It changes core and the live pages,
    so it is a multi-module change.

## Requirements

| ID | Requirement |
| --- | --- |
| EXPORT-1 | When the app starts with `beansprout.export.enabled=true`, the system shall write the blog as static files to the output folder. |
| EXPORT-2 | When the export finishes, the system shall stop the app. |
| EXPORT-3 | The system shall use `beansprout.export.output` as the output folder, and `target/blog` when it is not set. |
| EXPORT-4 | When `beansprout.export.enabled` is not `true`, the system shall write no export files. |
| EXPORT-5 | When the export runs, the system shall serve the app on a free port chosen at start, so a running copy of the app does not block it. |
| EXPORT-6 | When the export runs, the system shall listen only on `127.0.0.1`. |
| EXPORT-7 | When the export succeeds, the system shall exit with status 0. |
| EXPORT-8 | If the export fails, then the system shall exit with a status other than 0. |
| EXPORT-9 | When the export succeeds, the system shall log the output folder and the number of files it wrote. |
| EXPORT-10 | If `beansprout.site-url` is not set, then the system shall stop before writing anything, with a message that says to set it. |
| EXPORT-11 | If `beansprout.site-url` has a path after its host, then the system shall stop before writing anything, with a message that says to set the path as `server.servlet.context-path`. |
| EXPORT-12 | If no theme renders the blog's pages, then the system shall stop before writing anything, with a message that says to add a theme. |
| EXPORT-13 | If the output folder holds files but no `.beansprout-export` file, then the system shall stop without changing the folder, with a message that names it. |
| EXPORT-14 | When the output folder holds a `.beansprout-export` file, the system shall delete everything in the folder before it writes. |
| EXPORT-15 | The system shall write a `.beansprout-export` file at the root of the output folder. |
| EXPORT-16 | The system shall write one file for every address in `Pages.urls()`. |
| EXPORT-17 | The system shall write the HTML page at address `/a/b` to the file `a/b.html` in the output folder. |
| EXPORT-18 | The system shall write the feed to `{base}/feed.xml` in the output folder. |
| EXPORT-19 | The system shall write the sitemap to `{base}/sitemap.xml` in the output folder. |
| EXPORT-20 | For every `href` and `src` on an exported page that points into the app at a file that is not an HTML page, the system shall write that file at its address. |
| EXPORT-21 | For every `url(...)` in an exported stylesheet that points into the app, the system shall write that file at its address. |
| EXPORT-22 | If an `href`, `src` or `url(...)` the export follows does not answer 200, then the system shall fail the export with a message that names the address and the page or stylesheet that points to it. |
| EXPORT-23 | If an exported page links to an HTML page that is not in `Pages.urls()`, then the system shall leave that page out and log a warning that names the link and the page it is on. |
| EXPORT-24 | The system shall write `404.html` at the root of the output folder, holding the theme's not-found page. |
| EXPORT-25 | The system shall render `404.html` with an empty requested address. |
| EXPORT-26 | When the not-found page's requested address is empty, each theme shall leave out the line that names it. |
| EXPORT-27 | The system shall write `index.html` at the root of the output folder, with a meta refresh and a plain link to `{base}`. |
| EXPORT-28 | The system shall write an empty `.nojekyll` file at the root of the output folder. |
| EXPORT-29 | The system shall write each file at its address without the app's context path. |
| EXPORT-30 | When the app has a context path, the system shall put it in front of every link and image in a post's body that starts with a single `/`. |
| EXPORT-31 | The system shall write each file for an address with the same bytes the live app answers for that address. |
| EXPORT-32 | If a post is a draft, then the system shall write no file for its page or for any file in its bundle. |
| EXPORT-33 | If a post is dated after the moment the export starts, then the system shall write no file for its page or for any file in its bundle. |
| EXPORT-34 | The system shall write no file for the MCP server's address, even when the MCP starter is on the classpath. |
| EXPORT-35 | If the system cannot create or write a file in the output folder, then it shall fail the export with a message that names the file and the cause. |

EXPORT-22 is for a file: an answer that is not an HTML page. EXPORT-23 is for a page: an answer
that is HTML, whatever its status, such as the not-found page for a draft or a post dated later.
A redirect is left out with a warning too, because a static host has none.

EXPORT-31 is what the verifier checks. A new test file in `beansprout-sample` reuses
`SitemapCrawl`: for every page in the crawl and every internal target, the exported file exists
and its bytes match the live response. `SitemapCrawlTests` itself stays as it is. `404.html`, `index.html`, `.nojekyll` and `.beansprout-export` have no live
address, so EXPORT-24 to EXPORT-28 and EXPORT-15 cover them instead.

## Docs

`docs/static-export.md`: the command, the output folder, a GitHub project site with a context
path, and a GitHub Actions job that exports and deploys on every push and once a day, so a
scheduled post goes live within a day of its date. Netlify and Cloudflare Pages get their
build command and output folder.

## Out of scope

- The app's own pages and controllers outside the blog. A link to one is left out with a warning.
- Content types and headers. A static host picks the type from the file's extension, so the
  feed is served as XML, not `application/atom+xml`.
- Redirects such as `/blog/page/1` to `/blog`. No exported page links to them.
- Links to other hosts. They are not fetched or checked.
- The MCP server. A static site has no server, so agents cannot read it or start drafts there.
  A writer can still run the app locally with MCP, then export.
- Deploying. The export writes files. CI or the host publishes them.
- A Maven or Gradle plugin, and a separate CLI.
- Exporting only what changed since the last run.
- A blog at the site root, with an empty base path.
