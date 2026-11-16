# Spec: rich pages

Goal 8 in PRODUCT.md: Longhand should look like the screens designed for it in Pressworks.

## Why

The first version of the theme dropped everything posts had no data for: the writer's photo,
pictures on cards and posts, the lede under a title, the author's name, and the highlighted
menu item. The pages work, but they look plain next to the Longhand design.

## Requirements

| ID | Requirement |
| --- | --- |
| RICH-1 | When a post has `picture` frontmatter, the system shall show that picture on the post's card. |
| RICH-2 | When a post has `picture` frontmatter, the system shall show that picture above the post's body. |
| RICH-3 | The system shall use the post's `picture-alt` as the picture's alt text, or an empty alt when it has none. |
| RICH-4 | When a post has `caption` frontmatter and a picture, the system shall show the caption under the picture on the post page. |
| RICH-5 | When a `picture` starts with `/`, the system shall put the app's context path in front of it. |
| RICH-6 | When a `picture` starts with `http://` or `https://`, the system shall use it as written. |
| RICH-7 | If a `picture` is anything else, then the system shall skip the post and log a warning that names it. |
| RICH-8 | When a post has `summary` frontmatter, the system shall show it under the title on the post page. |
| RICH-9 | When `beansprout.author` is set, the system shall show the author's name on each post page. |
| RICH-10 | When `beansprout.photo` is set, the system shall show that picture above the blog's name on every page. |
| RICH-11 | The system shall mark the index pages' menu item as current on index pages. |
| RICH-12 | The system shall mark the archive's menu item as current on the archive. |

## Frontmatter

```markdown
---
title: Testcontainers without the waiting
date: 2026-03-14
picture: /images/test-run.svg        # a path in your app, or a full https:// address
picture-alt: Forty containers starting # optional
caption: What four minutes looks like # optional
summary: Shown under the title.       # optional
---
```

`beansprout.photo` follows the same rules as `picture`, and a bad value fails at startup. A path
whose second character is `/` or `\` is refused, because a browser would load it from another
site. So is a web address with no host.

## Out of scope

- Search, subscribe, series, an about page and guest authors. Longhand has screens for them, and
  each needs its own feature first.
- Resizing or checking pictures. Beansprout links to them as written.
- Pictures in the Atom feed.
- An `image:` key. It stays unknown and ignored, because other blog tools use it for nested
  data. The names follow Longhand's model: `picture`, `picture-alt`, `caption`.
