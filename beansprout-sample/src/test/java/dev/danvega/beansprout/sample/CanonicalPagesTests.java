package dev.danvega.beansprout.sample;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/** A post first published elsewhere, rendered by every theme. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CanonicalPagesTests {

	private static final String DEV_TO = "https://dev.to/jane/testcontainers-without-the-waiting";

	private static final List<String> THEMES = List.of("longhand", "dateline", "linocut", "marginalia", "small-machines",
			"plainsong", "onionskin", "calling-card");

	private final HttpClient http = HttpClient.newHttpClient();

	private String get(ConfigurableApplicationContext app, String path) throws IOException, InterruptedException {
		String url = "http://localhost:" + app.getEnvironment().getProperty("local.server.port") + path;
		HttpResponse<String> response = http.send(HttpRequest.newBuilder(URI.create(url)).build(),
				HttpResponse.BodyHandlers.ofString());
		assertThat(response.statusCode()).as(path).isEqualTo(200);
		return response.body();
	}

	@Test
	@DisplayName("CANON-1 CANON-2 every theme writes a post's canonical as its canonical link and og:url")
	void everyThemeWritesTheCanonical() throws Exception {
		for (String theme : THEMES) {
			ConfigurableApplicationContext app = new SpringApplicationBuilder(SampleApplication.class).run("--server.port=0",
					"--beansprout.theme=" + theme, "--beansprout.posts.location=classpath:test-posts-canonical/",
					"--beansprout.site-url=https://example.com");
			try {
				String moved = get(app, "/blog/moved");
				assertThat(moved).as(theme).contains("<link rel=\"canonical\" href=\"" + DEV_TO + "\">",
						"<meta property=\"og:url\" content=\"" + DEV_TO + "\">");
				String home = get(app, "/blog/home");
				assertThat(home).as(theme).contains("<link rel=\"canonical\" href=\"https://example.com/blog/home\">");
			}
			finally {
				app.close();
			}
		}
	}

	@AfterAll
	void stop() {
		http.close();
	}

	@Test
	@DisplayName("CANON-9 Small Machines labels a canonical post with its own slug")
	void smallMachinesLabel() throws Exception {
		ConfigurableApplicationContext app = new SpringApplicationBuilder(SampleApplication.class).run("--server.port=0",
				"--beansprout.theme=small-machines", "--beansprout.posts.location=classpath:test-posts-canonical/",
				"--beansprout.site-url=https://example.com");
		try {
			String moved = get(app, "/blog/moved");
			assertThat(moved).contains("moved.post").doesNotContain("testcontainers-without-the-waiting.post");
			String renamed = get(app, "/blog/tc-waiting");
			assertThat(renamed).contains("tc-waiting.post").doesNotContain("draft-name.post", "waiting-less-for-containers.post");
		}
		finally {
			app.close();
		}
	}

	@Test
	@DisplayName("CANON-4 the served sitemap leaves out a post whose canonical points elsewhere")
	void sitemapLeavesItOut() throws Exception {
		ConfigurableApplicationContext app = new SpringApplicationBuilder(SampleApplication.class).run("--server.port=0",
				"--beansprout.posts.location=classpath:test-posts-canonical/", "--beansprout.site-url=https://example.com");
		try {
			String sitemap = get(app, "/blog/sitemap.xml");
			assertThat(sitemap).contains("https://example.com/blog/home").doesNotContain("/blog/moved");
		}
		finally {
			app.close();
		}
	}

}
