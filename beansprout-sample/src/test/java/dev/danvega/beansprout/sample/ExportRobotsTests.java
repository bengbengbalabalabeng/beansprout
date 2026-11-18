package dev.danvega.beansprout.sample;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** robots.txt in the static export. */
class ExportRobotsTests {

	@Test
	@DisplayName("ROBOTS-15 the static export writes Beansprout's robots.txt at the root of the output folder")
	void written() throws IOException {
		Path out = Files.createTempDirectory("beansprout-export").resolve("site");
		StaticExportTests.export(out, "--beansprout.robots.ai-training=block").close();
		assertThat(Files.readString(out.resolve("robots.txt"), StandardCharsets.UTF_8))
			.startsWith("User-agent: *\nAllow: /\n")
			.contains("User-agent: GPTBot\n", "Disallow: /blog\n")
			.endsWith("\nSitemap: https://example.com/blog/sitemap.xml\n");
	}

	@Test
	@DisplayName("ROBOTS-15 a post that links to /robots.txt does not make the export write it twice")
	void linkedFromAPost() throws IOException {
		Path out = Files.createTempDirectory("beansprout-export").resolve("site");
		StaticExportTests.export(out, "--beansprout.posts.location=classpath:test-posts-robots-link/").close();
		assertThat(Files.readString(out.resolve("blog/crawlers.html"), StandardCharsets.UTF_8))
			.contains("href=\"/robots.txt\"");
		assertThat(Files.readString(out.resolve("robots.txt"), StandardCharsets.UTF_8)).startsWith("User-agent: *\n");
	}

	@Test
	@DisplayName("ROBOTS-15 a project-site export with a context path writes no robots.txt, since none is served")
	void notWrittenUnderAContextPath() throws IOException {
		Path out = Files.createTempDirectory("beansprout-export").resolve("site");
		StaticExportTests.export(out, "--server.servlet.context-path=/my-blog").close();
		assertThat(out.resolve("blog.html")).exists();
		assertThat(out.resolve("robots.txt")).doesNotExist();
	}

}
