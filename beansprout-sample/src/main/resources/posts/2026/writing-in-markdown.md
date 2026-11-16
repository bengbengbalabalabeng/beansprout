---
title: Writing posts in Markdown
date: 2026-11-10T09:00
tags: [beansprout, markdown]
picture: /images/build-diff.svg
picture-alt: A diff of a Markdown file
summary: Frontmatter on top, Markdown below. Tables, code and links all work.
---
A post is a Markdown file with a little frontmatter on top:

```markdown
---
title: Writing posts in Markdown
date: 2026-11-10T09:00
tags: [beansprout, markdown]
---
The post starts here.
```

Tables, code blocks, *emphasis* and [links](https://commonmark.org) all work.

Code fences are highlighted on the server, with no JavaScript:

```java
@RestController
class HelloController {

    @GetMapping("/hello")
    String hello(@RequestParam(defaultValue = "bean") String name) {
        return "Hello, " + name; // a sprout says hi
    }

}
```

```yaml
beansprout:
  title: Beansprout Notes   # the blog's name
  page-size: 10
```
