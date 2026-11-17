# Use your own templates

You don't need a Beansprout theme. Your blog can share your site's layout, header and styles.
There are two ways to do it.

| | Beansprout keeps | You write |
| --- | --- | --- |
| **1. Your templates** | The routes, the 404s, the feed and the sitemap | Five templates |
| **2. Your pages** | The feed and the sitemap | Controllers and templates |

Start with the first. Move to the second only when you need routes of your own.

Both examples use Thymeleaf. Any engine works, since Beansprout only needs HTML back.

## 1. Your templates, Beansprout's pages

Add the starter, and leave out `beansprout-theme-longhand`. You also need a web app and your
template engine. With Thymeleaf, that is three dependencies:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webmvc</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-thymeleaf</artifactId>
</dependency>
<dependency>
    <groupId>dev.danvega</groupId>
    <artifactId>beansprout-spring-boot-starter</artifactId>
    <version>0.1.0</version>
</dependency>
```

Then tell Beansprout how to render a page. Define one `ThemeRenderer` bean:

```java
import java.util.Locale;
import java.util.Map;

import dev.danvega.beansprout.web.ThemeRenderer;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.Context;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class BlogTemplates {

    @Bean
    ThemeRenderer blogTheme(ITemplateEngine thymeleaf) {
        return (template, page) -> thymeleaf.process("blog/" + template,
                new Context(Locale.ENGLISH, Map.of("page", page)));
    }

}
```

For every blog page, Beansprout calls it with a template name and a page record. The feed and
the sitemap don't use it.

| Template | Record | Served at |
| --- | --- | --- |
| `index` | `IndexPage` | `/blog`, `/blog/page/{n}` |
| `post` | `PostPage` | `/blog/{slug}` |
| `tag` | `TagPage` | `/blog/tags/{tag}`, `/blog/tags/{tag}/page/{n}` |
| `archive` | `ArchivePage` | `/blog/archive` |
| `not-found` | `NotFoundPage` | Anything missing, with status 404 |

So this example needs five files in `src/main/resources/templates/blog/`, one per row. All five
must exist: a missing `not-found.html` turns every 404 into an error. Here is `post.html`:

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org" lang="en">
<head>
  <title th:text="${page.meta.title}">Post</title>
  <meta name="description" th:content="${page.meta.description}">
  <link rel="canonical" th:href="${page.meta.canonical}">
  <meta property="og:type" th:content="${page.meta.type}">
  <meta property="og:title" th:content="${page.meta.title}">
  <meta property="og:description" th:content="${page.meta.description}">
  <meta property="og:url" th:content="${page.meta.canonical}">
  <meta property="og:site_name" th:content="${page.meta.siteName}">
  <meta property="og:image" th:unless="${page.meta.image.isEmpty()}" th:content="${page.meta.image}">
  <meta name="twitter:card" th:content="${page.meta.twitterCard}">
  <script type="application/ld+json" th:utext="${page.meta.jsonLd}"></script>
  <link rel="stylesheet" th:href="${page.site.root + '/beansprout/code.css'}">
</head>
<body>
  <a th:href="${page.site.home}" th:text="${page.site.name}">Blog</a>
  <article>
    <h1 th:text="${page.post.title}">Title</h1>
    <time th:datetime="${page.post.dateTime}" th:text="${page.post.date}">Date</time>
    <div th:utext="${page.post.body}"></div>
  </article>
</body>
</html>
```

And the list of posts in `index.html`:

```html
<article th:each="card : ${page.latest.cards}">
  <h2><a th:href="${card.url}" th:text="${card.title}">Title</a></h2>
  <time th:datetime="${card.dateTime}" th:text="${card.date}">Date</time>
  <p th:text="${card.excerpt}">Excerpt</p>
</article>
<a th:unless="${page.latest.newer.isEmpty()}" th:href="${page.latest.newer}">Newer posts</a>
<a th:unless="${page.latest.older.isEmpty()}" th:href="${page.latest.older}">Older posts</a>
```

Things to know:

- **Values are ready to show.** Each is a string, with dates and links already formatted, a list,
  another record, or now and then a flag such as `TagCount.current`. A missing string is an empty
  string, never null. In Thymeleaf an empty string counts as true, so test with
  `isEmpty()`, as the "Older posts" link does.
- **Escape everything except two values.** `post.body` is HTML that Beansprout rendered from
  Markdown, and `meta.jsonLd` is JSON. Use `th:utext` for those two, and `th:text` for the rest.
- **`page.meta` holds the SEO tags**: title, description, canonical link, Open Graph values and
  JSON-LD. The head above uses the main ones. Longhand's
  [layout](../beansprout-theme-longhand/src/main/jte/layout.jte) shows the rest, such as
  `og:image:alt` and the article dates.
