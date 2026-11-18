package dev.danvega.beansprout.sample;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/** A restart changes every ETag, since the theme, the templates or the code may have changed. */
class CachingRestartTests {

	private static final List<String> RESPONSES = List.of("/blog", "/blog/hello", "/blog/feed.xml",
			"/blog/sitemap.xml", "/robots.txt");

	private static Map<String, String> run() throws IOException, InterruptedException {
		Map<String, String> etags = new LinkedHashMap<>();
		try (ConfigurableApplicationContext app = new SpringApplicationBuilder(SampleApplication.class)
			.run("--server.port=0", "--beansprout.posts.location=classpath:test-posts/",
					"--beansprout.site-url=https://example.com");
				HttpClient http = HttpClient.newHttpClient()) {
			String port = app.getEnvironment().getRequiredProperty("local.server.port");
			for (String path : RESPONSES) {
				HttpRequest request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path)).GET().build();
				HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
				assertThat(response.statusCode()).as(path).isEqualTo(200);
				etags.put(path, response.headers().firstValue("ETag").orElse(null));
			}
		}
		return etags;
	}

	@Test
	@DisplayName("CACHE-8 when the app restarts, every blog response gets a new ETag")
	void restartChangesEtags() throws IOException, InterruptedException {
		Map<String, String> first = run();
		Map<String, String> second = run();
		for (String path : RESPONSES) {
			assertThat(first.get(path)).as(path).isNotNull();
			assertThat(second.get(path)).as(path).isNotEqualTo(first.get(path));
		}
	}

}
