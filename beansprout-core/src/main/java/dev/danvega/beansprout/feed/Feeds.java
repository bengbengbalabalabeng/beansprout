package dev.danvega.beansprout.feed;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

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
		List<Post> recent = posts.published().stream().limit(FEED_SIZE).toList();
		ZonedDateTime updated = recent.isEmpty() ? posts.clock().instant().atZone(ZoneOffset.UTC) : recent.getFirst().date();

		StringBuilder xml = new StringBuilder();
		xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
		xml.append("<feed xmlns=\"http://www.w3.org/2005/Atom\">\n");
		element(xml, "  ", "title", pages.settings().title());
		if (!pages.settings().description().isEmpty()) {
			element(xml, "  ", "subtitle", pages.settings().description());
		}
		xml.append("  <link href=\"").append(escape(site + links.home())).append("\"/>\n");
		xml.append("  <link rel=\"self\" href=\"").append(escape(site + links.feed())).append("\"/>\n");
		element(xml, "  ", "id", site + links.home());
		element(xml, "  ", "updated", TIMESTAMP.format(updated));
		xml.append("  <author><name>").append(escape(pages.settings().title())).append("</name></author>\n");
		for (Post post : recent) {
			String url = site + links.post(post.slug());
			xml.append("  <entry>\n");
			element(xml, "    ", "title", post.title());
			xml.append("    <link href=\"").append(escape(url)).append("\"/>\n");
			element(xml, "    ", "id", url);
			element(xml, "    ", "published", TIMESTAMP.format(post.date()));
			element(xml, "    ", "updated", TIMESTAMP.format(post.date()));
			for (String tag : post.tags()) {
				xml.append("    <category term=\"").append(escape(tag)).append("\"/>\n");
			}
			element(xml, "    ", "summary", post.summary());
			xml.append("    <content type=\"html\">").append(escape(post.html())).append("</content>\n");
			xml.append("  </entry>\n");
		}
		xml.append("</feed>\n");
		return xml.toString();
	}

	/** A sitemap of every page the blog serves. */
	public String sitemap(String siteUrl) {
		String site = trim(siteUrl);
		StringBuilder xml = new StringBuilder();
		xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
		xml.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");
		for (PageUrl url : pages.urls()) {
			xml.append("  <url><loc>").append(escape(site + url.path())).append("</loc>");
			if (!url.lastModified().isEmpty()) {
				xml.append("<lastmod>").append(escape(url.lastModified())).append("</lastmod>");
			}
			xml.append("</url>\n");
		}
		xml.append("</urlset>\n");
		return xml.toString();
	}

	private static void element(StringBuilder xml, String indent, String name, String value) {
		xml.append(indent).append('<').append(name).append('>').append(escape(value)).append("</").append(name).append(">\n");
	}

	private static String trim(String siteUrl) {
		return siteUrl.endsWith("/") ? siteUrl.substring(0, siteUrl.length() - 1) : siteUrl;
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
