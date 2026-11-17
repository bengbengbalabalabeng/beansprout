# Spec: mcp

Goal 7 in PRODUCT.md: agents can work with the blog over MCP. Dan asked on 17 November 2026,
chose the guard, the packaging and where drafts go, and approved this spec the same day.

## Why

An agent cannot see a Beansprout blog's posts or start a new one. A writer who works with
Claude Code or another agent copies posts in and out by hand.

## How it works

An app adds one more dependency, managed by the BOM:

```xml
<dependency>
    <groupId>dev.danvega</groupId>
    <artifactId>beansprout-mcp-spring-boot-starter</artifactId>
</dependency>
```

It brings Spring AI's MCP server (`spring-ai-starter-mcp-server-webmvc`) over streamable HTTP at
`/mcp`. It uses Spring AI 2.0.2, the newest GA release and the one that runs on Spring Boot 4.1,
as in Dan's `mcp-http-interfaces` demo. Spring AI 2.1 pairs with Spring Boot 4.2 and is a release
candidate today (2.1.0-RC2). Beansprout moves to 2.1 when it is GA and Beansprout moves to Boot
4.2, since a library on Maven Central should not make apps depend on a release candidate. Beansprout's
auto-configuration sees it and registers the tools. The main starter stays free of Spring AI.

| Tool | What it does |
| --- | --- |
| `list_posts` | Published posts, newest first: slug, title, date, tags, summary and address. |
| `list_tags` | Tags on published posts, with how many posts each has. |
| `find_posts_by_tag` | Published posts with one tag, by its name or its slug. |
| `read_post` | One published post: its frontmatter values and its Markdown body as written. |
| `create_draft` | A new post file with `draft: true`, in the posts folder on disk. |

The server never publishes. A person flips `draft` to `false`, as PRODUCT.md says.

## Requirements

| ID | Requirement |
| --- | --- |
| MCP-1 | If `beansprout-mcp-spring-boot-starter` is not on the classpath, then the system shall start with no MCP endpoint. |
| MCP-2 | The main starter shall not depend on Spring AI, directly or through another artifact. |
| MCP-3 | When `beansprout-mcp-spring-boot-starter` is on the classpath, the system shall offer the tools `list_posts`, `list_tags`, `find_posts_by_tag` and `read_post` at `/mcp`. |
| MCP-4 | `list_posts` shall return published posts newest first, each with its slug, title, date, tags, summary and absolute address. |
| MCP-5 | `list_tags` shall return the tags on published posts with each one's post count, most used first. |
| MCP-6 | `find_posts_by_tag` shall return the published posts that have a tag, found by the tag's name or its slug. |
| MCP-7 | `read_post` shall return a published post's title, date, updated date, tags, summary, picture and its Markdown body as written in the file. |
| MCP-8 | If `read_post` names a slug no published post has, then the tool shall answer that there is none, and show nothing of a draft or a future post. |
| MCP-9 | When `beansprout.posts.location` is a folder on disk, the system shall offer `create_draft`. |
| MCP-10 | If `beansprout.posts.location` is not a folder on disk, then the system shall not offer `create_draft`. |
| MCP-11 | When `create_draft` is called with a title and a Markdown body, the system shall write a new `.md` file in the posts folder with the title, today's date, the tags and summary given, and `draft: true`. |
| MCP-12 | `create_draft` shall write `draft: true` whatever the caller sends. |
| MCP-13 | `create_draft` shall name the file from the slug of the title. No value the caller sends shall name a path. |
| MCP-14 | If a post file with that slug already exists, then `create_draft` shall write nothing and say which file is in the way. |
| MCP-15 | When `beansprout.mcp.token` is not set, the system shall answer MCP requests that come from the same machine. |
| MCP-16 | If `beansprout.mcp.token` is not set and a request comes from another machine, or carries a `Forwarded` or `X-Forwarded-For` header, then the system shall answer 403. |
| MCP-17 | When `beansprout.mcp.token` is set, the system shall answer only requests that send it as `Authorization: Bearer <token>`, from any machine. |
| MCP-18 | If `beansprout.mcp.token` is set and a request does not send it, then the system shall answer 401. |
| MCP-19 | `beansprout-bom` shall manage `beansprout-mcp-spring-boot-starter`. |
| MCP-20 | The sample app shall answer an MCP client's `list_posts` call with its published posts. |

A forwarded request is refused without a token because a proxy on the same machine makes
public traffic look local.

## Public names

These are hard to change once people use them.

- The artifact `beansprout-mcp-spring-boot-starter`.
- The property `beansprout.mcp.token`.
- The tool names in the table above, and the endpoint `/mcp`.

## Out of scope

- Publishing, editing or deleting a post. A person does that, in the file.
- Reading drafts or future posts. An agent sees what a reader sees.
- Pictures and post folders in `create_draft`. It writes one `.md` file.
- The stdio transport, OAuth, and per-tool permissions.
- A Thymeleaf or FreeMarker angle: the tools return data, not pages.
