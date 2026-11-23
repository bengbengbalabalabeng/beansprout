# Beansprout

Beansprout is an open source Spring Boot starter that adds a blog to an app you already run.
Add the dependency, put Markdown files in a folder, and the app serves a blog through the
template engine it already uses.

## Purpose

Java developers who want a blog have two choices today. They run a separate platform (Halo,
Ghost, WordPress) next to their app, or they build posts, tags, feeds and sitemaps by hand.
Nobody offers "add a blog to the Spring Boot app I already have." This starter does that, and
nothing more.

Shaped with Dan on 15 November 2026. Purpose, in his words: an open source library for others
to start their own blog.

## First useful result

A Spring Boot app that adds the starter and a folder of Markdown posts serves a working blog,
with no controllers or configuration written by the developer.

Done when:

1. **Posts come from Markdown.** A `.md` file with frontmatter (title, date, tags, summary,
   `draft`) becomes a post. The folder location is configurable. Edits show up without a
   restart in development.
2. **The blog pages exist.** Under a configurable base path (default `/blog`): an index with
   pagination, a post page with newer and older links, a page per tag, and an archive grouped
   by year.
3. **Feeds and search engines are covered.** An RSS or Atom feed of recent posts, and post
   URLs added to the app's sitemap.
4. **Drafts and scheduled posts stay hidden.** `draft: true` hides a post. A post dated in the
   future appears on that date, with no redeploy.
5. **It renders with JTE.** JTE is the theme engine and the one the docs lead with. Thymeleaf
   and FreeMarker wait until later (see Non-goals).
6. **It stays out of the app's way.** The developer can turn off the default controllers and
   call a posts service from their own.
7. **Agents can work with the blog over MCP.** An optional MCP server lets an agent list posts,
   find posts by tag, read one post, and create a draft. A draft is a new Markdown file with
   `draft: true`. The agent can never publish: a person flips `draft` to `false`. It is off
   unless the developer adds it, and it only writes to a folder on disk, never into the jar.
8. **It is easy to theme.** A theme is a package of templates, CSS and images that builds
   against the view contract. One property picks the theme. An app can replace a single page
   of a theme without copying the rest. Longhand is the first theme.

## Non-goals

- **Admin editor and database storage** (not now): publishing is a commit. An editor needs
  security, drafts in tables, migrations and image uploads, several times the first version.
- **Comments, search, newsletter signup** (not now): each is its own product decision. A theme
  can still show a static search or subscribe page.
- **Image processing, multiple authors, series** (not now): Longhand supports series,
  corrections and guest authors. They wait until the core is used.
- **Thymeleaf and FreeMarker** (not now, Dan, 16 November 2026): JTE covers the first
  release. The view contract stays engine-neutral and each engine keeps its own package, so
  adding one later is a theme and an adapter, not a redesign.
- **A showcase of blogs built with Beansprout** (not now, Dan, 17 November 2026): a page that
  highlights real blogs running on Beansprout, each with its theme and a link. The Wrap website
  and Dan's own blog would be the first entries.
- **Changelogs** (not now): the same engine fits dated, versioned entries with Quoin as a theme.
  A good second release, not the first.
- **Spring Boot 3** (not now): Boot 3.5 is past open source support, and JTE ships a separate
  starter per Boot generation.
- **Live posts and a Markdown twin** (no, Dan, 18 November 2026): picking up new posts from a
  folder or volume without a deploy, and serving `/blog/{slug}.md` to agents. Dan: "i don't want
  to do c at all". Publishing stays a commit and a deploy. The research is in
  `reports/Live content reload in Java apps.md` for the record only.
