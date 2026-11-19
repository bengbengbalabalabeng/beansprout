# Spec: themes

The rest of goal 8 in PRODUCT.md: more than one theme, and a way to choose. Dan decided on
16 November 2026 that every Pressworks blog template ships here for free, and approved this spec
on 17 November 2026.

## Why

Beansprout has one theme, Longhand, and no way to pick another. Pressworks has four more blog
templates (Dateline, Linocut, Marginalia and Small Machines) that people can only use by
porting them by hand.

## How it works

Each theme is its own module under `themes/` and its own artifact, `beansprout-theme-<name>`.
An app adds the one it wants. Longhand moves to `themes/beansprout-theme-longhand` and keeps
its artifact name, so 0.1.0 users change nothing.

A theme jar names itself to Beansprout through a small descriptor in core: its name, its
engine and where its compiled templates live. Beansprout finds every theme on the classpath
and picks one.

Each port maps the template's Pressworks model onto Beansprout's view records, the way
Longhand was done. Pages Beansprout has no data for (search, subscribe, series, about, author)
are left out, and so are the links to them.

| Theme | Look |
| --- | --- |
| `longhand` | One writer, a long post with a picture. Light and dark. |
| `dateline` | An engineering blog dressed as a status board. Dark. |
| `linocut` | Bold and playful, black ink on warm paper with one red. |
| `marginalia` | A link blog kept like a notebook. Short entries. |
| `small-machines` | A lot of personality. The site is dressed as an old desktop. |

A new module, `beansprout-bom`, lists every published Beansprout artifact at one version. An
app imports it once, then adds the starter and a theme with no version of their own. Every
module keeps one shared version until 1.0, because a theme is compiled against the view
records and must match the core it runs with.

## Requirements

| ID | Requirement |
| --- | --- |
| THEME-1 | When exactly one theme is on the classpath, the system shall render the pages with it. |
| THEME-2 | When `beansprout.theme` names a theme on the classpath, the system shall render the pages with that theme. |
| THEME-3 | If more than one theme is on the classpath and `beansprout.theme` is not set, then the system shall fail to start with a message that lists the themes it found. |
| THEME-4 | If `beansprout.theme` names a theme that is not on the classpath, then the system shall fail to start with a message that names it and lists the themes it found. |
| THEME-5 | The system shall serve each theme's stylesheet at `/beansprout/<theme>/`, apart from every other theme's. |
| THEME-6 | The system shall provide the themes `longhand`, `dateline`, `linocut`, `marginalia` and `small-machines`. |
| THEME-7 | Each theme shall render the index, post, tag, archive and not-found pages from the same posts. |
| THEME-8 | Each theme shall link only to addresses the app answers with 200. |
| THEME-9 | Each theme shall put the page's title, description, canonical link, Open Graph tags and JSON-LD in its head. |
| THEME-10 | Each theme shall show a post's title, date and tags. |
| THEME-11 | When a post has a picture, each theme shall show it with its alt text. |
| THEME-12 | When a post has a caption, each theme shall show it with the picture. |
| THEME-13 | When a post has an `updated` date, each theme shall show it. |
| THEME-14 | Each theme shall link `/beansprout/code.css`. |
| THEME-15 | In each color scheme a theme has, it shall give body text, headings, secondary text and links a contrast of at least 4.5:1 against its backgrounds. |
| THEME-16 | Each theme shall HTML-escape every value it shows, except a post's rendered body and the JSON-LD. |
| THEME-17 | When an app defines its own `ThemeRenderer` bean, the system shall start whatever theme jars are on the classpath, without checking `beansprout.theme`. |
| THEME-18 | The system shall publish a `beansprout-bom` that manages the version of every published Beansprout artifact. |
| THEME-19 | When an app imports `beansprout-bom`, the system shall let it add the starter and any theme with no version. |
| THEME-20 | The system shall provide the themes `plainsong`, `onionskin` and `calling-card`. |
| THEME-21 | The plainsong index shall list each post as its title, date and summary, with no picture. |
| THEME-22 | The onionskin index shall show each post as a memo with Date, Subject, Filed and Length lines. |
| THEME-23 | On its first page, the calling-card index shall show every tag with its post count. |

## Out of scope

- Fork Join, Night Desk and Octavo. They are personal sites with a writing section, not blogs.
- Search, subscribe, series, about and author pages, in any theme. Beansprout has no data for
  them yet. Each can come later, for every theme at once.
- Dark versions of the ported themes designed light only. Longhand switches schemes
  (`specs/dark-mode.md`), and so do Plainsong, Onionskin and Calling Card, which were designed
  light and dark from the start.
- Replacing one page of a theme from the app. That is its own open question in PRODUCT.md.
- Thymeleaf and FreeMarker versions. JTE only for now.

## Public names

Approved by Dan on 17 November 2026.


- The property `beansprout.theme`, and the theme names in the table above.
- The artifacts `beansprout-theme-dateline`, `-linocut`, `-marginalia` and `-small-machines`.
- The artifact `beansprout-bom`.
- Added 18 November 2026, approved by Dan in chat: the theme names `plainsong`, `onionskin` and
  `calling-card`, and the artifacts `beansprout-theme-plainsong`, `-onionskin` and
  `-calling-card`. These three are Beansprout's own designs, not Pressworks ports.
- The stylesheet addresses `/beansprout/<theme>/<theme>.css`.
- No theme is picked for you when there are two. Starting with two and no `beansprout.theme`
  fails, rather than quietly choosing one.
