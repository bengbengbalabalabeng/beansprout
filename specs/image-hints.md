# Spec: image-hints

Rank 8 in `reports/Blog platform features for Beansprout.md`, and part of 0.3.1, "bring your blog
with you".

## Why

A picture with no size makes the page jump as it loads, and every picture loading at once slows
the first screen. Both count against a page's Core Web Vitals.

## Requirements

### Loading

| ID | Requirement |
| --- | --- |
| IMG-1 | The system shall give every `<img>` it renders from a post's Markdown `loading="lazy"`. |
| IMG-2 | The system shall give every `<img>` it renders from a post's Markdown `decoding="async"`. |
| IMG-3 | When a post has no `picture`, the system shall leave `loading="lazy"` off the first `<img>` in its body. |
| IMG-4 | The system shall leave every `<img>` written as raw HTML in a post as it is. |

### Size

| ID | Requirement |
| --- | --- |
| IMG-5 | When a body image is a PNG, JPEG or GIF in the post's folder, the system shall give its `<img>` the file's `width` and `height`. |
| IMG-6 | When a body image is a WebP in the post's folder, the system shall give its `<img>` the `width` and `height` from the file's header. |
| IMG-7 | When the system cannot read a body image's size, the system shall give its `<img>` no `width` or `height`. |
| IMG-8 | When a post's `picture` is in its folder and its size can be read, the system shall pass the size to the theme. |
| IMG-9 | When the theme has a picture's size, every theme shall set the `width` and `height` of the picture's `<img>`. |
| IMG-10 | When an `<img>` has a `width` and `height`, every theme shall keep it in proportion. |
| IMG-11 | When an `<img>` has a `width` and `height`, every theme shall keep it no wider than its column. |

## How it works

- Sizes come from the file's header, never the whole picture: `javax.imageio` readers for PNG,
  JPEG and GIF, and the RIFF header for WebP (`VP8 `, `VP8L` and `VP8X`). No new dependency.
- `PostSource` can list files and say whether one exists, but not read one. It gains a way to open
  a file in a post's folder, with a default that gives nothing, so an app's own source still
  works.
- CSS keeps pictures fluid, so a `width` and `height` only reserve the space. Dateline, Linocut,
  Longhand and Marginalia set `height: auto` on every image. Small Machines sets it on body images
  only, so its post picture needs it for IMG-10 and IMG-11. Plainsong, Onionskin and Calling Card set it on their post
  pictures and body images already.

## The view contract

IMG-8 adds `pictureWidth` and `pictureHeight` to `view.Post`, as text, empty when unknown. IMG-9
touches the post template of all eight themes: Calling Card, Dateline, Linocut, Longhand, Marginalia, Onionskin, Plainsong
and Small Machines. Cards keep their pictures unsized.

## Out of scope

- AVIF and SVG sizes. AVIF needs a box parser, and an SVG scales anyway.
- Pictures from the app's `static/` folder, such as `/images/chart.png`. Core cannot read the
  app's static files, so they get IMG-1 and IMG-2 only.
- Pictures at an `http` or `https` address. Beansprout never fetches them.
- Resizing, `srcset` and new image formats. Image processing stays a non-goal.
