# Spec: robots

Rank 9 in `reports/Blog platform features for Beansprout.md`: a robots.txt with a sitemap line and
a choice about AI crawlers.

## Why

Crawlers read `/robots.txt` at the site's root before anything else. Beansprout serves none, so
Google and Bing find the sitemap only if someone submits it by hand. An owner also has no simple way
to say "AI search may read my posts, AI training may not". OpenAI and Anthropic each run one crawler
for training and another for search, and blocking one does not block the other.

## Requirements

| ID | Requirement |
| --- | --- |
| ROBOTS-1 | When the app has no robots.txt of its own, the system shall serve `/robots.txt` at the site's root as `text/plain` in UTF-8. |
| ROBOTS-2 | The system shall start the file with a `User-agent: *` group that allows every path. |
| ROBOTS-3 | The system shall end the file with a `Sitemap:` line that holds the absolute address of `{base}/sitemap.xml`. |
| ROBOTS-4 | The system shall build that address from `beansprout.site-url`, or from the request's scheme, host and port when it is not set. |
| ROBOTS-5 | When `beansprout.robots.ai-training` is `block`, the system shall add one group that names GPTBot, ClaudeBot, Google-Extended, Applebot-Extended and CCBot. |
| ROBOTS-16 | When `beansprout.robots.ai-training` is `block`, the system shall disallow `{base}` in that group. |
| ROBOTS-6 | When `beansprout.robots.ai-training` is `block`, the system shall not name OAI-SearchBot, Claude-SearchBot or any other search crawler, so they follow the `*` group. |
| ROBOTS-7 | When `beansprout.robots.ai-training` is `allow`, the system shall name no crawler. |
| ROBOTS-8 | The system shall use `allow` when `beansprout.robots.ai-training` is not set. |
| ROBOTS-9 | If `beansprout.robots.ai-training` is any other value, then the app shall fail to start with a message that lists `allow` and `block`. |
| ROBOTS-10 | If the app has a `robots.txt` in one of its static locations, then the system shall not serve one. |
| ROBOTS-11 | If the app maps `/robots.txt` in a controller of its own, then the app shall start and its controller shall answer. |
| ROBOTS-12 | If the app has a servlet context path, then the system shall not serve robots.txt. |
| ROBOTS-13 | If the app has a servlet context path, then the system shall log once at startup that robots.txt is off because crawlers read it only at the root. |
| ROBOTS-14 | When `beansprout.robots.enabled` is `false`, the system shall not serve robots.txt. |
| ROBOTS-15 | When the static export runs and the app serves robots.txt, the system shall write `robots.txt` at the root of the output folder. |

## The file

With `ai-training: block` and `site-url: https://example.com`:

```text
User-agent: *
Allow: /

User-agent: GPTBot
User-agent: ClaudeBot
User-agent: Google-Extended
User-agent: Applebot-Extended
User-agent: CCBot
Disallow: /blog

Sitemap: https://example.com/blog/sitemap.xml
```

A crawler follows the most specific group that names it, so GPTBot skips the blog and may still
read the app's other pages. `Disallow: /blog` matches by prefix, so it also covers an app page
such as `/blogroll`.

## Settings

```yaml
beansprout:
  robots:
    enabled: true          # default
    ai-training: allow     # default; block keeps training crawlers out of the blog
```

## Out of scope

- Rules for the app's pages outside the blog. Beansprout is a guest in the app. An owner who wants
  one policy for the whole site writes their own `static/robots.txt`, and Beansprout steps aside.
- Other ways an app serves robots.txt, such as a filter, a proxy or a CDN rule. Set
  `beansprout.robots.enabled: false`.
- llms.txt. No provider confirms reading it, and Google says it ignores it.
- Crawlers that fetch a page because a person asked, such as ChatGPT-User and Claude-User. They
  act for the reader, not for training.
- A user-editable crawler list. The five names are the ones the providers document today.
