# Spec: highlighting

Item 5 in `reports/Blog platform features for Beansprout.md`: syntax highlighting for code fences,
done once on the server, in a form any theme can color.

## Why

Most posts on a developer blog are partly code. Today a fence like ```` ```java ```` becomes plain
`<pre><code class="language-java">` with no colors, and a theme that wants them has to add a
JavaScript highlighter. That makes highlighting a theme feature, costs every reader a script, and
gives every theme a different look for the same code.

## How it works

Core highlights each fence when the posts are read. It wraps tokens in `<span>` elements with
[Prism](https://prismjs.com)'s class names, such as `token keyword` and `token string`. Because
those names are a de facto standard, every Prism CSS theme colors Beansprout code with no changes.
Beansprout also serves one stylesheet of its own, apart from any theme, that themes can link.

The highlighter sits behind an interface in core, so an app can swap in another one.

## Requirements

| ID | Requirement |
| --- | --- |
| HL-1 | When a code fence names a language the highlighter knows, the system shall wrap each token in a `span` whose classes are `token` and a Prism token type. |
| HL-2 | The system shall keep the `language-` class on the `code` element of every fenced block. |
| HL-3 | If a fence names no language, or one the highlighter does not know, then the system shall show the code escaped and unchanged, as it does today. |
| HL-4 | The system shall HTML-escape the text of every token. |
| HL-5 | The system shall leave the code itself unchanged, so removing the spans gives back the text as written. |
| HL-6 | The system shall highlight when it reads the posts, not on each request. |
| HL-7 | The built-in highlighter shall know Java, Kotlin, XML, HTML, YAML, properties files, JSON, SQL, shell, JavaScript and TypeScript. |
| HL-8 | The system shall accept the common short names for those languages, such as `yml`, `sh`, `bash`, `js` and `ts`. |
| HL-9 | When an app defines its own `CodeHighlighter` bean, the system shall use it instead of the built-in one. |
| HL-10 | When `beansprout.code.highlight` is false, the system shall leave every fence unhighlighted. |
| HL-11 | The system shall serve a stylesheet for the token classes at `/beansprout/code.css`, apart from any theme. |
| HL-12 | The stylesheet shall take its colors from CSS custom properties, so a theme can change them without replacing the file. |
| HL-13 | The stylesheet shall switch to dark colors when the reader's system prefers a dark scheme. |
| HL-14 | The Longhand theme shall link the stylesheet. |
| HL-15 | The stylesheet shall use the colors of Shiki's GitHub Light Default theme for light pages and GitHub Dark Default for dark pages. |
| HL-16 | The stylesheet shall color every token type with a contrast of at least 4.5:1 against its code background, in both schemes. |
| HL-17 | The stylesheet shall give a code block its background and text color with zero specificity, so a theme's own rules win. |
| HL-18 | If highlighting a fence fails, or its code is longer than 100,000 characters (not counting the final line break), then the system shall show that fence as plain code and keep the rest of the post. |
| HL-19 | If the built-in highlighter takes longer than 250 milliseconds on one fence, then it shall stop and show that fence as plain code. |
| HL-20 | If highlighting a post's fences has taken more than one second, then the system shall show the post's remaining fences as plain code. |

## Out of scope

- Highlighting inline code, such as `` `var x` `` in a sentence.
- Line numbers, highlighted lines (`java {3,5-7}`), file names on fences, and copy buttons. Each
  can come later on top of the same spans. A copy button needs JavaScript.
- Colors in the Atom feed. The spans stay in the feed's HTML, and feed readers ignore the classes.
- Every language Prism knows. The list in HL-7 covers what a Spring blog shows. An app that
  needs more plugs in its own highlighter (HL-9).

## Engine (Dan, 16 November 2026)

A port of Prism's grammars to Java, written in core, with no new dependency. Prism's grammars are
named regular expressions per language and are MIT licensed. The port keeps Prism's copyright
notice. Prism's class names make the output work with every Prism CSS theme.

The colors come from Shiki's GitHub Light Default and GitHub Dark Default themes, which Dan uses
in his Nuxt app. Token boundaries will not match Shiki exactly, since Prism's rules are simpler
than TextMate grammars.

Considered and set aside: lumis4j (young, one more dependency), a TextMate engine on the JVM
(Eclipse TM4E is not on Maven Central, and a Kotlin port pulls in the Kotlin library), Shiki
itself through GraalJS or Node (too heavy for a starter, or build time only).

## Public names to approve

- The property `beansprout.code.highlight` (default `true`).
- The stylesheet address `/beansprout/code.css`.
- The interface `CodeHighlighter` and its one method, which takes a language and code and returns
  highlighted HTML, or nothing for a language it does not know.
