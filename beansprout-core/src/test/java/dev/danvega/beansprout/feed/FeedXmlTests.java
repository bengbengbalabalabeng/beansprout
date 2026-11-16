package dev.danvega.beansprout.feed;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;

import dev.danvega.beansprout.content.MarkdownFile;
import dev.danvega.beansprout.content.PostParser;
import dev.danvega.beansprout.content.Posts;
import dev.danvega.beansprout.view.BlogSettings;
import dev.danvega.beansprout.view.Links;
import dev.danvega.beansprout.view.Pages;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

import static org.assertj.core.api.Assertions.assertThat;

/** The feed and the sitemap parsed as XML, not just searched as text. */
class FeedXmlTests {

	private final Feeds feeds;

	FeedXmlTests() {
		Clock clock = Clock.fixed(Instant.parse("2026-11-16T12:00:00Z"), ZoneOffset.UTC);
		Posts posts = new Posts(() -> List.of(
				new MarkdownFile("a.md", "---\ntitle: Tom & <Jerry>\ndate: 2026-01-01\ntags: [\"C & C++\", java]\n---\nA <b>bold</b> body.\n"),
				new MarkdownFile("b.md", "---\ntitle: Second\ndate: 2026-01-02\n---\nTwo.\n")),
				new PostParser(ZoneOffset.UTC), clock, false);
		Links links = new Links("", "/blog");
		feeds = new Feeds(posts, links, new Pages(posts, links, new BlogSettings("Bean & <Notes>", "A \"quoted\" line", 10)));
	}

	private static Document parse(String xml) throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		return factory.newDocumentBuilder().parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
	}

	@Test
	@DisplayName("FEEDS-7 the feed is well-formed XML, and every escaped value reads back as written")
	void feedIsWellFormed() throws Exception {
		Document atom = parse(feeds.atom("https://example.com"));

		assertThat(atom.getElementsByTagName("entry").getLength()).isEqualTo(2);
		assertThat(atom.getElementsByTagName("title").item(0).getTextContent()).isEqualTo("Bean & <Notes>");
		assertThat(atom.getElementsByTagName("subtitle").item(0).getTextContent()).isEqualTo("A \"quoted\" line");
		assertThat(atom.getElementsByTagName("category").item(0).getAttributes().getNamedItem("term").getNodeValue())
			.isEqualTo("C & C++");
		assertThat(atom.getElementsByTagName("content").item(1).getTextContent()).contains("<b>bold</b>");
	}

	@Test
	@DisplayName("FEEDS-4 the sitemap is well-formed XML with one url per page")
	void sitemapIsWellFormed() throws Exception {
		Document sitemap = parse(feeds.sitemap("https://example.com"));

		assertThat(sitemap.getElementsByTagName("url").getLength()).isEqualTo(6); // index, archive, two posts, two tags
	}

}
