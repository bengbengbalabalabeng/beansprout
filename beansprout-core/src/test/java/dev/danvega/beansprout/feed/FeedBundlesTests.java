package dev.danvega.beansprout.feed;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import dev.danvega.beansprout.code.CodeHighlighter;
import dev.danvega.beansprout.content.DirectoryPostSource;
import dev.danvega.beansprout.content.PostParser;
import dev.danvega.beansprout.content.Posts;
import dev.danvega.beansprout.view.BlogSettings;
import dev.danvega.beansprout.view.Links;
import dev.danvega.beansprout.view.Pages;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

class FeedBundlesTests {

	@TempDir
	Path folder;

	@Test
	@DisplayName("BUNDLE-19 the feed links a bundle's relative images by their absolute address")
	void bundleImagesAreAbsoluteInTheFeed() throws IOException {
		Path post = folder.resolve("welcome/index.md");
		Files.createDirectories(post.getParent());
		Files.writeString(post, "---\ntitle: Welcome\ndate: 2026-11-10\n---\n![A shot](shots/a.png)\n\n![Logo](/images/logo.png)\n\n<img src='shots/single.png'>\n");

		Clock clock = Clock.fixed(Instant.parse("2026-11-17T12:00:00Z"), ZoneOffset.UTC);
		Posts posts = new Posts(new DirectoryPostSource(folder),
				new PostParser(ZoneOffset.UTC, CodeHighlighter.NONE, "/app", "/blog"), clock, false);
		Links links = new Links("/app", "/blog");
		Feeds feeds = new Feeds(posts, links, new Pages(posts, links, new BlogSettings("Notes", "", 10)));

		String atom = feeds.atom("https://example.com");

		assertThat(atom).contains("src=&quot;https://example.com/app/blog/welcome/shots/a.png&quot;")
			.contains("src=&quot;/images/logo.png&quot;")
			.contains("src=&quot;https://example.com/app/blog/welcome/shots/single.png&quot;");
	}

}
