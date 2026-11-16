package dev.danvega.beansprout.autoconfigure;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import dev.danvega.beansprout.content.Post;
import dev.danvega.beansprout.content.Posts;
import dev.danvega.beansprout.view.Pages;
import dev.danvega.beansprout.web.BlogController;
import dev.danvega.beansprout.web.FeedController;
import dev.danvega.beansprout.web.ThemeRenderer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class BeansproutAutoConfigurationTests {

	private static final AutoConfigurations BEANSPROUT = AutoConfigurations.of(BeansproutAutoConfiguration.class,
			BeansproutJteAutoConfiguration.class, BeansproutWebAutoConfiguration.class);

	private final WebApplicationContextRunner web = new WebApplicationContextRunner().withConfiguration(BEANSPROUT);

	@Test
	@DisplayName("POSTS-13 posts are read from classpath:posts/ by default, subfolders included")
	void defaultLocation() {
		new ApplicationContextRunner().withConfiguration(BEANSPROUT).run(context -> {
			assertThat(context).hasNotFailed();
			assertThat(context.getBean(Posts.class).published()).extracting(Post::title)
				.containsExactly("Deep in a subfolder", "Hello from the classpath");
			assertThat(context.getBean(Posts.class).find("deep")).get().extracting(Post::path).isEqualTo("2026/deep.md");
		});
	}

	@Test
	@DisplayName("POSTS-13 beansprout.posts.location can point at a folder on disk, and a missing one means no posts")
	void fileLocation(@TempDir Path dir) throws IOException {
		Files.writeString(dir.resolve("disk.md"), "---\ntitle: On disk\ndate: 2026-01-01\n---\n");

		new ApplicationContextRunner().withConfiguration(BEANSPROUT)
			.withPropertyValues("beansprout.posts.location=file:" + dir + "/")
			.run(context -> assertThat(context.getBean(Posts.class).published()).extracting(Post::title)
				.containsExactly("On disk"));
		new ApplicationContextRunner().withConfiguration(BEANSPROUT)
			.withPropertyValues("beansprout.posts.location=file:" + dir.resolve("missing") + "/")
			.run(context -> assertThat(context.getBean(Posts.class).published()).isEmpty());
	}

	@Test
	@DisplayName("POSTS-14 beansprout.posts.reload=true shows a new file on the next call, with no restart")
	void reloadFromDisk(@TempDir Path dir) throws IOException {
		Files.writeString(dir.resolve("one.md"), "---\ntitle: One\ndate: 2026-01-01\n---\n");

		new ApplicationContextRunner().withConfiguration(BEANSPROUT)
			.withPropertyValues("beansprout.posts.location=file:" + dir + "/", "beansprout.posts.reload=true")
			.run(context -> {
				Posts posts = context.getBean(Posts.class);
				assertThat(posts.published()).hasSize(1);
				Files.writeString(dir.resolve("two.md"), "---\ntitle: Two\ndate: 2026-01-02\n---\n");
				assertThat(posts.published()).extracting(Post::title).containsExactly("Two", "One");
			});
	}

	@Test
	@DisplayName("POSTS-15 beansprout.time-zone sets the zone a plain date starts in")
	void timeZone() {
		new ApplicationContextRunner().withConfiguration(BEANSPROUT)
			.withPropertyValues("beansprout.time-zone=Asia/Tokyo")
			.run(context -> assertThat(context.getBean(Posts.class).find("hello")).get()
				.extracting(post -> post.date().getZone().getId()).isEqualTo("Asia/Tokyo"));
	}

	@Test
	@DisplayName("PAGES-15 with beansprout.web.enabled=false there are no blog pages, and the posts service is still a bean")
	void webCanBeTurnedOff() {
		web.withUserConfiguration(FakeTheme.class).withPropertyValues("beansprout.web.enabled=false").run(context -> {
			assertThat(context).hasNotFailed();
			assertThat(context).doesNotHaveBean(BlogController.class).doesNotHaveBean(FeedController.class);
			assertThat(context).hasSingleBean(Posts.class).hasSingleBean(Pages.class);
		});
		web.withUserConfiguration(FakeTheme.class).run(context -> {
			assertThat(context).hasSingleBean(BlogController.class).hasSingleBean(FeedController.class);
		});
	}

	@Test
	@DisplayName("PAGES-16 with no theme on the classpath the app starts and has no blog pages")
	void noTheme() {
		web.run(context -> {
			assertThat(context).hasNotFailed();
			assertThat(context).doesNotHaveBean(ThemeRenderer.class).doesNotHaveBean(BlogController.class);
			assertThat(context).hasSingleBean(Posts.class);
		});
		web.withClassLoader(new FilteredClassLoader("gg.jte")).run(context -> {
			assertThat(context).hasNotFailed();
			assertThat(context).doesNotHaveBean(BlogController.class).hasSingleBean(Posts.class);
		});
	}

	@Test
	@DisplayName("PAGES-1 beansprout.page-size and beansprout.base-path shape the pages")
	void pageSettings() {
		new ApplicationContextRunner().withConfiguration(BEANSPROUT)
			.withPropertyValues("beansprout.page-size=1", "beansprout.base-path=/notes", "beansprout.title=Notes")
			.run(context -> {
				Pages pages = context.getBean(Pages.class);
				assertThat(pages.index(1).orElseThrow().latest().older()).isEqualTo("/notes/page/2");
				assertThat(pages.index(1).orElseThrow().site().name()).isEqualTo("Notes");
			});
	}

	@Test
	@DisplayName("PAGES-13 with nothing set, the blog is called Blog and lives at /blog with 10 posts a page")
	void defaults() {
		new ApplicationContextRunner().withConfiguration(BEANSPROUT).run(context -> {
			Pages pages = context.getBean(Pages.class);
			assertThat(pages.settings().title()).isEqualTo("Blog");
			assertThat(pages.settings().pageSize()).isEqualTo(10);
			assertThat(pages.links().home()).isEqualTo("/blog");
		});
	}

	@Test
	@DisplayName("PAGES-1 a base path with a trailing slash fails at startup with a clear message")
	void badBasePath() {
		new ApplicationContextRunner().withConfiguration(BEANSPROUT)
			.withPropertyValues("beansprout.base-path=/blog/")
			.run(context -> assertThat(context).getFailure().rootCause()
				.hasMessageContaining("beansprout.base-path must look like /blog"));
	}

	@Configuration(proxyBeanMethods = false)
	static class FakeTheme {

		@Bean
		ThemeRenderer themeRenderer() {
			return (template, page) -> "<p>" + template + "</p>";
		}

	}

}
