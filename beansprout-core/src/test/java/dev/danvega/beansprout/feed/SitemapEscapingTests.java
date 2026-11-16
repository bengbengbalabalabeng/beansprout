package dev.danvega.beansprout.feed;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
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

class SitemapEscapingTests {

	@Test
	@DisplayName("FEEDS-7 every value in the sitemap is XML-escaped")
	void sitemapEscaping() {
		Clock clock = Clock.fixed(Instant.parse("2026-11-16T12:00:00Z"), ZoneOffset.UTC);
		Posts posts = new Posts(() -> List.of(new MarkdownFile("a.md", "---\ntitle: A\ndate: 2026-01-01\n---\n")),
				new PostParser(ZoneOffset.UTC), clock, false);
		Links links = new Links("", "/blog");
		Feeds feeds = new Feeds(posts, links, new Pages(posts, links, new BlogSettings("B", "", 10)));

		assertThat(feeds.sitemap("https://example.com/?x=1&y=<2>"))
			.contains("<loc>https://example.com/?x=1&amp;y=&lt;2&gt;/blog/a</loc>").doesNotContain("&y=<2>");
	}

}
