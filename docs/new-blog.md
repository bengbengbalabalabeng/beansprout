# Start a new blog

This guide makes a new Spring Boot app that is nothing but a blog. If you already have an app,
skip to step 2.

You need JDK 25 or newer.

## 1. Create the app

Go to [start.spring.io](https://start.spring.io) and pick:

- Project: Maven
- Spring Boot: 4.1
- Java: 25
- Dependencies: Spring Web

Download it and unzip it.

## 2. Add Beansprout

Beansprout is not on Maven Central yet. Until it is, clone this repo and install it into your
local Maven repository:

```bash
git clone https://github.com/danvega/beansprout.git
cd beansprout && ./mvnw install -DskipTests
```

Then add two dependencies to your app's `pom.xml`. The starter is the blog. The theme is how it
looks.

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

## 3. Name your blog

In `src/main/resources/application.yaml`:

```yaml
server:
  port: ${PORT:8080}       # most hosts, Railway included, tell the app its port this way

beansprout:
  title: My Blog
  description: Notes on Java and Spring.
  author: Your Name
```

The README lists every setting.

## 4. Write a post

Create `src/main/resources/posts/hello.md`:

```markdown
---
title: Hello, world
date: 2026-11-16
tags: [java, spring boot]
---
My first post.
```

Any `.md` file under `posts/` is a post, in any subfolder. The file name becomes the address:
`hello.md` is served at `/blog/hello`.

## 5. Run it

```bash
./mvnw spring-boot:run
```

Open `http://localhost:8080/blog`.

## Writing posts

| You want | Add to the frontmatter |
| --- | --- |
| A different address | `slug: my-address` |
| A line under the title, and on the card | `summary: One sentence.` |
| A picture on the card and the post | `picture: /images/hello.png` and `picture-alt: What it shows` |
| A line under the picture | `caption: Taken in Ljubljana.` |
| To keep it hidden | `draft: true` |
| To publish it later | a date in the future, like `date: 2026-12-01T09:00` |

Put pictures in `src/main/resources/static/images/`. Spring Boot serves that folder, so
`static/images/hello.png` is `/images/hello.png`.

To put your photo above the blog's name, add `photo: /images/me.png` under `beansprout:`.

## See edits without a restart

Posts are read once at startup. While you write, read them straight from the folder on disk
and re-read them on every request:

```yaml
beansprout:
  posts:
    location: file:src/main/resources/posts/
    reload: true
```

Keep this for development only. In production, leave both settings out.

## Next

[Deploy it](deploy.md).
