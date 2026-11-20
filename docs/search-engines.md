# Search engines and AI crawlers

Beansprout tells crawlers what they may read, and lets them skip pages that have not changed.
You get both with no setup. This needs Beansprout 0.3.1 or newer.

## What every page has

Each page gets a description, a canonical link and Open Graph tags. Each post also gets
structured data (`BlogPosting`) for Google. [Start a new blog](new-blog.md#what-search-engines-see)
has the three things that make them better: a `summary`, an `updated` date and `site-url`.

Nothing else is needed for AI search. Google says AI Overviews have no extra requirements, and
that it ignores llms.txt. A fast page with clean HTML and structured data is the whole recipe.

## robots.txt

Crawlers read `/robots.txt` before anything else. Beansprout serves one at your site's root:

```text
User-agent: *
Allow: /

Sitemap: https://example.com/blog/sitemap.xml
```

Every crawler may read every page, and Google and Bing find your sitemap without you submitting
it. The `Sitemap:` line uses `beansprout.site-url`, or the address the request came in on.

### Keep AI training crawlers out

Some AI companies run one crawler to train their models and another for their search. You can
block the first kind and keep the second:

```yaml
beansprout:
  robots:
    ai-training: block
```

The file then gets one more group:

```text
User-agent: GPTBot
User-agent: ClaudeBot
User-agent: Google-Extended
User-agent: Applebot-Extended
User-agent: CCBot
Disallow: /blog
```

These five crawlers skip the blog. Search crawlers such as OAI-SearchBot and Claude-SearchBot are
not named, so they may still read it, and your posts can still show up in AI search.

`ai-training` takes `allow`, the default, or `block`. Any other value stops the app at startup,
with a message that names both. That includes a blank value, so a half-finished line never
quietly means `allow`.

The rule covers the blog only, at your `base-path`. `Disallow: /blog` matches by prefix, so it also covers an app page
whose path starts the same way, like `/blogroll`. The rest of your app is not touched.

### When Beansprout steps aside

Beansprout serves robots.txt only when your app has none of its own:

- **Your own file wins.** Put `robots.txt` in `src/main/resources/static/`, or another of your
  static locations, and Spring serves yours instead. Do this to write one rule for the whole
  site.
- **Your own controller wins.** An app that maps `/robots.txt` itself still starts, and its
  mapping answers.
- **A context path turns it off.** With `server.servlet.context-path` set, the blog is not at the
  root, and crawlers only read robots.txt at the root. The app logs this once at startup.
- **You can turn it off:** `beansprout.robots.enabled: false`.

## Pages that have not changed

Every page carries an ETag, a short fingerprint of what is on it. A crawler or browser that has
seen the page sends the ETag back. When nothing changed, Beansprout answers `304 Not Modified`
with no body, and does not render the page at all. Google asks sites to do this. It saves your
server's time and your crawl budget.

You can see it with curl. First get the page's ETag:

```bash
curl -s -i localhost:8080/blog | grep -i etag
```

It prints a line such as `ETag: W/"b56ad270ab680330566f3f2851eafaa2"`. Send that value back:

```bash
curl -s -o /dev/null -w "%{http_code}\n" -H 'If-None-Match: W/"b56ad270ab680330566f3f2851eafaa2"' localhost:8080/blog
```

With your own value in place of the one above, it prints `304`.

The index and its numbered pages, posts, tag pages, the archive, the feed, the sitemap and
robots.txt all get one. A 404 and a redirect do not. Each also gets `Cache-Control: no-cache`,
which means "keep it, but ask before you use it again". With an ETag, asking costs one small
request.

A page's ETag changes when what it shows changes:

- **A scheduled post goes live.** The index, the archive, its tags, the feed and the sitemap get
  new ETags at the post's time, with no restart.
- **You edit a post** while `beansprout.posts.reload` is on.
- **The app restarts.** A new theme, new templates or new code all need a restart, so every ETag
  changes then.

If you run more than one copy of the app, each copy has its own ETags. A crawler that reaches
another copy gets the full page. That is slower, never wrong.

## The static export

The [static export](static-export.md) writes `robots.txt` at the root of its folder, with the same
rules. With a context path, as for a GitHub project site, it writes none. ETags do not apply
there: your host sets the caching headers for files.
