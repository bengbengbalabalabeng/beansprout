# Publish as static files

You don't need a Java host to publish a Beansprout blog. Write and preview in your app as usual.
Then one run of the app writes the whole blog as plain files for GitHub Pages, Netlify or
Cloudflare Pages. This needs Beansprout 0.3.0 or newer.

## 1. Set your site's address

The feed, the sitemap and every page's canonical link need the address the files will live at.
Add it to `application.yaml`:

```yaml
beansprout:
  site-url: https://your-name.github.io
```

The export stops if `site-url` is not set.

## 2. Export

From your app's folder:

```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments=--beansprout.export.enabled=true
```

Or from a packaged jar:

```bash
java -jar target/my-blog-0.0.1-SNAPSHOT.jar --beansprout.export.enabled=true
```

The app starts on a free port on `127.0.0.1`, so it never clashes with a copy you already have
running. It writes the blog to `target/blog`, then stops. To write somewhere else, add
`--beansprout.export.output=build/blog`.

You get:

- every page: `blog.html`, `blog/archive.html`, `blog/<post>.html`, `blog/tags/<tag>.html`
- `blog/feed.xml` and `blog/sitemap.xml`
- every file the pages use: the theme's CSS, your pictures from `static/`, and the pictures in
  each post's folder
- `404.html`, the theme's not-found page
- `index.html`, which sends a reader at `/` to `/blog`
- `.nojekyll`, so GitHub Pages serves the files as they are

Drafts and posts dated in the future are left out.

The export stops, and writes nothing, if a page links to a file the app does not have. The
message names the link and the page. A link to a page that is not part of the export is left out
with a warning: a page of your app outside the blog, such as `/about`, a draft, a post dated
later, or a redirect.

Run it again any time. The folder holds a `.beansprout-export` file, so the next export knows it
may empty the folder first, and a post you deleted disappears. A folder that has other files and
no `.beansprout-export` file is never touched: the export stops and names it.

## 3. A GitHub project site

A project site lives under a path, like `https://your-name.github.io/my-blog/`. Put that path in
the app's context path, and keep it out of `site-url`:

```bash
java -jar target/my-blog-0.0.1-SNAPSHOT.jar --beansprout.export.enabled=true \
  --beansprout.site-url=https://your-name.github.io --server.servlet.context-path=/my-blog
```

Every link then starts with `/my-blog`, including links you wrote in a post as `/blog/other-post`
or `![](/images/chart.png)`. The files are still written without `/my-blog` in front, because
GitHub serves the folder at that path.

## 4. Publish with GitHub Actions

This workflow exports and publishes on every push to `main`, and once a day. The daily run is
what makes a scheduled post appear: a post dated tomorrow goes live on the first run after its
date. The date is read in the build machine's time zone, which is UTC on GitHub's runners. To
use yours, set `beansprout.time-zone`, such as `Europe/Amsterdam`.

Save it as `.github/workflows/blog.yml`. In the repository's **Settings**, under **Pages**, set
the source to **GitHub Actions**.

```yaml
name: Blog

on:
  push:
    branches: [main]
  schedule:
    - cron: "0 6 * * *"
  workflow_dispatch:

permissions:
  contents: read
  pages: write
  id-token: write

concurrency:
  group: pages
  cancel-in-progress: true

jobs:
  publish:
    runs-on: ubuntu-latest
    environment:
      name: github-pages
      url: ${{ steps.deploy.outputs.page_url }}
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: 25
          cache: maven
      - run: ./mvnw --batch-mode --quiet -DskipTests package
      - run: java -jar target/*.jar --beansprout.export.enabled=true
      - uses: actions/upload-pages-artifact@v3
        with:
          path: target/blog
      - id: deploy
        uses: actions/deploy-pages@v4
```

If `target/` holds more than one jar, name yours instead of `target/*.jar`. For a project site,
add `--server.servlet.context-path=/my-blog` to the export step, as in step 3.

## Netlify and Cloudflare Pages

Both serve the same folder. Their own build machines may not have Java 25, so the simplest path
is the workflow above with its last two steps swapped for the host's command line:

- Netlify: `npx netlify-cli deploy --dir=target/blog --prod`, with `NETLIFY_AUTH_TOKEN` and
  `NETLIFY_SITE_ID` as repository secrets.
- Cloudflare Pages: `npx wrangler pages deploy target/blog --project-name=my-blog`, with
  `CLOUDFLARE_API_TOKEN` and `CLOUDFLARE_ACCOUNT_ID` as repository secrets.

## What a static site can't do

- **Scheduled posts need a rebuild.** On a Java host a post appears at its time with no deploy.
  As files, it appears on the next export after its date. That's why the workflow runs daily.
- **No MCP server.** Static files have no server, so an agent can't read the blog or start drafts
  there. Run the app on your machine with MCP to write, then export.
- **Content types come from the host.** It picks them from the file's extension, so the feed is
  served as XML rather than `application/atom+xml`. Feed readers accept both.
