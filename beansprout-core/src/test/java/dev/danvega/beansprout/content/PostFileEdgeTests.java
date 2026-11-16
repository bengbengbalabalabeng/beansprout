package dev.danvega.beansprout.content;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/** Files and frontmatter shapes found by the second adversarial review. */
class PostFileEdgeTests {

	private final PostParser parser = new PostParser(ZoneOffset.UTC);

	private final Clock clock = Clock.fixed(Instant.parse("2026-11-16T12:00:00Z"), ZoneOffset.UTC);

	@Test
	@DisplayName("POSTS-1 unknown keys of any shape are ignored and the post is published")
	void unknownKeysAreIgnored() {
		Post post = parser.parse(new MarkdownFile("a.md", """
				---
				title: Hello
				og.image: /hello.png
				my key: something
				image:
				  url: /x.png
				  alt: an x
				date: 2026-01-01
				---
				Body.
				"""));

		assertThat(post.title()).isEqualTo("Hello");
		assertThat(post.date().toLocalDate()).hasToString("2026-01-01");
	}

	@Test
	@DisplayName("POSTS-5 an indented known key is refused, so a meant draft is never published by mistake")
	void indentedKnownKeyIsRefused() {
		assertThatExceptionOfType(InvalidPostException.class)
			.isThrownBy(() -> parser.parse(new MarkdownFile("a.md", "---\ntitle: A\ndate: 2026-01-01\n  draft: true\n---\n")))
			.withMessageContaining("draft");
	}

	@Test
	@DisplayName("POSTS-5 a file that is not UTF-8 is skipped with a problem, and the other posts still load")
	void notUtf8IsSkipped(@TempDir Path dir) throws IOException {
		Files.writeString(dir.resolve("good.md"), "---\ntitle: Good\ndate: 2026-01-01\n---\n");
		Files.write(dir.resolve("bad.md"), "---\ntitle: Café\ndate: 2026-01-01\n---\n".getBytes(StandardCharsets.ISO_8859_1));
		Files.write(dir.resolve("._good.md"), new byte[] { 0, 5, 22, 7, (byte) 0xff, (byte) 0xfe });

		Posts posts = new Posts(new DirectoryPostSource(dir), parser, clock, false);

		assertThat(posts.published()).extracting(Post::slug).containsExactly("good");
		assertThat(posts.problems()).singleElement().asString().contains("bad.md", "UTF-8");
	}

}
