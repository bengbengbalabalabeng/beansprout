<p align="center">
  <img src="images/beansprout-mascot.png" alt="Beansprout, a coffee bean with a green sprout on its head" width="240">
</p>

# Beansprout

Add a Markdown blog to the Spring Boot app you already run.

You write posts as Markdown files. Beansprout turns them into a blog inside your app: an index,
a page per post, tag pages, an archive, an Atom feed and a sitemap. Every page carries the tags
search engines and social sites read. Code is highlighted on the server, and the theme follows
the reader's light or dark mode. No controllers, no templates, no database.

## Why

Java developers who want a blog today have two choices. Run a separate platform like Ghost or
WordPress next to their app, or build posts, tags, feeds and sitemaps by hand. Beansprout is
the third choice: add a dependency to the app you already have.

## Guides

- [Start a new blog](docs/new-blog.md): a new app, your first post, pictures, drafts and scheduling.
- [Deploy it](docs/deploy.md): to Railway, or anywhere that runs Java or Docker.

## Tested with

Java 25 and Spring Boot 4.1.2. Pages render with [JTE](https://jte.gg).

## Add it to your app

Add the starter and the default theme, Longhand:

```xml
<dependency>
    <groupId>dev.danvega</groupId>
    <artifactId>beansprout-spring-boot-starter</artifactId>
    <version>0.1.0</version>
</dependency>
<dependency>
    <groupId>dev.danvega</groupId>
    <artifactId>beansprout-theme-longhand</artifactId>
    <version>0.1.0</version>
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
redeploy. Add `picture: /images/hello.png` for a picture on the card and the post, and
`updated: 2026-12-02` when you change a post later.
[Start a new blog](docs/new-blog.md) has every frontmatter option.

## Settings

All optional, in `application.yaml`:

```yaml
beansprout:
  title: My Blog                    # default: Blog
  description: Notes on Java.       # default: empty
  author: Your Name                 # shown on each post. Default: empty
  photo: /images/me.png             # shown above the blog's name. Default: none
  base-path: /blog                  # default: /blog
  page-size: 10                     # default: 10
  site-url: https://example.com     # for canonical links, previews, feed and sitemap. Default: the request
  time-zone: Europe/Amsterdam       # default: the server's zone
  posts:
    location: classpath:posts/      # or a folder on disk, like file:./posts/
    reload: false                   # true re-reads posts on every request, for development
  web:
    enabled: true                   # false turns the pages off. Posts and Pages stay as beans.
  code:
    highlight: true                 # false shows code fences as plain code
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
| `verification` | Checks the build runs on the repo itself, such as no leftover TODOs in the code. |

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
