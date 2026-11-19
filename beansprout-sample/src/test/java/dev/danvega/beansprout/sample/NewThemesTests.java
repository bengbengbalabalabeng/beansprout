package dev.danvega.beansprout.sample;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import dev.danvega.beansprout.theme.Theme;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/** What sets plainsong, onionskin and calling-card apart. The shared rules are in ThemesTests. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NewThemesTests {

	private static final String TITLE = "Rich &amp; &lt;strong&gt;post&lt;/strong&gt;";

	private final HttpClient http = HttpClient.newHttpClient();

	private final Map<String, ConfigurableApplicationContext> apps = new HashMap<>();

	@AfterAll
	void stop() {
		apps.values().forEach(ConfigurableApplicationContext::close);
		http.close();
	}

	/** The index page with one theme, after its head: what a reader sees. */
	private String index(String theme) throws IOException, InterruptedException {
		ConfigurableApplicationContext app = apps.computeIfAbsent(theme,
				name -> new SpringApplicationBuilder(SampleApplication.class).run("--server.port=0",
						"--beansprout.theme=" + name, "--beansprout.posts.location=classpath:test-posts-themes/"));
		String url = "http://localhost:" + app.getEnvironment().getProperty("local.server.port") + "/blog";
		HttpResponse<String> response = http.send(HttpRequest.newBuilder(URI.create(url)).build(),
				HttpResponse.BodyHandlers.ofString());
		assertThat(response.statusCode()).as(theme + " /blog").isEqualTo(200);
		String html = response.body();
		return html.substring(html.indexOf("</head>"));
	}

	private static int count(String text, String part) {
		return text.split(Pattern.quote(part), -1).length - 1;
	}

	@Test
	@DisplayName("THEME-20 the themes plainsong, onionskin and calling-card are provided")
	void threeNewThemes() {
		Set<String> names = new TreeSet<>();
		ServiceLoader.load(Theme.class).forEach(theme -> names.add(theme.name()));
		assertThat(names).contains("plainsong", "onionskin", "calling-card");
	}

	@Test
	@DisplayName("THEME-21 the plainsong index lists each post as its title, date and summary, with no picture")
	void plainsongList() throws Exception {
		String page = index("plainsong");
		assertThat(page).contains(TITLE, "Plain post", "Sum &lt;i&gt;mary&lt;/i&gt; &amp; co.", "10 Nov");
		assertThat(page).doesNotContain("<img");
	}

	@Test
	@DisplayName("THEME-22 the onionskin index shows each post as a memo with Date, Subject, Filed and Length lines")
	void onionskinMemos() throws Exception {
		String page = index("onionskin");
		for (String line : new String[] { "Date:", "Subject:", "Filed:", "Length:" }) {
			assertThat(count(page, line)).as(line).isEqualTo(2);
		}
		assertThat(page).contains(TITLE, "Plain post", "2026-11-10");
	}

	@Test
	@DisplayName("THEME-23 the calling-card index shows every tag with its post count on its first page")
	void callingCardTopics() throws Exception {
		String page = index("calling-card");
		assertThat(page).containsPattern(topic("java", "/blog/tags/java", 2));
		assertThat(page).containsPattern(topic("a&amp;b", "/blog/tags/a-b", 1));
	}

	/** A link to the tag whose text holds its name and then its count, with any markup between. */
	private static Pattern topic(String name, String href, int count) {
		return Pattern.compile("href=\"" + Pattern.quote(href) + "\"[^>]*>(?:(?!</a>).)*?" + Pattern.quote(name)
				+ "(?:(?!</a>).)*?\\b" + count + "\\b(?:(?!</a>).)*?</a>", Pattern.DOTALL);
	}

}
