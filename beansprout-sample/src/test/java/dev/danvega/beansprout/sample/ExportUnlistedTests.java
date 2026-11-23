package dev.danvega.beansprout.sample;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Unlisted posts in the static export, from specs/unlisted.md. */
class ExportUnlistedTests {

	private static String read(Path file) throws IOException {
		return Files.readString(file, StandardCharsets.UTF_8);
	}

	@Test
	@DisplayName("UNLISTED-16 the static export writes every live unlisted post, with noindex, and keeps it out of the sitemap")
	void writesUnlistedPosts() throws IOException {
		Path out = Files.createTempDirectory("beansprout-export").resolve("site");
		StaticExportTests.export(out, "--beansprout.posts.location=classpath:test-posts-unlisted/").close();

		String keynote = read(out.resolve("blog/keynote.html"));
		assertThat(keynote).contains("The body of the keynote notes.", "<meta name=\"robots\" content=\"noindex\">");
		assertThat(read(out.resolve("old/keynote.html"))).as("its alias page").contains("url=/blog/keynote");
		assertThat(read(out.resolve("blog/sitemap.xml"))).contains("/blog/hello").doesNotContain("keynote");
		assertThat(read(out.resolve("blog/feed.xml"))).contains("Hello Beans").doesNotContain("keynote");
		assertThat(read(out.resolve("blog.html"))).doesNotContain("keynote");
		assertThat(out.resolve("blog/secret.html")).as("a draft").doesNotExist();
		assertThat(out.resolve("blog/later.html")).as("dated later").doesNotExist();
		assertThat(out.resolve("blog/tags/porto.html")).as("a tag only it uses").doesNotExist();
	}

}
