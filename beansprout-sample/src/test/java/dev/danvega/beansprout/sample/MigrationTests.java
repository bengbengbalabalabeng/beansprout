package dev.danvega.beansprout.sample;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

import dev.danvega.beansprout.content.Post;
import dev.danvega.beansprout.content.Posts;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/** A folder of posts moved from Jekyll or Hugo, as they are. The migration guide's claims rest on these. */
class MigrationTests {

	private final HttpClient http = HttpClient.newHttpClient();

	private ConfigurableApplicationContext start(String folder) {
		return new SpringApplicationBuilder(SampleApplication.class).run("--server.port=0",
				"--beansprout.posts.location=classpath:" + folder + "/", "--beansprout.site-url=https://example.com");
	}

	private String get(ConfigurableApplicationContext app, String path) throws IOException, InterruptedException {
		String url = "http://localhost:" + app.getEnvironment().getProperty("local.server.port") + path;
		HttpResponse<String> response = http.send(HttpRequest.newBuilder(URI.create(url)).build(),
				HttpResponse.BodyHandlers.ofString());
		assertThat(response.statusCode()).as(path).isEqualTo(200);
		return response.body();
	}

	/** Each post's address and title, and that every one is served and on the index. */
	private void publishesEvery(ConfigurableApplicationContext app, Map<String, String> titles) throws Exception {
		Posts posts = app.getBean(Posts.class);
		assertThat(posts.problems()).isEmpty();
		assertThat(posts.published()).extracting(Post::slug).containsExactlyInAnyOrderElementsOf(titles.keySet());
		String index = get(app, "/blog");
		for (Map.Entry<String, String> post : titles.entrySet()) {
			assertThat(get(app, "/blog/" + post.getKey())).contains(post.getValue());
			assertThat(index).contains("/blog/" + post.getKey());
		}
	}

	@Test
	@DisplayName("FM-21 a folder of Jekyll-shaped posts is published, every one of them")
	void jekyll() throws Exception {
		try (ConfigurableApplicationContext app = start("test-posts-jekyll")) {
			publishesEvery(app, Map.of("first-post", "My first Jekyll post", "nested-image", "A picture with a path",
					"road-trip", "Road trip"));

			Posts posts = app.getBean(Posts.class);
			Post first = posts.find("first-post").orElseThrow();
			assertThat(first.date().toLocalDate()).hasToString("2024-05-01");
			assertThat(first.lede()).isEqualTo("Moving a blog without losing a thing.");
			assertThat(first.image()).isEqualTo("/images/first.png");
			assertThat(posts.find("nested-image").orElseThrow().image()).isEmpty();
			assertThat(posts.find("nested-image").orElseThrow().canonical()).isEqualTo("https://dev.to/jane/nested-image");
			assertThat(posts.find("road-trip").orElseThrow().date().toLocalDate()).hasToString("2024-07-20");
		}
	}

	@Test
	@DisplayName("FM-22 a folder of Hugo-shaped posts is published, every one of them")
	void hugo() throws Exception {
		try (ConfigurableApplicationContext app = start("test-posts-hugo")) {
			publishesEvery(app, Map.of("first-hugo-post", "My first Hugo post", "hugo-notes", "Notes on Hugo",
					"yaml-in-hugo", "A Hugo post in YAML"));

			Posts posts = app.getBean(Posts.class);
			Post first = posts.find("first-hugo-post").orElseThrow();
			assertThat(first.tags()).containsExactly("hugo", "java");
			assertThat(first.lede()).isEqualTo("Moving a Hugo blog.");
			assertThat(first.modified().toInstant()).hasToString("2024-06-01T14:00:00Z");
			assertThat(posts.find("yaml-in-hugo").orElseThrow().modified().toLocalDate()).hasToString("2024-07-02");
		}
	}

}