- **A standalone, hosted blog platform** (outside this project's purpose): that is Halo's and
  Ghost's ground, and it would turn a library into an operations business.
- **Living inside the Pressworks repo** (outside this project's purpose): Pressworks sells view
  layers and never ships runnable apps. This starter is a separate product that uses its themes.

## Decisions

- **The release after 0.3.0 is 0.4.0, not 0.3.1** (Dan, 23 November 2026). It adds new
  components to `view.Post`, `Site`, `TagPage` and `content.Post` with no old constructor kept,
  so code that builds them stops compiling, and `Posts.published()` now leaves out unlisted
  posts. In 0.x the middle number marks a release that can break code. The notes are in
  `docs/releases/0.4.0.md`.

- **Menu links, in 0.4.0** (Dan, 20 November 2026). `beansprout.links` adds links to every
  theme's main menu, after its own items, so a blog inside a bigger site links back to the rest
  of it. Pressworks needs it for its blog. It ships in 0.4.0 with the rest of "keep readers
  around", not as its own release. Spec rows THEME-24 to 27 in `specs/themes.md`.

- **Three new themes of Beansprout's own: Plainsong, Onionskin and Calling Card** (Dan,
  18 November 2026), picked from four directions in `reports/Personal blog design inspiration.md`
  and the mockups in `.shipit/prototype/new-theme/` (Galley was not picked). All three follow the
  reader's light or dark setting and use system fonts. Spec rows THEME-20 to 23 in
  `specs/themes.md`. `ThemesTests` pins the original five themes, so Dan extends its lists by hand.

- **0.4.0 is "bring your blog with you" and "keep readers around"** (Dan, 18 November 2026).
  Robots.txt and ETags alone are too small for a release. Migration: `aliases` with 301s,
  Jekyll and Hugo frontmatter names, a `canonical` override, image size and loading hints, and a
  migration guide. Reading: related posts, a feed per tag, reading time, a table of contents,
  theme hook slots and an `unlisted` flag. One spec per feature, as usual. Reading time already
  ships in every theme, so it is not new work. Table of contents goes to the three themes that lack
  it. Dan's calls the same day, each the recommended option: `image:` is read as the picture when it
  is a plain string (nested `image:` stays ignored, which changes the earlier "image stays
  unknown" decision); a per-post `author` name is allowed (a name only: no author pages, so
  "multiple authors" stays a non-goal); Hugo's TOML frontmatter is read for the same simple
  shapes as YAML, with no new dependency.

- **Static site export, now** (Dan, 17 November 2026), from danvega/beansprout#1. This moves it
  out of Non-goals, where it waited for the second release. One run of the app writes every
  blog page, the feed, the sitemap, a `404.html` and the files they use, for GitHub Pages,
  Netlify or Cloudflare Pages. Write and preview in the app, publish as files. Scheduled posts
  then need a rebuild on their date (a daily CI job), and the MCP server does not apply.
  Context, still true: static-only users already have Hugo, Jekyll and JBake, and Beansprout's
  edge is living inside a Spring app. The export lets someone who only wants a blog start with
  Beansprout and grow it into an app later. Spec: `specs/static-export.md`, approved with its
  12 choices as recommended: a `beansprout.export.enabled` property on the app, output in
  `target/blog`, `site-url` required, and context-path links in post bodies fixed live too.
- **The name is Beansprout** (Dan, 15 November 2026). A coffee bean (Java) sprouting
  (Spring), and a character that works as a mascot. Open on GitHub (89 repos, none above
  4 stars) and Maven Central. Domains as of 15 November 2026: `beansprout.dev`, `.io` and
  `getbeansprout.com` are registered by others; `.org` and `.app` are parked for sale at
  Afternic. `beansprout.blog`, `beansprout.sh`, `usebeansprout.dev` and `beansprouthq.dev`
  had no nameservers, so they are likely free. Not checked with a registrar.
- **Spring Boot 4.1 and Java 25** (Dan, 15 November 2026). Newest LTS and nicer library code.
  Tradeoff, accepted: Boot 4 apps still on Java 17 or 21 cannot use Beansprout.
- **Apache 2.0** (Dan, 15 November 2026), like Spring Boot.
- **GitHub repo `danvega/beansprout`, public since 16 November 2026** (Dan). It was private
  until the release setup was ready.
- **Group ID `dev.danvega`**, packages under `dev.danvega.beansprout`, like Dan's other starter
  (`browserbase-spring-boot-starter`). Easy to change before the first release.
- **Five modules**: `beansprout-core` (posts, tags, view contract; plain Java),
  `beansprout-spring-boot-autoconfigure`, `beansprout-spring-boot-starter` (no code),
  `beansprout-sample` (where the end-to-end tests run) and `verification` (repo-wide gates).
  Core is separate so the MCP server and every engine reuse it, and the build refuses web,
  Boot or engine dependencies in it.
- **Gates before features** (Dan, 15 November 2026). `AGENTS.md` is the ask, the gates are the
  check. The build runs NullAway, Error Prone with `-Werror`, ArchUnit, Maven Enforcer and a
  definition-of-done test that ties every spec requirement ID to a test. Claude Code hooks stop
  an agent from finishing or committing on a red build or a changed test, and keep a spec and
  its implementation in separate commits. `/attack` runs an adversarial review. CI runs the
  same `./mvnw verify` on GitHub.
- **Specs first.** Each feature starts as `specs/<feature>.md` with EARS requirements and IDs.
  Dan approves the spec before code.
- **Markdown files first** (Dan, 15 November 2026). No database and no admin in the first
  version.
- **Longhand ships precompiled, with its own JTE engine** (16 November 2026, built while Dan was
  away). `beansprout-theme-longhand` compiles its templates at build time into
  `dev.danvega.beansprout.engine.jte.longhand.templates` and serves its CSS from
  `META-INF/resources`. Beansprout renders it with its own `TemplateEngine`, so it works
  whatever JTE settings the app has, and the "development-mode" startup error never reaches
  Beansprout users. Tradeoff: an app cannot edit a theme template in place.
- **Longhand shows the designed details** (Dan asked, 16 November 2026: "make the default theme
  look nicer"). The writer's photo, pictures on cards and posts with captions, the lede, the
  author's name and the current menu item, from `specs/rich-pages.md`. The frontmatter keys
  follow Longhand's model: `picture`, `picture-alt`, `caption`. `image:` stays unknown, because
  Jekyll posts use it for nested data. Search, subscribe, series and about wait for features.
- **SEO tags, `updated` and JSON-LD first** (Dan, 16 November 2026), the top three from
  `reports/Blog platform features for Beansprout.md`. Spec: `specs/seo.md`. Absolute addresses
  come from `beansprout.site-url` or the request, through `Pages.forSite`. Next in the report's
  list: `aliases` with real 301s, code highlighting, CSS dark mode, ETags, robots.txt.
- **Code highlighting is a Prism port with Shiki's colors** (Dan, 16 November 2026). Core
  highlights fences on the server with Prism's grammars ported to Java (MIT notice kept) and
  Prism's class names, so any Prism theme works. `/beansprout/code.css` uses Shiki's GitHub
  Light Default and Dark Default colors. Shiki itself and TextMate engines on the JVM were set
  aside: see `specs/highlighting.md`.
- **Dark mode follows the reader's system** (Dan, 16 November 2026), CSS only, no toggle. Both
  schemes are tested for WCAG AA contrast. Spec: `specs/dark-mode.md`.
- **Railway is the default deploy** (Dan, 16 November 2026). `docs/deploy.md` uses a Dockerfile,
  not `railway.json`, which Railway stops reading on 1 December 2026.
- **Every Pressworks blog theme ships free here** (Dan, 16 November 2026): Longhand, Dateline,
  Linocut, Marginalia and Small Machines, under Apache 2.0, each its own module in `themes/`.
  This sets aside "premium themes sold on Pressworks" from the first shaping, and it conflicts
  with "Every new template is paid" in the Pressworks PRODUCT.md, which Dan updates there.
- **Specs stay public** (Dan, 16 November 2026). CI runs the done-judge, which needs them, and
  the test names point to them. The README does not mention them.
- **Two dependencies for now** (Dan, 16 November 2026): the starter and
  `beansprout-theme-longhand`. The starter never pulls in an engine, as AGENTS.md says, and JTE
  is the theme's engine. Having the theme jar bring the starter can come later.
- **The routes stay as built** (Dan, 16 November 2026): `/blog`, `/blog/page/{n}`,
  `/blog/{slug}`, `/blog/tags/{tag}`, `/blog/archive`, `/blog/feed.xml`, `/blog/sitemap.xml`,
  and the `beansprout.*` properties. Dan: "we can always change them later". Before 1.0 a
  rename is allowed, with a note in the release.
- **One BOM, published flat** (Dan, 17 November 2026): `beansprout-bom` manages every
  published artifact at one version. It is published with no parent, through
  flatten-maven-plugin in bom mode, so an app that imports it gets Beansprout's versions and not
  Spring Boot's. The docs lead with the BOM: import it, then add the starter and a theme.
- **MCP: local, or a token** (Dan, 17 November 2026): the MCP server answers only the same
  machine, and never a proxied request, unless `beansprout.mcp.token` is set; then any machine
  that sends it as a Bearer header. It ships as `beansprout-mcp-spring-boot-starter` and drafts
  go only into the posts folder on disk (`specs/mcp.md`).
- **Spring AI 2.0.2 for MCP** (Dan asked for the latest, 17 November 2026): the newest GA, and
  the one that runs on Spring Boot 4.1. 2.1 pairs with Boot 4.2 and is a release candidate. Move
  when both are GA. Spring AI 2.0 falls back to SSE without `spring.ai.mcp.server.protocol`, so
  Beansprout defaults it to streamable.
- **A theme jar brings JTE** (decided while building themes, 17 November 2026, for Dan to
  confirm): each theme depends on `jte-runtime` at compile scope, as Longhand has since 0.1.0.
  Picking a theme is picking its engine. The starter still pulls in no engine.
- **The feed and sitemap need no theme.** They are served whenever the web pages are on.
- **JTE only for now** (Dan, 16 November 2026). This replaces "all three engines from the
  start" (15 November). Thymeleaf and FreeMarker move to Non-goals as "not now". Longhand
  already exists in both in Pressworks, so they stay cheap to add later.
- **JTE is the default** (Dan, 15 November 2026), for speed. JTE templates compile to Java
  ahead of time, so pages render fast.
- **Longhand is the first theme** (Dan, 15 November 2026), taken from Pressworks. This
  replaces the earlier plan of a plain default theme with Longhand only linked. Consequence,
  accepted: Longhand becomes open source here, so the Pressworks signup no longer gates it.
- **Themeable from the start** (Dan, 15 November 2026). See goal 8.
- **An MCP server for agents** (Dan, 15 November 2026): list posts, find by tag, create drafts.
  Drafts only, so a person always decides what goes live. It ships as its own optional module,
  so apps that do not want it never pull in the MCP dependencies.
- **Open source library** (Dan, 15 November 2026), for people starting their own blog.
- **The view contract is the product.** The starter documents every value it hands a view:
  the posts, the current post, tags with counts, pagination, the archive. Every theme builds
  against that list. Its field names follow Longhand's `LonghandModel` (`Post`, `Card`, `Tag`,
  `TagCount`, `Archive`, `Year`) where they fit, so Longhand ports with little mapping.
- **A page never depends on a live request** (Dan, 16 November 2026). The posts service builds
  each page's view model from its URL alone, and one list names every blog URL (the sitemap
  uses it too). This keeps static export a small add-on later, not a rewrite.

