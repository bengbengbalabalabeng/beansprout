package dev.danvega.beansprout.sample;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.TreeSet;

import dev.danvega.beansprout.theme.Theme;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/** Unlisted posts as each theme serves them, from specs/unlisted.md. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UnlistedThemesTests {

	private static final String NOINDEX = "<meta name=\"robots\" content=\"noindex\">";

	private final HttpClient http = HttpClient.newHttpClient();

	private final Map<String, ConfigurableApplicationContext> apps = new HashMap<>();

	@AfterAll
	void stop() {
		apps.values().forEach(ConfigurableApplicationContext::close);
		http.close();
	}

	private static Set<String> themes() {
		Set<String> names = new TreeSet<>();
		ServiceLoader.load(Theme.class).forEach(theme -> names.add(theme.name()));
		assertThat(names).hasSize(8);
		return names;
	}

	private HttpResponse<String> get(String theme, String path) throws IOException, InterruptedException {
		ConfigurableApplicationContext app = apps.computeIfAbsent(theme,
				key -> new SpringApplicationBuilder(SampleApplication.class).run("--server.port=0",
						"--beansprout.theme=" + theme, "--beansprout.site-url=https://example.com",
						"--beansprout.posts.location=classpath:test-posts-unlisted/"));
		String url = "http://localhost:" + app.getEnvironment().getProperty("local.server.port") + path;
		return http.send(HttpRequest.newBuilder(URI.create(url)).build(), HttpResponse.BodyHandlers.ofString());
	}

	private String page(String theme, String path) throws IOException, InterruptedException {
		HttpResponse<String> response = get(theme, path);
		assertThat(response.statusCode()).as(theme + " " + path).isEqualTo(200);
		return response.body();
	}

	private static String head(String html) {
		return html.substring(0, html.indexOf("</head>"));
	}

	@Test
	@DisplayName("UNLISTED-1 every theme serves an unlisted post at its address with 200, and its alias leads there")
	void servedAtItsAddress() throws Exception {
		for (String theme : themes()) {
			assertThat(page(theme, "/blog/keynote")).as(theme).contains("The body of the keynote notes.")
				.contains("Notes for the Porto keynote");
			HttpResponse<String> alias = get(theme, "/old/keynote");
			assertThat(alias.statusCode()).as(theme + " alias").isEqualTo(301);
			assertThat(alias.headers().firstValue("Location")).as(theme + " alias").contains("/blog/keynote");
		}
	}

	@Test
	@DisplayName("UNLISTED-9 in every theme, the blog's other pages have no link to an unlisted post")
	void noLinksToIt() throws Exception {
		for (String theme : themes()) {
			for (String path : List.of("/blog", "/blog/hello", "/blog/older", "/blog/archive", "/blog/tags/java")) {
				assertThat(page(theme, path)).as(theme + " " + path).doesNotContain("/blog/keynote")
					.doesNotContain("Porto keynote");
			}
			assertThat(get(theme, "/blog/tags/porto").statusCode()).as(theme + " a tag only it uses").isEqualTo(404);
		}
	}

	@Test
	@DisplayName("UNLISTED-15 every theme renders a page's robots value as a robots meta tag in its head, and no tag without one")
	void robotsMeta() throws Exception {
		for (String theme : themes()) {
			String keynote = page(theme, "/blog/keynote");
			assertThat(head(keynote)).as(theme).contains(NOINDEX);
			assertThat(keynote.split(java.util.regex.Pattern.quote("name=\"robots\""), -1)).as(theme + " once").hasSize(2);
			for (String path : List.of("/blog", "/blog/hello", "/blog/archive", "/blog/tags/java")) {
				assertThat(page(theme, path)).as(theme + " " + path).doesNotContain("name=\"robots\"");
			}
		}
	}

	@Test
	@DisplayName("UNLISTED-18 no theme serves an unlisted post that is a draft or dated in the future")
	void notWhileHidden() throws Exception {
		for (String theme : themes()) {
			assertThat(get(theme, "/blog/secret").statusCode()).as(theme + " draft").isEqualTo(404);
			assertThat(get(theme, "/blog/later").statusCode()).as(theme + " later").isEqualTo(404);
		}
	}

}
