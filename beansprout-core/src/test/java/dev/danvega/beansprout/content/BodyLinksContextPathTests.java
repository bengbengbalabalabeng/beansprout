package dev.danvega.beansprout.content;

import java.time.ZoneOffset;

import dev.danvega.beansprout.code.CodeHighlighter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Links and images in a post's body get the app's context path, like a {@code picture:} does. */
class BodyLinksContextPathTests {

	private static String html(String root, String body) {
		return new PostParser(ZoneOffset.UTC, CodeHighlighter.NONE, root, "/blog")
			.parse(new MarkdownFile("a.md", "---\ntitle: A\ndate: 2026-01-01\n---\n" + body))
			.html();
	}

	private static String bundleHtml(String root, String body) {
		return new PostParser(ZoneOffset.UTC, CodeHighlighter.NONE, root, "/blog")
			.parse(new MarkdownFile("trip/index.md", "---\ntitle: A\ndate: 2026-01-01\n---\n" + body))
			.html();
	}

	@Test
	@DisplayName("EXPORT-30 a Markdown link that starts with / gets the context path")
	void markdownLink() {
		assertThat(html("/notes", "[Other](/blog/other)")).contains("href=\"/notes/blog/other\"");
	}

	@Test
	@DisplayName("EXPORT-30 a Markdown image that starts with / gets the context path")
	void markdownImage() {
		assertThat(html("/notes", "![A chart](/images/chart.png)")).contains("src=\"/notes/images/chart.png\"");
	}

	@Test
	@DisplayName("EXPORT-30 a raw HTML link and image that start with / get the context path")
	void rawHtml() {
		String html = html("/notes", "<p><a href='/about'>About</a> <img alt=\"x\" src=\"/images/a.png\"></p>");
		assertThat(html).contains("href=\"/notes/about\"").contains("src=\"/notes/images/a.png\"");
	}

	@Test
	@DisplayName("EXPORT-30 a query, a fragment and an escaped & survive the context path")
	void queryAndFragment() {
		assertThat(html("/notes", "[Q](/search?q=a&b=c#top)")).contains("href=\"/notes/search?q=a&amp;b=c#top\"");
	}

	@Test
	@DisplayName("EXPORT-30 a link to another host, a // link, a relative link and a fragment stay as written")
	void othersStay() {
		String html = html("/notes",
				"[a](https://example.com/x) [b](//cdn.example.com/y) [c](other) [d](#top) [e](mailto:a@example.com)");
		assertThat(html).contains("href=\"https://example.com/x\"")
			.contains("href=\"//cdn.example.com/y\"")
			.contains("href=\"other\"")
			.contains("href=\"#top\"")
			.contains("href=\"mailto:a@example.com\"")
			.doesNotContain("/notes");
	}

	@Test
	@DisplayName("EXPORT-30 with no context path a link stays as written")
	void noContextPath() {
		assertThat(html("", "[Other](/blog/other) ![A](/images/a.png)")).contains("href=\"/blog/other\"")
			.contains("src=\"/images/a.png\"");
	}

	@Test
	@DisplayName("EXPORT-30 a link shown as code is not changed")
	void codeStays() {
		assertThat(html("/notes", "`<a href=\"/x\">`\n\n```\n<img src=\"/y.png\">\n```\n"))
			.doesNotContain("/notes");
	}

	@Test
	@DisplayName("EXPORT-30 a bundle's own image gets the context path once")
	void bundleImageOnce() {
		assertThat(bundleHtml("/notes", "![Shot](shot.png) ![Chart](/images/chart.png)"))
			.contains("src=\"/notes/blog/trip/shot.png\"")
			.contains("src=\"/notes/images/chart.png\"")
			.doesNotContain("/notes/notes");
	}

	@Test
	@DisplayName("EXPORT-30 a raw HTML link with a bare & in its query still gets the context path")
	void bareAmpersand() {
		assertThat(html("/notes", "<p><a href=\"/x?a=1&b=2\">x</a> <a href='/y?c=1&d'>y</a></p>"))
			.contains("href=\"/notes/x?a=1&b=2\"")
			.contains("href=\"/notes/y?c=1&d\"");
	}
}
