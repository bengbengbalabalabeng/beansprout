# Spec: post-author

Part of 0.4.0, "bring your blog with you". A name only: "multiple authors" stays a non-goal
(PRODUCT.md, Dan, 18 November 2026).

## Why

A post by a guest, or a blog moved from a site with several writers, shows `beansprout.author` on
every post. The Wrap website dropped its per-post `author` key for this reason.

## Requirements

```markdown
---
title: Notes from the road
date: 2026-11-10
author: Jane Doe
---
```

| ID | Requirement |
| --- | --- |
| AUTHOR-1 | When a post has `author`, the system shall show that name on the post's page in place of `beansprout.author`. |
| AUTHOR-2 | When a post has `author`, the system shall use that name for the post's JSON-LD `Person`. |
| AUTHOR-3 | When a post's `author` is not `beansprout.author`, the system shall give its JSON-LD `Person` no `url`. |
| AUTHOR-4 | When a post has `author`, the system shall give its feed entry an `<author>` with that name. |
| AUTHOR-5 | When `beansprout.author` is set, the system shall use it as the feed's `<author>`. |
| AUTHOR-6 | When `beansprout.author` is not set, the system shall use the blog's title as the feed's `<author>`, as it does today. |
| AUTHOR-7 | When a post has no `author`, the system shall use `beansprout.author`, as it does today. |
| AUTHOR-8 | If a post's `author` has indented lines under it, as jekyll-seo-tag writes it, then the system shall ignore `author` and keep the post. |

## A change to the feed

AUTHOR-5 changes today's feed. `Feeds.atom` writes the blog's title as the feed's `<author>`
(line 56), even when `beansprout.author` names the writer. Feed readers show this name, so the
writer's name is the better default. A blog with no `beansprout.author` keeps today's feed
(AUTHOR-6).

## How it fits the view contract

No new field. `view.Post.author` already carries the name the themes show. It now comes from the
post when the post has one. Every theme already renders it.

## Out of scope

- Author pages, an author list, bios, photos or links per author.
- More than one author on one post.
- Authors on cards and listings. Themes show the author on the post page only.
