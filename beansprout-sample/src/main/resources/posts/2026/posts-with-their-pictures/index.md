---
title: Posts with their pictures
date: 2026-11-17
tags: [beansprout]
summary: A post can be a folder. Its Markdown and its pictures live side by side.
picture: cover.png
picture-alt: The Beansprout mascot
---
This post is a folder, not a file. The folder holds `index.md` and the pictures the post uses.
The folder's name is the post's address, so this post lives at `/blog/posts-with-their-pictures`.

```
posts/2026/posts-with-their-pictures/
  index.md
  cover.png
  sprout.png
```

Link a picture by its name, with no folder in front:

![The Beansprout mascot, a second time](sprout.png)

The picture at the top is `picture: cover.png` in the frontmatter. Delete the folder and the post
and its pictures go together. A plain `.md` file still works too, so mix the two as you like.
