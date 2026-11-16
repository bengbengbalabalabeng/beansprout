<p align="center">
  <img src="images/beansprout-mascot.png" alt="Beansprout, a coffee bean with a green sprout on its head" width="240">
</p>

# Beansprout

Add a Markdown blog to the Spring Boot app you already run.

You write posts as Markdown files. Beansprout turns them into a blog inside your app: an index,
a page per post, tag pages, an archive, an Atom feed and a sitemap. No controllers, no
templates, no database.

## Why

Java developers who want a blog today have two choices. Run a separate platform like Ghost or
WordPress next to their app, or build posts, tags, feeds and sitemaps by hand. Beansprout is
the third choice: add a dependency to the app you already have.

**Status:** early. Nothing is released yet. [PRODUCT.md](PRODUCT.md) has the plan.

## Tested with

Java 25 and Spring Boot 4.1.2. Pages render with [JTE](https://jte.gg).

## Add it to your app

Nothing is on Maven Central yet, so install it locally first:

```bash
./mvnw install -DskipTests
```

Then add the starter and the default theme, Longhand:

```xml
<dependency>
    <groupId>dev.danvega</groupId>
    <artifactId>beansprout-spring-boot-starter</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
<dependency>
    <groupId>dev.danvega</groupId>
    <artifactId>beansprout-theme-longhand</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

Put a post in `src/main/resources/posts/hello.md`:

```markdown
---
title: Hello, Beansprout
date: 2026-11-16
tags: [spring boot, java]
---
My first post.
```

Start your app and open `http://localhost:8080/blog`.

A post with `draft: true` stays hidden. A post dated in the future appears on that date, with no
redeploy. [specs/posts.md](specs/posts.md) has every frontmatter rule.

## Settings

All optional, in `application.yaml`:

```yaml
beansprout:
  title: My Blog                    # default: Blog
  description: Notes on Java.       # default: empty
  base-path: /blog                  # default: /blog
  page-size: 10                     # default: 10
  site-url: https://example.com     # for the feed and sitemap. Default: taken from the request
  time-zone: Europe/Amsterdam       # default: the server's zone
  posts:
    location: classpath:posts/      # or a folder on disk, like file:./posts/
    reload: false                   # true re-reads posts on every request, for development
  web:
    enabled: true                   # false turns the pages off. Posts and Pages stay as beans.
```

## Try the sample

The repo has a sample app with a few posts:

```bash
./mvnw -q install -DskipTests && ./mvnw -pl beansprout-sample spring-boot:run
```

Open `http://localhost:8080/blog`.

## How the project is laid out

| Module | What it does |
| --- | --- |
| `beansprout-core` | The blog's logic in plain Java. Reads Markdown into posts, decides what is published, builds the data for each page, writes the feed and sitemap. No Spring. |
| `beansprout-spring-boot-autoconfigure` | The Spring part. Reads the `beansprout.*` settings, creates the beans, and holds the controllers. |
| `beansprout-spring-boot-starter` | The dependency you add. It has no code of its own. |
| `beansprout-theme-longhand` | The look: Longhand's templates and stylesheet. |
| `beansprout-sample` | A small Spring Boot app that uses all of the above. |
| `verification` | Build checks for the repo itself, such as "every requirement has a test". |

Core has no Spring on purpose. A future MCP server or a static site export can reuse it without
a web server.

When someone opens `/blog/hello`:

1. `BlogController` takes the request.
2. `Pages` asks `Posts` for the post. `Posts` reads the Markdown and checks the post is published.
3. `Pages` returns a `PostPage`: plain text and lists, ready to show.
4. The Longhand theme turns it into HTML.

To read the code, start with
[Posts.java](beansprout-core/src/main/java/dev/danvega/beansprout/content/Posts.java) and
[Pages.java](beansprout-core/src/main/java/dev/danvega/beansprout/view/Pages.java).

## Build

You need JDK 25 or newer.

```bash
./mvnw verify
```

## License

Apache 2.0. See [LICENSE](LICENSE).
