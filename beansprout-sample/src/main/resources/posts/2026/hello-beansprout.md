---
title: Hello, Beansprout
date: 2026-11-16
tags: [beansprout, spring boot]
summary: The first post, served from a folder of Markdown by a Spring Boot starter.
---
This blog has no controllers and no templates of its own. It has one dependency and a folder of
Markdown files.

## How it works

Beansprout reads every `.md` file under `src/main/resources/posts`. The frontmatter gives each
post a title, a date and tags. The rest of the file is the post.

## What you get

| Page | Address |
| --- | --- |
| Index | `/blog` |
| Post | `/blog/{slug}` |
| Tag | `/blog/tags/{tag}` |
| Archive | `/blog/archive` |

There is an Atom feed at `/blog/feed.xml` and a sitemap at `/blog/sitemap.xml`.
