# Spec: related-posts

From the second tier of `reports/Blog platform features for Beansprout.md`, and part of 0.3.1,
"keep readers around".

## Why

A reader who finishes a post sees only the newer and older links, which rarely match what they
just read. Posts that share its tags are a better next read.

## Requirements

| ID | Requirement |
| --- | --- |
| RELATED-1 | The system shall give each post page up to `beansprout.related-posts` other published posts that share at least one tag with it. |
| RELATED-2 | The system shall rank related posts by how many tags they share with the post, most first. |
| RELATED-3 | When two related posts share the same number of tags, the system shall put the newer one first. |
| RELATED-4 | The system shall never list a post as related to itself. |
| RELATED-5 | The system shall leave drafts and posts dated in the future out of related posts. |
| RELATED-6 | When a post has no tags, the system shall give it no related posts. |
| RELATED-7 | The system shall use 3 when `beansprout.related-posts` is not set. |
| RELATED-8 | When `beansprout.related-posts` is 0, the system shall give no post related posts. |
| RELATED-9 | If `beansprout.related-posts` is below 0, then the system shall stop the app at startup with a message that names the setting. |
| RELATED-10 | When a post has related posts, every theme shall show them after the post, each as a link with its title. |
| RELATED-11 | When a post has no related posts, every theme shall show nothing in their place. |

Tags match by slug, as tag pages do, so `Spring Boot` and `spring boot` are one tag.

## The view contract

`view.Post` gains `related`: a `List<PostLink>`, the same record as `newer` and `older`. It is
built in `Pages.post`, where `newer` and `older` are built today. RELATED-10 touches the post
template of all eight themes: Calling Card, Dateline, Linocut, Longhand, Marginalia, Onionskin, Plainsong
and Small Machines. Each
theme picks its own heading and look, such as "Read next".

## Out of scope

- Related posts by words in the text. Tags are the writer's own signal, and need no index.
- A `related:` frontmatter key to pick them by hand.
- Related posts in the feed or the MCP tools.
