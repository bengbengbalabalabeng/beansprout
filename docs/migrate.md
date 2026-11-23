# Move a blog from Jekyll or Hugo

Beansprout reads Jekyll and Hugo posts as they are. You copy the files, start the app, and fix
the few posts it tells you about. Old links keep working through redirects. This needs Beansprout
0.4.0 or newer.

If your app has no blog yet, [start a new blog](new-blog.md) first, then come back here.

## 1. Copy the posts

Copy the posts into `src/main/resources/posts/`, folders and all:

- From Jekyll, the files in `_posts/`.
- From Hugo, the files in `content/posts/`, or wherever your posts live.

Keep the file names. A Jekyll name like `2024-05-01-first-post.md` gives the post its date,
unless the post has a `date:` line of its own. The post's address leaves the date out:
`/blog/first-post`.

Beansprout reads `.md` files only. Rename any `.markdown` file to `.md`, or it is left out with
no message. Leave out Hugo's `_index.md` files. They describe a section, not a post.

Two names that match once the date is gone, like `2023-06-01-weekly.md` and
`2023-06-08-weekly.md`, give the same address. Beansprout keeps the first by name and skips the
other, with a log line. Give one of them a `slug:` line.

To try it without copying, point Beansprout at the old folder on disk:

```yaml
beansprout:
  posts:
    location: file:../my-old-blog/_posts/
```

## 2. Copy the pictures

Pictures that live outside the posts, like Jekyll's `assets/` or Hugo's `static/images/`, go in
`src/main/resources/static/`. Spring Boot serves that folder at the root. So
`src/main/resources/static/images/first.png` answers at `/images/first.png`, as it did before.

A Hugo page bundle works as it is. A folder with an `index.md` and its pictures next to it is one
post, and `![A cat](cat.png)` shows the picture from that folder.

## 3. Start the app, open the blog, then read the log

```bash
./mvnw spring-boot:run
```

Open `http://localhost:8080/blog`. Beansprout reads the posts on the first request, not at
startup, so the log has nothing to say until then. After that it logs one line for each post it
could not read, and why:

```
Beansprout skipped spaced-date.md: its date '2024-05-01 10:00:00 -0500' is not a date. Write it like 2026-11-16 or 2026-11-16T09:30.
```

The post is left out, and the rest of the blog runs. Fix the file and start again. The fixes
for the common cases are below.

## What Beansprout reads

These keys keep working with no change:

| Key | What it becomes |
| --- | --- |
| `title`, `tags`, `draft`, `slug` | The same as before |
| `description` | The summary under the title and in search results. `summary` wins when both are there. |
| `image` | The post's picture, when it is a single value. `picture` wins when both are there. |
| `lastmod` | The updated date. `updated` wins when both are there. |
| `canonical_url` | The address where the post first appeared, for search engines |
| `aliases`, `redirect_from` | Old addresses that redirect to the post with a 301 |
| `author` | The post's writer, when it is a plain name |

Hugo's TOML frontmatter, between `+++` lines, is read too: text, dates, `true` and `false`, and
lists like `tags = ["hugo", "java"]`.

Beansprout ignores the keys it does not use, so they can stay in the file. These include
`layout`, `categories`, `permalink`, Hugo's `url`, `weight`, `series`, `images` and `toc`, and
tables like `[params]`.

## Keep your old links

A post's address in Beansprout is `/blog/` and its slug, like `/blog/first-post`. List the old
address in `redirect_from` or `aliases`, and Beansprout answers it with a 301 to the new one:

```yaml
redirect_from:
  - /2024/05/01/first-post.html
```

An alias is a path from your site's root. It matches with or without a trailing slash. So Hugo's
`/posts/first-post/` works as an alias.

A Jekyll post with no `slug:` line also redirects from `/blog/2024-05-01-first-post`, with no
alias to write. A post with a `slug:` line loses that address, so add it to `redirect_from` if you need
it. Either way, Jekyll's own addresses, like `/2024/05/01/first-post.html`, still need
`redirect_from`.

Jekyll's `permalink` and Hugo's `url` are ignored. If a post sets one, copy its value into
`redirect_from` or `aliases`. A pattern like `/:year/:month/:title/` must be written out for each
post, like `/2024/05/first-post/`.

The [static export](static-export.md) writes a page at each alias that sends the reader on, since
a static host sends no redirects.

## Fix these by hand

**`published: false`.** Jekyll hides a post with this line. Beansprout does not read it, so the
post goes live. Change it to `draft: true`.

**A date with a space and a time zone.** Jekyll accepts `date: 2024-05-01 10:00:00 -0500`.
Beansprout skips that post. The same goes for `lastmod` and `updated`. Write each one with a `T`
and a colon in the zone:

```yaml
date: 2024-05-01T10:00:00-05:00
```

**A picture path that does not start with `/`.** `image: assets/pic.png` skips the post. Write
`image: /assets/pic.png`, and put the file in `src/main/resources/static/assets/`.

**A nested picture.** Some Jekyll themes write `image:` with `path:` and `alt:` under it.
Beansprout keeps the post but shows no picture. Write it as two keys:

```yaml
picture: /images/nested.png
picture-alt: A nested picture
```

**Tags with spaces between them.** `tags: java spring` is one tag, "java spring". A tag can have
a space in it, like "Spring Boot". Write a list:

```yaml
tags: [java, spring]
```

**A Hugo alias without a `/`.** Hugo reads `aliases: [old-name]` from the post's folder.
Beansprout ignores it. It logs a warning once, on the first request that no page answers, such
as a mistyped address. Write the full path, like `/posts/old-name/`.

**Liquid tags and Hugo shortcodes.** `{% highlight %}`, `{{ site.baseurl }}` and
`{{< youtube >}}` show up as plain text. Use Markdown instead: a fenced code block for code, and
plain HTML for an embed.

## Before you switch

- Set `beansprout.site-url` to your real address, such as `https://blog.example.com`. Canonical
  links, the feed and the sitemap use it.
- Open a few old addresses and check each one redirects to its post.
- Keep `canonical_url` on posts that first appeared somewhere else, like dev.to. Search engines
  then credit that page, not this one.

[Search engines and AI crawlers](search-engines.md) covers robots.txt and the sitemap.
