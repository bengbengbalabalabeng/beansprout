package dev.danvega.beansprout.mcp;

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
import dev.danvega.beansprout.view.Links;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/** What the MCP tools do with unlisted posts, from specs/unlisted.md. */
class UnlistedToolsTests {

	private final Clock clock = Clock.fixed(Instant.parse("2026-11-17T12:00:00Z"), ZoneOffset.UTC);

	@TempDir
	Path folder;

	private BlogTools blog;

	@BeforeEach
	void posts() throws IOException {
		write("hello.md", "---\ntitle: Hello\ndate: 2026-11-01\ntags: [java]\n---\nHi.\n");
		write("keynote.md", "---\ntitle: Notes for the Porto keynote\ndate: 2026-11-10\ntags: [java, porto]\n"
				+ "unlisted: true\n---\nThe *slides* are here.\n");
		write("later.md", "---\ntitle: Later\ndate: 2999-01-01\nunlisted: true\n---\nOne day.\n");
		write("secret.md", "---\ntitle: Secret\ndate: 2026-11-05\nunlisted: true\ndraft: true\n---\nNot yet.\n");
		DirectoryPostSource source = new DirectoryPostSource(folder);
		Posts posts = new Posts(source, new PostParser(ZoneOffset.UTC, CodeHighlighter.NONE), clock, false);
		blog = new BlogTools(posts, source, () -> "https://example.com", new Links("", "/blog"));
	}

	private void write(String path, String text) throws IOException {
		Files.writeString(folder.resolve(path), text);
	}

	@Test
	@DisplayName("UNLISTED-12 list_posts and find_posts_by_tag leave unlisted posts out")
	void notListed() {
		assertThat(blog.listPosts()).extracting(BlogTools.PostSummary::slug).containsExactly("hello");
		assertThat(blog.findPostsByTag("java")).extracting(BlogTools.PostSummary::slug).containsExactly("hello");
		assertThat(blog.findPostsByTag("porto")).isEmpty();
		assertThat(blog.listTags()).extracting(BlogTools.TagCount::slug).containsExactly("java");
	}

	@Test
	@DisplayName("UNLISTED-13 read_post returns an unlisted post by its slug")
	void readBySlug() {
		BlogTools.PostText keynote = blog.readPost("keynote");

		assertThat(keynote.title()).isEqualTo("Notes for the Porto keynote");
		assertThat(keynote.markdown()).contains("The *slides* are here.");
		assertThat(keynote.url()).isEqualTo("https://example.com/blog/keynote");
		assertThatIllegalArgumentException().isThrownBy(() -> blog.readPost("later"));
		assertThatIllegalArgumentException().isThrownBy(() -> blog.readPost("secret"));
	}

}
