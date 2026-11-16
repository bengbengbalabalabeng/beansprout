package dev.danvega.beansprout.autoconfigure;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import dev.danvega.beansprout.content.MarkdownFile;
import dev.danvega.beansprout.content.Post;
import dev.danvega.beansprout.content.Posts;
import dev.danvega.beansprout.web.BlogController;
import dev.danvega.beansprout.web.FeedController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.webmvc.autoconfigure.DispatcherServletAutoConfiguration;
import org.springframework.boot.webmvc.autoconfigure.WebMvcAutoConfiguration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;

/** The loader the auto-configuration uses, with the names and locations real apps have. */
class ResourcePostSourceTests {

	private static final AutoConfigurations BEANSPROUT = AutoConfigurations.of(BeansproutAutoConfiguration.class,
			BeansproutJteAutoConfiguration.class, BeansproutWebAutoConfiguration.class);

	private final PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

	@Test
	@DisplayName("POSTS-3 a file name with spaces or accents gives a clean slug from the classpath")
	void spacesAndAccentsOnTheClasspath() {
		new ApplicationContextRunner().withConfiguration(BEANSPROUT)
			.withPropertyValues("beansprout.posts.location=classpath:named/")
			.run(context -> assertThat(context.getBean(Posts.class).published())
				.extracting(Post::slug, Post::path)
				.containsExactlyInAnyOrder(
						org.assertj.core.groups.Tuple.tuple("my-first-post", "My First Post.md"),
						org.assertj.core.groups.Tuple.tuple("cafe-notes", "sub/Café Notes.md")));
	}

	@Test
	@DisplayName("POSTS-3 a file name with spaces gives a clean slug from a folder on disk")
	void spacesOnDisk(@TempDir Path dir) throws IOException {
		Files.createDirectories(dir.resolve("sub dir"));
		Files.writeString(dir.resolve("sub dir/Deep Post.md"), "---\ntitle: Deep\ndate: 2026-01-01\n---\n");

		assertThat(new ResourcePostSource(resolver, "file:" + dir + "/").files()).extracting(MarkdownFile::path)
			.containsExactly("sub dir/Deep Post.md");
	}

	@Test
	@DisplayName("POSTS-13 a relative file: location keeps subfolders in each path")
	void relativeLocation() throws IOException {
		Path root = Path.of("target/relative-posts");
		Files.createDirectories(root.resolve("a"));
		Files.createDirectories(root.resolve("b"));
		Files.writeString(root.resolve("a/hello.md"), "---\ntitle: A\ndate: 2026-01-01\n---\n");
		Files.writeString(root.resolve("b/hello.md"), "---\ntitle: B\ndate: 2026-01-02\n---\n");

		assertThat(new ResourcePostSource(resolver, "file:./target/relative-posts/").files()).extracting(MarkdownFile::path)
			.containsExactlyInAnyOrder("a/hello.md", "b/hello.md");
	}

	@Test
	@DisplayName("POSTS-5 a file that is not UTF-8 is skipped with a problem, and the other posts still load")
	void notUtf8OnDisk(@TempDir Path dir) throws IOException {
		Files.writeString(dir.resolve("good.md"), "---\ntitle: Good\ndate: 2026-01-01\n---\n");
		Files.write(dir.resolve("bad.md"), "---\ntitle: Caf\u00e9\ndate: 2026-01-01\n---\n".getBytes(StandardCharsets.ISO_8859_1));
		Files.write(dir.resolve("._good.md"), new byte[] { 0, 5, 22, 7, (byte) 0xff });

		new ApplicationContextRunner().withConfiguration(BEANSPROUT)
			.withPropertyValues("beansprout.posts.location=file:" + dir + "/")
			.run(context -> {
				Posts posts = context.getBean(Posts.class);
				assertThat(posts.published()).extracting(Post::slug).containsExactly("good");
				assertThat(posts.problems()).singleElement().asString().contains("bad.md", "UTF-8");
			});
	}

	@Test
	@DisplayName("POSTS-15 with no beansprout.time-zone, a plain date starts in the server's zone")
	void serverZoneByDefault() {
		new ApplicationContextRunner().withConfiguration(BEANSPROUT).run(context -> assertThat(
				context.getBean(Posts.class).find("hello").orElseThrow().date().getZone())
			.isEqualTo(java.time.ZoneId.systemDefault()));
	}

	@Test
	@DisplayName("FEEDS-1 the feed and the sitemap are served even when no theme is on the classpath")
	void feedWithoutTheme() {
		new WebApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(DispatcherServletAutoConfiguration.class, WebMvcAutoConfiguration.class))
			.withConfiguration(BEANSPROUT)
			.run(context -> {
				assertThat(context).hasSingleBean(FeedController.class).doesNotHaveBean(BlogController.class);
				MockMvcTester mvc = MockMvcTester.from(context);
				assertThat(mvc.get().uri("/blog/feed.xml")).hasStatusOk().bodyText()
					.contains("<feed", "Hello from the classpath");
				assertThat(mvc.get().uri("/blog/sitemap.xml")).hasStatusOk().bodyText().contains("/blog/hello");
				assertThat(mvc.get().uri("/blog")).hasStatus(404);
			});
	}

}