## Assumptions (change any of these freely)

- **commonmark-java 0.29.0** renders Markdown, with GitHub tables (picked during goal 1).
  0.29.0 because it was already in the local Maven cache; 0.31 or newer is a safe bump.
- **Frontmatter is a small format, not YAML**: `key: value`, quotes, `[a, b]` or `- item`
  lists, and comments. No YAML library, so no surprises and clear errors. See `specs/posts.md`.
- **"Category" means tag** for now. Posts carry `tags` in frontmatter, and the MCP server's
  find tool searches them. Add a separate `category` field only if one tag per post turns out
  to be wanted.
- **The MCP server uses Spring AI's MCP server support**, the same stack as Dan's other MCP
  demos. Confirm the version when goal 7 starts.

## Open questions

- **How an app overrides one page of a theme.** Longhand ships precompiled (see Decisions), so
  an override has to come from the app's own templates. Settle it at the start of goal 8.
- **A hidden post can take a live post's slug.** Duplicates are settled by path before drafts
  and future posts are filtered, as POSTS-6 says. So a draft `a/hello.md` hides a live
  `b/hello.md`. Settling duplicates among visible posts only is likely better. Spec change.
- **Publishing to Maven Central now blocks deploying.** Railway, like most hosts, builds the
  app from source, and its build cannot download Beansprout until it is on Maven Central. The
  build side is ready (16 November 2026): a `release` profile, `.github/workflows/release.yml`
  on `v*` tags, and `RELEASING.md`. The `dev.danvega` namespace is verified (16 November 2026) and
  the repository is public. Still needed from Dan: a Central user token and a release signing
  key, saved as four GitHub secrets. Nothing has been published under `dev.danvega` yet.

