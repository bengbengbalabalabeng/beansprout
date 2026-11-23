# Spec: theme-slots

Rank 13 in `reports/Blog platform features for Beansprout.md`, and part of 0.4.0, "keep readers
around".

## Why

Comments, a newsletter form and analytics each need a few lines of HTML on every page. Today an
app can only add them by writing its own templates.

## Requirements

An app supplies a slot as a file. Each one holds the owner's own HTML, such as a `<script>` tag
for analytics or a signup form.

| File | Where it goes |
| --- | --- |
| `src/main/resources/beansprout/head.html` | At the end of every page's `<head>` |
| `src/main/resources/beansprout/after-post.html` | After the body of every post |
| `src/main/resources/beansprout/footer.html` | In every page's footer |

| ID | Requirement |
| --- | --- |
| SLOT-1 | When the app has `beansprout/head.html` on its classpath, every theme shall put its contents at the end of every page's `<head>`. |
| SLOT-2 | When the app has `beansprout/after-post.html` on its classpath, every theme shall put its contents after the body of every post. |
| SLOT-3 | When the app has `beansprout/footer.html` on its classpath, every theme shall put its contents in every page's footer. |
| SLOT-4 | The system shall pass each slot's contents to the theme as it is, without escaping it. |
| SLOT-5 | When a slot's file is missing, every theme shall render nothing in its place. |
| SLOT-6 | The system shall read each slot's file once, when the app starts. |
| SLOT-7 | If a slot's file is not UTF-8, then the system shall stop the app at startup with a message that names the file. |
| SLOT-8 | Every theme shall put the head and footer slots on the not-found page too. |
| SLOT-9 | When the static export runs, the system shall write the slots into every exported page, as the app serves them. |

## The view contract

`Site` gains `slots`, a new `Slots` record with `head`, `afterPost` and `footer`, each an empty
string when its file is missing. `Site` is on every page, so a theme reaches all three from its
layout. SLOT-1 to SLOT-3 touch the layout and post templates of all eight themes: Calling Card, Dateline, Linocut, Longhand, Marginalia, Onionskin, Plainsong
and Small Machines.

## Notes

- `view.Meta`'s Javadoc says `jsonLd` is the only value a theme writes without escaping, besides
  a post's body. The three slots join that list, so the Javadoc changes with SLOT-4.
- The HTML comes from the app's owner, in the app's own jar, like a template. That is why it is
  not escaped. Nothing a reader sends ever reaches a slot.
- A post's title or address is not filled into a slot. A comments script reads the page's own
  address, and the head already has the canonical link.

## Out of scope

- Slots per page type, such as a slot for tag pages only.
- Filling values into a slot, such as `{{title}}`.
- A "Reply by email" link. It can go in `after-post.html` as plain HTML.
- Slots read again without a restart. A new slot is a deploy, like a new post.
