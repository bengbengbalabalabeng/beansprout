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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import dev.danvega.beansprout.theme.Theme;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Each theme shows a post's related posts after it, from specs/related-posts.md. Every page is
 * compared with the same page from an app that sets {@code beansprout.related-posts=0}.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RelatedPostsThemesTests {

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

	/** One page with one theme, as served, with up to {@code related} related posts. */
	private String page(String theme, int related, String path) throws IOException, InterruptedException {
		ConfigurableApplicationContext app = apps.computeIfAbsent(theme + related,
				key -> new SpringApplicationBuilder(SampleApplication.class).run("--server.port=0",
						"--beansprout.theme=" + theme, "--beansprout.related-posts=" + related,
						"--beansprout.site-url=https://example.com",
						"--beansprout.posts.location=classpath:test-posts-related/"));
		String url = "http://localhost:" + app.getEnvironment().getProperty("local.server.port") + path;
		HttpResponse<String> response = http.send(HttpRequest.newBuilder(URI.create(url)).build(),
				HttpResponse.BodyHandlers.ofString());
		assertThat(response.statusCode()).as(theme + " " + path).isEqualTo(200);
		return response.body();
	}

	/** Every link to a post, as its start in the page and its text with the tags taken out. */
	private static List<int[]> links(String html, String href) {
		Matcher link = Pattern.compile("<a\\b[^>]*href=\"" + Pattern.quote(href) + "\"[^>]*>").matcher(html);
		return link.results().map(m -> new int[] { m.start(), m.end() }).toList();
	}

	private static String text(String html, int[] link) {
		int end = html.indexOf("</a>", link[1]);
		return html.substring(link[1], end).replaceAll("<[^>]*>", "").replaceAll("\\s+", " ").strip();
	}

	@Test
	@DisplayName("RELATED-10 every theme shows a post's related posts after the post, each as a link with its title")
	void shownAfterThePost() throws Exception {
		for (String theme : themes()) {
			String on = page(theme, 3, "/blog/jvm");
			String off = page(theme, 0, "/blog/jvm");
			int body = on.indexOf("The body of the JVM post.");
			assertThat(body).as(theme).isPositive();

			assertThat(links(off, "/blog/boot")).as(theme + " off").isEmpty();
			assertThat(links(off, "/blog/more-java")).as(theme + " off").isEmpty();
			List<int[]> boot = links(on, "/blog/boot");
			List<int[]> more = links(on, "/blog/more-java");
			assertThat(boot).as(theme).hasSize(1);
			assertThat(more).as(theme).hasSize(1);
			assertThat(text(on, boot.getFirst())).as(theme).contains("Boot &amp; Beans");
			assertThat(text(on, more.getFirst())).as(theme).contains("More Java");
			assertThat(boot.getFirst()[0]).as(theme + " after the post").isGreaterThan(body);
			assertThat(more.getFirst()[0]).as(theme + " most shared first").isGreaterThan(boot.getFirst()[0]);
			assertThat(on).as(theme).doesNotContain("href=\"/blog/draft\"").doesNotContain("Secret draft");
		}
	}

	@Test
	@DisplayName("RELATED-11 every theme shows nothing in their place when a post has no related posts")
	void nothingWhenNone() throws Exception {
		for (String theme : themes()) {
			for (String path : List.of("/blog/kotlin", "/blog/loose")) {
				assertThat(page(theme, 3, path)).as(theme + " " + path).isEqualTo(page(theme, 0, path));
			}
		}
	}

}
