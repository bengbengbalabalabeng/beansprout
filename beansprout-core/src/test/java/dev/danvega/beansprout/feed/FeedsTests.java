package dev.danvega.beansprout.feed;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import dev.danvega.beansprout.content.MarkdownFile;
import dev.danvega.beansprout.content.PostParser;
import dev.danvega.beansprout.content.Posts;
import dev.danvega.beansprout.view.BlogSettings;
import dev.danvega.beansprout.view.Links;
import dev.danvega.beansprout.view.Pages;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FeedsTests {

	private final Clock clock = Clock.fixed(Instant.parse("2026-11-16T12:00:00Z"), ZoneOffset.UTC);

	private final List<MarkdownFile> files = new ArrayList<>();

	private Feeds feeds() {
		Posts posts = new Posts(() -> List.copyOf(files), new PostParser(ZoneOffset.UTC), clock, false);
		Links links = new Links("", "/blog");
		return new Feeds(posts, links, new Pages(posts, links, new BlogSettings("Bean & Notes", "", 10)));
	}

	private void add(String slug, String date, String extra) {
		files.add(new MarkdownFile(slug + ".md", "---\ntitle: Post " + slug + "\ndate: " + date + "\n" + extra
				+ "---\nBody of " + slug + ".\n"));
	}

	@Test
	@DisplayName("FEEDS-1 the feed is Atom with the 20 newest published posts")
	void atomWithTwentyNewest() {
		for (int i = 1; i <= 25; i++) {
			add("p" + i, "2026-01-%02d".formatted(i), "");
		}

		String atom = feeds().atom("https://example.com");

		assertThat(atom).startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
			.contains("<feed xmlns=\"http://www.w3.org/2005/Atom\">", "<title>Bean &amp; Notes</title>");
		assertThat(atom.split("<entry>", -1)).hasSize(21);
		assertThat(atom).contains("Post p25", "Post p6").doesNotContain("Post p5<");
		assertThat(atom).contains("<updated>2026-01-25T00:00:00Z</updated>");
	}

	@Test
	@DisplayName("FEEDS-2 each entry has the title, absolute link and id, date, summary and HTML body")
	void entryFields() {
		add("hello", "2026-03-14T09:30:00Z", "summary: A short one.\n");

		String atom = feeds().atom("https://example.com/");

		assertThat(atom).contains(
				"<title>Post hello</title>",
				"<link href=\"https://example.com/blog/hello\"/>",
				"<id>https://example.com/blog/hello</id>",
				"<published>2026-03-14T09:30:00Z</published>",
				"<updated>2026-03-14T09:30:00Z</updated>",
				"<summary>A short one.</summary>",
				"<content type=\"html\">&lt;p&gt;Body of hello.&lt;/p&gt;");
		assertThat(atom).contains("<link rel=\"self\" href=\"https://example.com/blog/feed.xml\"/>");
	}

	@Test
	@DisplayName("FEEDS-4 the sitemap lists every page with an absolute address")
	void sitemap() {
		add("hello", "2026-03-14", "tags: [java]\n");

		String sitemap = feeds().sitemap("https://example.com");

		assertThat(sitemap).contains("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">",
				"<loc>https://example.com/blog</loc>", "<loc>https://example.com/blog/archive</loc>",
				"<loc>https://example.com/blog/hello</loc>", "<loc>https://example.com/blog/tags/java</loc>");
	}

	@Test
	@DisplayName("FEEDS-5 a post in the sitemap has its date as lastmod")
	void sitemapLastmod() {
		add("hello", "2026-03-14", "");

		assertThat(feeds().sitemap("https://example.com"))
			.contains("<url><loc>https://example.com/blog/hello</loc><lastmod>2026-03-14</lastmod></url>");
	}

	@Test
	@DisplayName("FEEDS-6 drafts and future posts are left out of the feed and the sitemap")
	void hiddenPostsLeftOut() {
		add("live", "2026-01-01", "");
		add("draft", "2026-01-02", "draft: true\n");
		add("future", "2027-01-01", "");
		Feeds feeds = feeds();

		assertThat(feeds.atom("https://example.com")).contains("Post live").doesNotContain("draft", "future");
		assertThat(feeds.sitemap("https://example.com")).contains("/blog/live").doesNotContain("draft", "future");
	}

	@Test
	@DisplayName("FEEDS-7 every value in the feed and the sitemap is XML-escaped")
	void escaping() {
		files.add(new MarkdownFile("x.md", "---\ntitle: \"Tom & <Jerry> \\\"quoted\\\"\"\ndate: 2026-01-01\nslug: tom\n"
				+ "summary: a < b & c\n---\nHi.\n"));

		String atom = feeds().atom("https://example.com/?a=1&b=2");

		assertThat(atom).contains("<title>Tom &amp; &lt;Jerry&gt;", "<summary>a &lt; b &amp; c</summary>",
				"https://example.com/?a=1&amp;b=2").doesNotContain("<Jerry>", "a < b");
	}

	@Test
	@DisplayName("FEEDS-1 an empty blog still has a valid feed")
	void emptyFeed() {
		String atom = feeds().atom("https://example.com");

		assertThat(atom).contains("<feed", "</feed>", "<updated>").doesNotContain("<entry>");
	}

}
