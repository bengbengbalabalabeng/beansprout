package dev.danvega.beansprout.mcp;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import dev.danvega.beansprout.content.MarkdownFile;
import dev.danvega.beansprout.content.Post;
import dev.danvega.beansprout.content.PostSource;
import dev.danvega.beansprout.content.Posts;
import dev.danvega.beansprout.content.Slugs;
import dev.danvega.beansprout.view.Links;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;

/**
 * What an agent may read: the blog as a reader sees it. Drafts and posts dated in the future are
 * never listed or read, the same as on the blog's pages.
 */
public final class BlogTools {

	private final Posts posts;

	private final PostSource source;

	private final Supplier<String> site;

	private final Links links;

	/**
	 * Creates the tools.
	 *
	 * @param site the site's scheme, host and port, like {@code https://example.com}, for each
	 *     post's absolute address
	 * @param links the blog's paths, context path included
	 */
	public BlogTools(Posts posts, PostSource source, Supplier<String> site, Links links) {
		this.posts = posts;
		this.source = source;
		this.site = site;
		this.links = links;
	}

	@McpTool(name = "list_posts", description = "List the blog's published posts, newest first, with each one's slug,"
			+ " title, date, tags, summary and address. Drafts and posts dated in the future are not listed.")
	public List<PostSummary> listPosts() {
		return posts.published().stream().map(this::summary).toList();
	}

	@McpTool(name = "list_tags", description = "List the tags on the blog's published posts, most used first, with"
			+ " how many posts have each one.")
	public List<TagCount> listTags() {
		return posts.tags().stream().map(tag -> new TagCount(tag.name(), tag.slug(), tag.count())).toList();
	}

	@McpTool(name = "find_posts_by_tag", description = "Find the published posts with a tag, newest first. Give the"
			+ " tag's name, like Spring Boot, or its slug, like spring-boot.")
	public List<PostSummary> findPostsByTag(
			@McpToolParam(description = "The tag's name or its slug", required = true) String tag) {
		return posts.tagged(Slugs.slugify(tag)).stream().map(this::summary).toList();
	}

	@McpTool(name = "read_post", description = "Read one published post by its slug: its title, date, updated date,"
			+ " tags, summary, picture and its Markdown body as written.")
	public PostText readPost(@McpToolParam(description = "The post's slug, as list_posts gives it", required = true) String slug) {
		Post post = posts.find(slug.strip())
			.orElseThrow(() -> new IllegalArgumentException("No published post has the slug '" + slug + "'."));
		String markdown = source.files()
			.stream()
			.filter(file -> file.path().equals(post.path()))
			.findFirst()
			.map(MarkdownFile::text)
			.map(BlogTools::body)
			.orElse("");
		String updated = post.modified().toLocalDate().isAfter(post.date().toLocalDate())
				? post.modified().toLocalDate().toString() : "";
		return new PostText(post.slug(), post.title(), post.date().toLocalDate().toString(), updated, post.tags(),
				post.summary(), post.image(), url(post), markdown);
	}

	private PostSummary summary(Post post) {
		return new PostSummary(post.slug(), post.title(), post.date().toLocalDate().toString(), post.tags(),
				post.summary(), url(post));
	}

	private String url(Post post) {
		return site.get() + links.post(post.slug());
	}

	/** The Markdown after the frontmatter, exactly as written. */
	static String body(String text) {
		String clean = text.startsWith("\uFEFF") ? text.substring(1) : text;
		String[] lines = clean.replace("\r\n", "\n").split("\n", -1);
		if (!lines[0].strip().equals("---")) {
			return clean;
		}
		for (int i = 1; i < lines.length; i++) {
			if (lines[i].strip().equals("---")) {
				return String.join("\n", Arrays.copyOfRange(lines, i + 1, lines.length));
			}
		}
		return clean;
	}

	/**
	 * One post in a list.
	 *
	 * @param date the day it went live, like {@code 2026-11-17}
	 * @param url its absolute address
	 */
	public record PostSummary(String slug, String title, String date, List<String> tags, String summary, String url) {
	}

	/** A tag and how many published posts have it. */
	public record TagCount(String name, String slug, int posts) {
	}

	/**
	 * One post to read.
	 *
	 * @param updated the day it last changed, or empty when that is its date
	 * @param picture its picture's address, or empty for none
	 * @param markdown its body as written, after the frontmatter
	 */
	public record PostText(String slug, String title, String date, String updated, List<String> tags, String summary,
			String picture, String url, String markdown) {
	}

}
