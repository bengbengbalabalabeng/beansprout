package dev.danvega.beansprout.feed;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import dev.danvega.beansprout.content.Bundles;
import dev.danvega.beansprout.content.Post;
import dev.danvega.beansprout.content.Posts;
import dev.danvega.beansprout.view.Links;
import dev.danvega.beansprout.view.PageUrl;
import dev.danvega.beansprout.view.Pages;

/**
 * Writes the Atom feed and the sitemap. Both need absolute addresses, so each takes the site's
 * address: {@code https://example.com}, with or without a trailing slash.
 */
public final class Feeds {

	/** How many posts the feed carries. */
	public static final int FEED_SIZE = 20;

	private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ssXXX");

	private final Posts posts;

	private final Links links;

	private final Pages pages;

	public Feeds(Posts posts, Links links, Pages pages) {
		this.posts = posts;
		this.links = links;
		this.pages = pages;
	}

	/** An Atom 1.0 feed of the newest published posts. */
	public String atom(String siteUrl) {
		String site = trim(siteUrl);
		return atom(site, pages.settings().title(), links.home(), links.feed(), posts.published());
	}

	/**
	 * An Atom 1.0 feed of the newest published posts with one tag, titled like {@code My Blog: Java},
	 * or empty when no published post has it.
	 *
	 * @param tagSlug the tag as it appears in its page's address, like {@code spring-boot}
	 */
	public Optional<String> tagAtom(String tagSlug, String siteUrl) {
		String site = trim(siteUrl);
		return posts.tag(tagSlug)
			.map(tag -> atom(site, pages.settings().title() + ": " + tag.name(), links.tag(tagSlug),
					links.tagFeed(tagSlug), posts.tagged(tagSlug)));
	}

	/**
	 * One feed. Every feed has the same entries for the same posts.
	 *
	 * @param home the page the feed stands for, which is also its id
	 * @param self the feed's own address
	 * @param newestFirst the published posts it may carry, newest first
	 */
	private String atom(String site, String title, String home, String self, List<Post> newestFirst) {
		List<Post> recent = newestFirst.stream().limit(FEED_SIZE).toList();
		ZonedDateTime updated = recent.stream().map(Post::modified).max(java.util.Comparator.naturalOrder())
			.orElseGet(() -> posts.clock().instant().atZone(ZoneOffset.UTC));

		StringBuilder xml = new StringBuilder();
		xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
		xml.append("<feed xmlns=\"http://www.w3.org/2005/Atom\">\n");
		element(xml, "  ", "title", title);
		if (!pages.settings().description().isEmpty()) {
			element(xml, "  ", "subtitle", pages.settings().description());
		}
		xml.append("  <link href=\"").append(escape(site + home)).append("\"/>\n");
		xml.append("  <link rel=\"self\" href=\"").append(escape(site + self)).append("\"/>\n");
		element(xml, "  ", "id", site + home);
		element(xml, "  ", "updated", TIMESTAMP.format(updated));
		String author = pages.settings().author().isEmpty() ? pages.settings().title() : pages.settings().author();
		xml.append("  <author><name>").append(escape(author)).append("</name></author>\n");
		for (Post post : recent) {
			String url = site + links.post(post.slug());
			xml.append("  <entry>\n");
			element(xml, "    ", "title", post.title());
			xml.append("    <link href=\"").append(escape(url)).append("\"/>\n");
			element(xml, "    ", "id", url);
			element(xml, "    ", "published", TIMESTAMP.format(post.date()));
			element(xml, "    ", "updated", TIMESTAMP.format(post.modified()));
			if (!post.author().isEmpty()) {
				xml.append("    <author><name>").append(escape(post.author())).append("</name></author>\n");
			}
			for (String tag : post.tags()) {
				xml.append("    <category term=\"").append(escape(tag)).append("\"/>\n");
			}
			element(xml, "    ", "summary", post.summary());
			xml.append("    <content type=\"html\">").append(escape(absoluteImages(post, site))).append("</content>\n");
			xml.append("  </entry>\n");
		}
		xml.append("</feed>\n");
		return xml.toString();
	}

	/** A sitemap of every page the blog serves, less the posts that set a {@code canonical}. */
	public String sitemap(String siteUrl) {
		String site = trim(siteUrl);
		StringBuilder xml = new StringBuilder();
		xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
		xml.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");
		for (PageUrl url : pages.sitemapUrls()) {
			xml.append("  <url><loc>").append(escape(site + url.path())).append("</loc>");
			if (!url.lastModified().isEmpty()) {
				xml.append("<lastmod>").append(escape(url.lastModified())).append("</lastmod>");
			}
			xml.append("</url>\n");
		}
		xml.append("</urlset>\n");
		return xml.toString();
	}

	/**
	 * A post's body for the feed. A feed reader has no page to resolve a path against, so the images
	 * of a post that is a folder get the site's address in front.
	 */
	private String absoluteImages(Post post, String site) {
		if (Bundles.folder(post.path()) == null) {
			return post.html();
		}
		String images = links.post(post.slug()) + "/";
		return post.html().replace("src=\"" + images, "src=\"" + site + images);
	}

	private static void element(StringBuilder xml, String indent, String name, String value) {
		xml.append(indent).append('<').append(name).append('>').append(escape(value)).append("</").append(name).append(">\n");
	}

	private static String trim(String siteUrl) {
		String tidy = siteUrl.strip();
		while (tidy.endsWith("/")) {
			tidy = tidy.substring(0, tidy.length() - 1);
		}
		return tidy;
	}

	/** Escapes the five XML special characters, and drops characters XML 1.0 does not allow. */
	static String escape(String value) {
		StringBuilder out = new StringBuilder(value.length());
		for (int i = 0; i < value.length(); i++) {
			char c = value.charAt(i);
			switch (c) {
				case '&' -> out.append("&amp;");
				case '<' -> out.append("&lt;");
				case '>' -> out.append("&gt;");
				case '"' -> out.append("&quot;");
				case '\'' -> out.append("&apos;");
				default -> {
					if (Character.isHighSurrogate(c) && i + 1 < value.length() && Character.isLowSurrogate(value.charAt(i + 1))) {
						out.append(c).append(value.charAt(++i));
					}
					else if (allowed(c)) {
						out.append(c);
					}
				}
			}
		}
		return out.toString();
	}

	/** A single char XML 1.0 allows: not a control char, not a lone surrogate, not U+FFFE or U+FFFF. */
	private static boolean allowed(char c) {
		if (c == '\n' || c == '\r' || c == '\t') {
			return true;
		}
		return c >= 0x20 && !Character.isSurrogate(c) && c != '\uFFFE' && c != '\uFFFF';
	}

}
