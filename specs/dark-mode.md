# Spec: dark mode

Item 6 in `reports/Blog platform features for Beansprout.md`, asked for by Dan with highlighting:
"can we also support light and dark mode?"

## Why

Many readers run their system in dark mode, and a bright page at night is the first thing they
notice. Dark mode is the second most-reacted request in Quarkus Roq's issue tracker. Longhand
keeps every color in CSS custom properties, so dark mode is a second set of values, with no
JavaScript.

## Requirements

| ID | Requirement |
| --- | --- |
| DARK-1 | When the reader's system prefers a dark color scheme, the Longhand theme shall use its dark colors. |
| DARK-2 | The Longhand theme shall switch schemes with CSS alone, with no JavaScript. |
| DARK-3 | The Longhand theme shall declare `color-scheme: light dark`, so form controls and scrollbars match the page. |
| DARK-4 | In both schemes, the Longhand theme shall give body text, headings, secondary text and links a contrast of at least 4.5:1 against the page and card backgrounds. |
| DARK-5 | In both schemes, the Longhand theme shall give text on an accent background a contrast of at least 4.5:1. |
| DARK-6 | The Longhand theme shall take its code block colors from the highlighter's stylesheet, so code is light on light pages and dark on dark pages. |

## Out of scope

- A button to switch schemes by hand. It needs JavaScript and a place to remember the choice.
- Dark versions of the sample's pictures. Pictures are shown as they are.
