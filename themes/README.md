# Themes

Each folder here is one Beansprout theme, published as its own artifact. An app adds one theme
and gets its look.

These themes come from [Pressworks](https://pressworks.dev), where each one started as a blog
template. Pressworks gave them to Beansprout for free, under the same Apache 2.0 license as the
rest of the project. Thank you, Pressworks.

| Theme | Folder | Look |
| --- | --- | --- |
| Longhand | [beansprout-theme-longhand](beansprout-theme-longhand) | One writer, a long post with a picture. Light and dark. |
| Dateline | [beansprout-theme-dateline](beansprout-theme-dateline) | An engineering blog dressed as a status board. Dark. |
| Linocut | [beansprout-theme-linocut](beansprout-theme-linocut) | Bold and playful, black ink on warm paper with one red. |
| Marginalia | [beansprout-theme-marginalia](beansprout-theme-marginalia) | A link blog kept like a notebook. Short entries. |
| Small Machines | [beansprout-theme-small-machines](beansprout-theme-small-machines) | A lot of personality. The site is dressed as an old desktop. |

Three more are Beansprout's own, drawn from what readers like about well-loved personal blogs:

| Theme | Folder | Look |
| --- | --- | --- |
| Plainsong | [beansprout-theme-plainsong](beansprout-theme-plainsong) | A plain list of posts in black and white, no pictures. Light and dark. |
| Onionskin | [beansprout-theme-onionskin](beansprout-theme-onionskin) | Every post typed like a memo, in monospace. Light and dark. |
| Calling Card | [beansprout-theme-calling-card](beansprout-theme-calling-card) | You first: your name in poster-sized type, your photo, your topics. Light and dark. |
| Kiln TUI | [beansprout-theme-kilntui](beansprout-theme-kilntui) | A developer blog dressed as a terminal, amber on carbon black. Dark. |

## What a theme holds

- A class that implements `dev.danvega.beansprout.theme.Theme`, listed in
  `src/main/resources/META-INF/services/dev.danvega.beansprout.theme.Theme`. It gives the
  theme's name, which `beansprout.theme` uses to pick it.
- JTE templates in `src/main/jte`, one for each page: `index`, `post`, `tag`, `archive` and
  `not-found`. They are compiled when the theme is built.
- Its stylesheet in `src/main/resources/META-INF/resources/beansprout/<name>/`, served at
  `/beansprout/<name>/<name>.css`.

Every theme is held to the same rules in `specs/themes.md`, and `ThemesTests` in the sample app
checks each one against them.
