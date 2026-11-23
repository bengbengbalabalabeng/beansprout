# Spec: table-of-contents

Part of 0.4.0, "keep readers around". Longhand, Plainsong and Small Machines already show one.
This brings the other five themes in line.

## Why

A long post in Calling Card, Dateline, Linocut, Marginalia or Onionskin gives the reader no map of
its sections, though `view.Post.headings` is already filled for every post.

## Requirements

| ID | Requirement |
| --- | --- |
| TOC-1 | When a post has headings, Dateline shall show an "On this page" list of them. |
| TOC-2 | When a post has headings, Linocut shall show an "On this page" list of them. |
| TOC-3 | When a post has headings, Marginalia shall show an "On this page" list of them. |
| TOC-4 | Each entry in the list shall link to its heading's id on the page. |
| TOC-5 | When a post has no headings, Calling Card, Dateline, Linocut, Marginalia and Onionskin shall show no list. |
| TOC-6 | The list shall keep the contrast every theme already holds: at least 4.5 to 1 for its text. |
| TOC-7 | When a post has headings, Onionskin shall show an "On this page" list of them. |
| TOC-8 | When a post has headings, Calling Card shall show an "On this page" list of them. |

## Notes

- `post.headings()` holds the `##` headings only. `MarkdownRenderer` gives only those an id. The
  list follows Longhand's (`post.jte`, lines 45 to 54).
- Each theme places and styles the list in its own way. Longhand puts it in the sidebar. Plainsong
  shows a "Contents" list only when a post has two or more headings.
- No view contract change, and no change to Longhand, Plainsong or Small Machines.
- Reading time already shows in all eight themes, so it is not part of this spec.

## Out of scope

- `###` headings in the list. They have no ids today.
- A `toc: false` frontmatter key.
- A list that follows the reader as they scroll. No JavaScript.
