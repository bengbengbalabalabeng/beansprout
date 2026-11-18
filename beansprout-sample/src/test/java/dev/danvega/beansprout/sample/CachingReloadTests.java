package dev.danvega.beansprout.sample;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import static org.assertj.core.api.Assertions.assertThat;

/** An edited post, read again with beansprout.posts.reload, gets new ETags. */
@SpringBootTest(properties = "beansprout.posts.reload=true")
@AutoConfigureMockMvc
class CachingReloadTests {

	private static final Path FOLDER = folder();

	@Autowired
	private MockMvcTester mvc;

	@DynamicPropertySource
	static void posts(DynamicPropertyRegistry registry) {
		registry.add("beansprout.posts.location", () -> FOLDER.toUri().toString());
	}

	private static Path folder() {
		try {
			Path folder = Files.createTempDirectory("beansprout-reload");
			write(folder, "The first version.");
			return folder;
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	private static void write(Path folder, String body) throws IOException {
		Files.writeString(folder.resolve("edited.md"), "---\ntitle: Edited\ndate: 2026-03-01\n---\n" + body + "\n",
				StandardCharsets.UTF_8);
	}

	private String etag(String path) {
		MvcTestResult result = mvc.get().uri(path).exchange();
		assertThat(result).as(path).hasStatusOk();
		return result.getResponse().getHeader("ETag");
	}

	private static int edits;

	/** Writes a new version of the post and returns its body, so each test sees its own change. */
	private static String edit() throws IOException {
		String body = "Version " + (++edits) + ".";
		write(FOLDER, body);
		return body;
	}

	@Test
	@DisplayName("CACHE-6 when a post's file changes and reload is on, its post page gets a new ETag")
	void editChangesPostEtag() throws IOException {
		String before = etag("/blog/edited");
		assertThat(etag("/blog/edited")).isEqualTo(before);
		String body = edit();
		assertThat(mvc.get().uri("/blog/edited")).bodyText().contains(body);
		assertThat(etag("/blog/edited")).isNotEqualTo(before);
	}

	@Test
	@DisplayName("CACHE-13 when a post's file changes and reload is on, the feed gets a new ETag")
	void editChangesFeedEtag() throws IOException {
		String before = etag("/blog/feed.xml");
		assertThat(etag("/blog/feed.xml")).isEqualTo(before);
		String body = edit();
		assertThat(mvc.get().uri("/blog/feed.xml")).bodyText().contains(body);
		assertThat(etag("/blog/feed.xml")).isNotEqualTo(before);
	}

}
