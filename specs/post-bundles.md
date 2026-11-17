# Spec: post bundles

A post as a folder: its Markdown and its images in one place. Dan asked and approved on
17 November 2026.

## Why

Today a post's images live in the app's `static/` folder, far from the post. With hundreds of
posts nobody can tell which images a post uses, and deleting a post leaves its images behind.

## How it looks

```
posts/
  hello.md                      a post, as today: /blog/hello (the two kinds mix freely)
  welcome-to-wrap/              a bundle: /blog/welcome-to-wrap
    index.md
    editor.jpg                  /blog/welcome-to-wrap/editor.jpg
    shots/recorder.png          /blog/welcome-to-wrap/shots/recorder.png
```

```markdown
---
title: Welcome to Wrap
date: 2026-11-17
picture: editor.jpg
picture-alt: The editor and its timeline
---
![The recorder before a take](shots/recorder.png)
```

## Requirements

| ID | Requirement |
| --- | --- |
| BUNDLE-1 | When a folder under the posts location holds a file named `index.md`, the system shall read that file as one post, called a bundle. |
| BUNDLE-2 | When a bundle has no `slug` frontmatter, the system shall use its folder's name as the slug. |
| BUNDLE-3 | When a bundle has `slug` frontmatter, the system shall use that slug, as for any other post. |
| BUNDLE-4 | The system shall not read any other Markdown file inside a bundle's folder, or its subfolders, as a post. |
| BUNDLE-5 | The system shall keep reading a Markdown file outside any bundle as a post named after its file. |
| BUNDLE-6 | The system shall serve each image in a bundle's folder, or its subfolders, at the post's address followed by the image's path in the folder. |
| BUNDLE-7 | The system shall serve only files ending in `.png`, `.jpg`, `.jpeg`, `.gif`, `.webp` or `.avif` from a bundle. |
| BUNDLE-8 | If a request names a path that leaves the bundle's folder, then the system shall answer 404. |
| BUNDLE-9 | If a request names a file in a bundle that is not one of those images, then the system shall answer 404. |
| BUNDLE-10 | If a bundle's post is a draft or dated in the future, then the system shall answer 404 for its images. |
| BUNDLE-11 | The system shall send each bundle image with the content type of its extension. |
| BUNDLE-12 | The system shall send each bundle image with a `Last-Modified` header. |
| BUNDLE-13 | When a request for a bundle image carries an `If-Modified-Since` date no older than the file, the system shall answer 304 with no body. |
| BUNDLE-14 | When posts are reloaded, the system shall serve a bundle's images from its folder as it is at the time of the request. |
| BUNDLE-15 | When a bundle's body links an image by a relative path, the system shall write the link as a path from the app's root, with the context path in front. |
| BUNDLE-16 | When a bundle's body links an image by a path that starts with `/` or by a full web address, the system shall leave the link as written. |
| BUNDLE-17 | When a bundle's `picture` is a relative path to a file in its folder, the system shall use that file as the post's picture. |
| BUNDLE-18 | If a bundle's relative `picture` names a file that is not in its folder, then the system shall skip the post and log a warning that names the file. |
| BUNDLE-19 | When the feed carries a bundle's body, the system shall write its relative image links as absolute addresses. |
| BUNDLE-20 | The sample app shall have one bundle post whose page, picture and body image are served end to end. |

A bundle picture is a picture like any other once it is found. RICH-1 to RICH-5 and SEO-5 and
SEO-13 hold for it, so the share tags and the JSON-LD get its absolute address. RICH-7 still
skips a post whose picture is not a path, a full address or, in a bundle, a file in its folder.

A bundle's slug is checked like any other: an empty slug, a reserved one (`page`, `tags`,
`archive`) or a slug another post already has skips the post with a warning, as POSTS says.

## Public names

These are hard to change once people use them.

- `index.md` marks a bundle. Hugo and Astro use the same name.
- Images are served under the post's own address: `/blog/{slug}/{path}`.
- A relative `picture`, such as `picture: cover.jpg`, is allowed only in a bundle.

## Out of scope

- Files other than images in a bundle: video, PDFs, downloads, SVG. SVG can carry script, so it
  stays in `static/`. Each can come later with its own rules.
- Resizing images or making WebP and AVIF copies. The writer commits the images they want served.
- Fingerprinted image addresses with a year-long cache. `Last-Modified` and 304 come first.
- Moving existing posts into bundles. A plain `.md` post keeps working as it is.
- A bundle inside a bundle. The first `index.md` up the tree owns the folder.
- Static export of bundles. The export release copies them when it lands.

## Decided

- No `Cache-Control` max-age for now. `Last-Modified` and 304 keep repeat visits cheap with no
  new setting.
- An app's own route under a post, such as Wrap's `/blog/{slug}/og.png`, wins over a bundle file
  with the same name, because Spring prefers the more exact route. The guide says so.
