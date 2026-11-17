package dev.danvega.beansprout.autoconfigure;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import dev.danvega.beansprout.content.Post;
import dev.danvega.beansprout.content.Posts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/** Images that live in a post's folder, served at the post's address. */
class BundleImagesTests {

	private static final Instant CHANGED = Instant.parse("2026-11-12T08:30:00Z");

	@TempDir
	Path posts;

	@TempDir
	Path outside;

	private WebApplicationContextRunner web(boolean reload) {
		return new WebApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(BeansproutAutoConfiguration.class,
					BeansproutJteAutoConfiguration.class, BeansproutWebAutoConfiguration.class))
			.withPropertyValues("beansprout.posts.location=" + posts.toUri(), "beansprout.posts.reload=" + reload);
	}

	@BeforeEach
	void folder() throws IOException {
		post("welcome/index.md", "");
		image("welcome/cover.png");
		image("welcome/shots/a.webp");
		for (String type : new String[] { "jpg", "jpeg", "gif", "avif" }) {
			image("welcome/photo." + type);
		}
		image("welcome/logo.svg");
		write("welcome/notes.txt", "notes");
		image("secret.png");
		post("hidden/index.md", "draft: true\n");
		image("hidden/a.png");
		post("later/index.md", "date: 2999-01-01\n");
		image("later/a.png");
		post("plain.md", "");
		image("plain/secret.png");
		image("welcome/100%.png");
		post("Hash #2/index.md", "");
		image("Hash #2/cover.png");
		post("renamed/index.md", "slug: other\n");
		image("renamed/cover.png");
		Files.writeString(outside.resolve("secret.png"), "outside bytes");
		Files.createSymbolicLink(posts.resolve("welcome/leak.png"), outside.resolve("secret.png"));
		Files.createSymbolicLink(posts.resolve("welcome/linked"), outside);
	}

	private void write(String path, String text) throws IOException {
		Path file = posts.resolve(path);
		Files.createDirectories(file.getParent());
		Files.writeString(file, text);
	}

	private void image(String path) throws IOException {
		write(path, "image bytes of " + path);
		Files.setLastModifiedTime(posts.resolve(path), FileTime.from(CHANGED));
	}

	private void post(String path, String extra) throws IOException {
		post(path, extra, "Hello.");
	}

	private void post(String path, String extra, String body) throws IOException {
		String date = extra.contains("date:") ? "" : "date: 2026-11-10\n";
		write(path, "---\ntitle: A post\n" + date + extra + "---\n" + body + "\n");
	}

	@Test
	@DisplayName("BUNDLE-6 an image in a bundle is served at the post's address plus its path in the folder")
	void servesImages() {
		web(false).run(context -> {
			MockMvcTester mvc = mvc(context);
			assertThat(mvc.get().uri("/blog/welcome/cover.png")).hasStatusOk()
				.body()
				.asString()
				.isEqualTo("image bytes of welcome/cover.png");
			assertThat(mvc.get().uri("/blog/welcome/shots/a.webp")).hasStatusOk()
				.body()
				.asString()
				.isEqualTo("image bytes of welcome/shots/a.webp");
			assertThat(mvc.get().uri(URI.create("/blog/welcome/100%25.png"))).hasStatusOk()
				.body()
				.asString()
				.isEqualTo("image bytes of welcome/100%.png");
			assertThat(mvc.get().uri("/blog/hash-2/cover.png")).hasStatusOk()
				.body()
				.asString()
				.isEqualTo("image bytes of Hash #2/cover.png");
			assertThat(mvc.get().uri("/blog/other/cover.png")).hasStatusOk()
				.body()
				.asString()
				.isEqualTo("image bytes of renamed/cover.png");
			assertThat(mvc.get().uri("/blog/renamed/cover.png")).hasStatus(404);
		});
	}

	@Test
	@DisplayName("BUNDLE-7 only png, jpg, jpeg, gif, webp and avif files are served from a bundle")
	void onlyImageTypes() {
		web(false).run(context -> {
			MockMvcTester mvc = mvc(context);
			for (String type : new String[] { "jpg", "jpeg", "gif", "avif" }) {
				assertThat(mvc.get().uri("/blog/welcome/photo." + type)).hasStatusOk();
			}
			assertThat(mvc.get().uri("/blog/welcome/logo.svg")).hasStatus(404);
		});
	}

	@Test
	@DisplayName("BUNDLE-8 a path that leaves the bundle's folder answers 404")
	void cannotLeaveTheFolder() {
		web(false).run(context -> {
			MockMvcTester mvc = mvc(context);
			assertThat(mvc.get().uri(URI.create("/blog/welcome/../secret.png"))).hasStatus(404);
			assertThat(mvc.get().uri(URI.create("/blog/welcome/%2e%2e/secret.png"))).hasStatus(404);
			assertThat(mvc.get().uri(URI.create("/blog/welcome/shots/../../secret.png"))).hasStatus(404);
			assertThat(mvc.get().uri("/blog/plain/secret.png")).hasStatus(404);
			assertThat(mvc.get().uri("/blog/welcome/leak.png")).hasStatus(404);
			assertThat(mvc.get().uri("/blog/welcome/linked/secret.png")).hasStatus(404);
		});
	}

	@Test
	@DisplayName("BUNDLE-9 a file in a bundle that is not an allowed image answers 404")
	void notAnImage() {
		web(false).run(context -> {
			MockMvcTester mvc = mvc(context);
			assertThat(mvc.get().uri("/blog/welcome/index.md")).hasStatus(404);
			assertThat(mvc.get().uri("/blog/welcome/notes.txt")).hasStatus(404);
			assertThat(mvc.get().uri("/blog/welcome/missing.png")).hasStatus(404);
		});
	}

	@Test
	@DisplayName("BUNDLE-10 a draft's or a future post's images answer 404")
	void unpublishedImagesAreHidden() {
		web(false).run(context -> {
			MockMvcTester mvc = mvc(context);
			assertThat(mvc.get().uri("/blog/hidden/a.png")).hasStatus(404);
			assertThat(mvc.get().uri("/blog/later/a.png")).hasStatus(404);
		});
	}

	@Test
	@DisplayName("BUNDLE-11 a bundle image is sent with the content type of its extension")
	void contentTypes() {
		web(false).run(context -> {
			MockMvcTester mvc = mvc(context);
			assertThat(mvc.get().uri("/blog/welcome/cover.png")).hasContentType("image/png");
			assertThat(mvc.get().uri("/blog/welcome/shots/a.webp")).hasContentType("image/webp");
			assertThat(mvc.get().uri("/blog/welcome/photo.jpg")).hasContentType("image/jpeg");
			assertThat(mvc.get().uri("/blog/welcome/photo.jpeg")).hasContentType("image/jpeg");
			assertThat(mvc.get().uri("/blog/welcome/photo.gif")).hasContentType("image/gif");
			assertThat(mvc.get().uri("/blog/welcome/photo.avif")).hasContentType("image/avif");
		});
	}

	@Test
	@DisplayName("BUNDLE-12 a bundle image is sent with a Last-Modified header")
	void lastModified() {
		web(false).run(context -> assertThat(mvc(context).get().uri("/blog/welcome/cover.png")).hasStatusOk()
			.headers()
			.hasValue("Last-Modified", httpDate(CHANGED)));
	}

	@Test
	@DisplayName("BUNDLE-13 a request whose copy is current gets 304 with no body")
	void notModified() {
		web(false).run(context -> {
			MockMvcTester mvc = mvc(context);
			assertThat(mvc.get().uri("/blog/welcome/cover.png").header("If-Modified-Since", httpDate(CHANGED)))
				.hasStatus(304)
				.body()
				.isEmpty();
			assertThat(mvc.get()
				.uri("/blog/welcome/cover.png")
				.header("If-Modified-Since", httpDate(CHANGED.minusSeconds(60)))).hasStatusOk()
				.body()
				.asString()
				.isEqualTo("image bytes of welcome/cover.png");
		});
	}

	@Test
	@DisplayName("BUNDLE-14 with reload on, a bundle added while the app runs serves its images")
	void reload() {
		web(true).run(context -> {
			MockMvcTester mvc = mvc(context);
			assertThat(mvc.get().uri("/blog/new-post/a.png")).hasStatus(404);
			post("new-post/index.md", "");
			image("new-post/a.png");
			assertThat(mvc.get().uri("/blog/new-post/a.png")).hasStatusOk();
			write("new-post/a.png", "new bytes");
			assertThat(mvc.get().uri("/blog/new-post/a.png")).hasStatusOk().body().asString().isEqualTo("new bytes");
		});
	}

	@Test
	@DisplayName("BUNDLE-15 with a context path and a base path, a bundle's body links its images under both")
	void contextAndBasePath() throws IOException {
		post("pictured/index.md", "", "![A shot](shot.png)");
		image("pictured/shot.png");
		web(false).withPropertyValues("server.servlet.context-path=/app", "beansprout.base-path=/news").run(context -> {
			assertThat(context.getBean(Posts.class).find("pictured")).get()
				.extracting(Post::html)
				.asString()
				.contains("src=\"/app/news/pictured/shot.png\"");
			assertThat(mvc(context).get().uri("/news/pictured/shot.png")).hasStatusOk();
		});
	}

	private static String httpDate(Instant instant) {
		return DateTimeFormatter.RFC_1123_DATE_TIME.format(instant.atOffset(ZoneOffset.UTC));
	}

	private static MockMvcTester mvc(WebApplicationContext context) {
		return MockMvcTester.create(MockMvcBuilders.webAppContextSetup(context).build());
	}

}
