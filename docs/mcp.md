# Write with an agent over MCP

Beansprout can serve your blog to an agent over MCP, the protocol Claude Code, Codex, Cursor and
others use to call tools. The agent can read your posts and start new drafts. It can never
publish. You do that, by setting `draft: false` in the file.

## Turn it on

Add one more dependency next to the starter. The BOM gives it its version:

```xml
<dependency>
    <groupId>dev.danvega</groupId>
    <artifactId>beansprout-mcp-spring-boot-starter</artifactId>
</dependency>
```

Start your app. The MCP server is at `http://localhost:8080/mcp`, over streamable HTTP. It uses
Spring AI 2.0.2 and needs Beansprout 0.3.0 or newer.

## The tools

| Tool | What it does |
| --- | --- |
| `list_posts` | Your published posts, newest first, with their slugs, dates, tags and addresses. |
| `list_tags` | Your tags, with how many posts each has. |
| `find_posts_by_tag` | The posts with one tag, by its name or its slug. |
| `read_post` | One post: its values and its Markdown as you wrote it. |
| `create_draft` | A new post file with `draft: true`. |

An agent sees what a reader sees. Drafts and posts dated in the future are not listed or read.
Unlisted posts are not listed, but `read_post` reads one by its slug.

`create_draft` is there only when your posts are a folder on disk, as they are while you write:

```yaml
beansprout:
  posts:
    location: file:src/main/resources/posts/
    reload: true
```

It names the file after the title, like `notes-from-the-road.md`, and never replaces a file that
is already there. A deployed app that reads its posts from the jar gets the read tools only.

## Connect an agent

With Claude Code:

```bash
claude mcp add --transport http beansprout http://localhost:8080/mcp
```

Then ask it things like "List my posts about Spring Boot" or "Start a draft about virtual
threads, in the style of my last three posts".

## Who may call it

With no token set, the server only answers requests from the same machine, which is what you
want while you write. A request that came through a proxy is refused too, because a proxy on the
same machine makes outside traffic look local.

To call it from somewhere else, set a token and send it as a Bearer header. Spring Boot reads
the environment variable `BEANSPROUT_MCP_TOKEN` as `beansprout.mcp.token`, so set that where the
app runs and keep the token out of your repository:

```bash
export BEANSPROUT_MCP_TOKEN=a-long-random-value
```

```bash
claude mcp add --transport http beansprout https://blog.example.com/mcp \
  --header "Authorization: Bearer $BEANSPROUT_MCP_TOKEN"
```

Don't write `token: ${BEANSPROUT_MCP_TOKEN}` in `application.yaml`. When the variable is not
set, Spring keeps that text as it is, and the token becomes the words in the braces.

## Change the defaults

Beansprout sets `spring.ai.mcp.server.protocol=STREAMABLE` and names the server `beansprout`.
Any `spring.ai.mcp.server.*` setting of your own wins.