## Current state

Goals 1, 2, 3 and 4 are built, verified and committed (344644d specs, 4c31aad code) on
16 November 2026, built while Dan was away. Not pushed. Goal 6 is half done: `beansprout.web.enabled=false` turns the pages off and `Posts` and
`Pages` are beans an app can call. The sample app serves the Longhand theme at `/blog`.

- Specs: `specs/posts.md` (POSTS-1 to 18), `specs/pages.md` (PAGES-1 to 19), `specs/feeds.md`
  (FEEDS-1 to 8). Dan has not approved them yet. Read them first.
- Modules: each theme is its own module under `themes/`, and `beansprout-bom` holds the
  versions. With more than one theme on the classpath, `beansprout.theme` picks one.
- Tests: 119 across core (65), autoconfigure (28) and sample (26), every spec ID covered.
  `/attack` took five rounds: 13 valid findings, each fixed with a failing test first, then PASS.
  Most were frontmatter edge cases and the Spring loader (encoded paths, non-UTF-8 files).
- Frontmatter rules that came out of review: unknown keys of any shape are ignored, only
  `tags` reads `[a, b]` as a list, an indented known key is refused, a file that is not UTF-8
  is skipped with a warning, and `._` files are skipped silently.
- Since then: Longhand's designed details (`specs/rich-pages.md`, RICH-1 to 12, /attack PASS),
  the README with the mascot, and two guides: `docs/new-blog.md` and `docs/deploy.md`. A fresh
  app built from the guide, a packaged jar, and the dev reload tip were all run and checked.
  The Railway steps were not run: Beansprout is not on Maven Central yet.