- **Set `beansprout.site-url` in production.** Without it the canonical link comes from the
  request, which can say `http` behind a proxy that handles HTTPS.
- **`/beansprout/code.css` colors code blocks.** Code is highlighted on the server, so link the
  stylesheet, or write your own for the Prism class names. `page.site.root` is the app's context
  path, empty at the root, so the link works under any context path.
- **Your renderer wins over a theme.** If a theme jar is on the classpath too, Beansprout still
  uses your bean.

The [Longhand templates](../beansprout-theme-longhand/src/main/jte) show every value in use,
and each record's fields are in
[the view package](../beansprout-core/src/main/java/dev/danvega/beansprout/view).

### Let your coding agent build it

An agent can match your site faster than you can copy templates by hand. Open your app in
Claude Code, or any coding agent, and paste this:

```text
Add a blog to this Spring Boot app with Beansprout, in this site's own look.

1. Add the dependency dev.danvega:beansprout-spring-boot-starter:0.1.0.
   Do not add any beansprout-theme jar.
2. Read option 1 of https://github.com/danvega/beansprout/blob/main/docs/own-templates.md
   and follow it.
3. Find this app's template engine, its main layout and its stylesheets. Reuse them, so the
   blog has the same header, footer, fonts and colors as the rest of the site.
4. Define one ThemeRenderer bean. It renders "blog/" + the template name with this app's
   engine, and passes the page record to the template as "page".
5. Write the five templates: index, post, tag, archive and not-found. Use only the fields of
   the records in dev.danvega.beansprout.view: IndexPage, PostPage, TagPage, ArchivePage and
   NotFoundPage.
6. In every page's head, output the page.meta values: title, description, canonical link,
   the Open Graph tags, twitter:card, and the JSON-LD in a script tag.
7. Escape every value except post.body and meta.jsonLd. A missing value is an empty string,
   never null, so test for empty strings.
8. Link page.site.root + "/beansprout/code.css". Show the newer and older links when they
   are not empty.
9. Add src/main/resources/posts/hello.md with title, date and tags in its frontmatter and a
   fenced code block in its body.
10. Start the app and check each page with curl. /blog, the post, its tag page, /blog/archive
    and /blog/feed.xml answer 200. /blog/no-such-post answers 404 with the not-found page.
    Fix anything that fails before you stop.
```

Then read what it built. The pages are yours now, so change anything you like.

## 2. Your pages too

When you want your own routes, write the controllers yourself. Inject `Pages`, which builds the
same records, and leave out both the theme and the `ThemeRenderer` bean. With no renderer,
Beansprout registers no pages of its own. Keep either one, and Beansprout maps the same addresses
as your controller, so the app fails to start with an "Ambiguous mapping" error.

```java
import dev.danvega.beansprout.view.Pages;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

@Controller
class WritingController {

    private final Pages pages;

    WritingController(Pages pages, @Value("${beansprout.site-url:}") String siteUrl) {
        this.pages = pages.forSite(siteUrl);
    }

    @GetMapping("/writing")
    String index(Model model) {
        model.addAttribute("page", pages.index(1).orElseThrow());
        return "blog/index";
    }

    @GetMapping("/writing/{slug}")
    String post(@PathVariable String slug, Model model) {
        model.addAttribute("page",
                pages.post(slug).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)));
        return "blog/post";
    }

}
```

```yaml
beansprout:
  base-path: /writing
  site-url: https://example.com
```

Things to know:

- **Set `base-path` to your route.** Beansprout builds every link from it, so cards, tags and
  the feed point to `/writing/...`. It needs at least one segment: the blog cannot sit at `/`.
- **Call `pages.forSite(...)`** with your site's address. The `:` in `${beansprout.site-url:}`
  makes the setting optional. Without it, canonical links are paths such as `/writing/hello`,
with no host.
- **Those links include pages this example does not map**: `/writing/page/{n}` for older posts,
  `/writing/tags/{tag}` and `/writing/archive`. Add a mapping for each one you link to, with
  `pages.index(n)`, `pages.tag(slug, n)` and `pages.archive()`. A tag's address uses its slug,
  such as `spring-boot` for "spring boot".
- **The feed and the sitemap stay on**, at `/writing/feed.xml` and `/writing/sitemap.xml`. The
  sitemap lists every page Beansprout knows: the index and its numbered pages, each post, each tag
  and its numbered pages, and the archive.
  Serve those addresses too, or search engines find pages that answer 404.
- **Want no Beansprout routes at all?** Set `beansprout.web.enabled: false`. That turns off the
  feed and the sitemap as well, and `Pages` and `Posts` stay as beans you can call.