- Not done: page overrides and theme choice (rest of goal 8), MCP (goal 7), the rest of goal 6.
  Later: Thymeleaf and FreeMarker. Static export is built (`specs/static-export.md`, EXPORT-1 to 35, /attack PASS).

Gate note: the commit hook runs `verify` on the working tree, not on the staged files. So a
spec-only commit passes the hook while its code sits uncommitted beside it, which is how
344644d (specs) and 4c31aad (code) went in. Checked out alone, the spec commit fails `verify`,
because the done-judge wants a test for every spec ID. Fine for local history. If CI ever builds
every commit, the done-judge needs a way to skip specs whose code is not committed yet.

**First real user: the Wrap website** (Dan, 16 November 2026). `~/dev/projects/wrap-www` has a
hand-built blog (`dev.danvega.wrap.blog`, about 880 lines, Markdown in `src/main/resources/blog`).
When Beansprout is in a good spot, replace it with Beansprout and a custom theme built through
`docs/own-templates.md` option 1. Gaps to close or map first: Wrap's frontmatter says
`description` (Beansprout: `summary`), and has `cover`, `image` and a per-post `author`; it makes a
1200x630 share image per post (`ShareCard`); it shows drafts in local runs (`wrap.blog.show-drafts`);
and it checks a description is 40 to 170 characters. wrap-www pushes to main deploy to production.

Next step: Dan reviews the specs and the open questions above, then push.
