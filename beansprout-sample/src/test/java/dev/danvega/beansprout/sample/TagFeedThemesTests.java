package dev.danvega.beansprout.sample;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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

/** Each theme's tag pages link the tag's feed, and the export writes it, from specs/tag-feeds.md. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TagFeedThemesTests {

	private static final Pattern ATOM_LINK = Pattern.compile("<link\\b[^>]*type=\"application/atom\\+xml\"[^>]*>");

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

	/** One page with one theme, as served. */
	private String page(String theme, String path) throws IOException, InterruptedException {
		ConfigurableApplicationContext app = apps.computeIfAbsent(theme,
				name -> new SpringApplicationBuilder(SampleApplication.class).run("--server.port=0",
						"--beansprout.theme=" + name, "--beansprout.posts.location=classpath:test-posts-themes/"));
		String url = "http://localhost:" + app.getEnvironment().getProperty("local.server.port") + path;
		HttpResponse<String> response = http.send(HttpRequest.newBuilder(URI.create(url)).build(),
				HttpResponse.BodyHandlers.ofString());
		assertThat(response.statusCode()).as(theme + " " + path).isEqualTo(200);
		return response.body();
	}

	/** The head's Atom links, by their href. */
	private static Map<String, String> atomLinks(String html) {
		Map<String, String> links = new HashMap<>();
		String head = html.substring(0, html.indexOf("</head>"));
		Matcher link = ATOM_LINK.matcher(head);
		while (link.find()) {
			Matcher href = Pattern.compile("href=\"([^\"]*)\"").matcher(link.group());
			assertThat(href.find()).as(link.group()).isTrue();
			links.put(href.group(1), link.group());
		}
		return links;
	}

	@Test
	@DisplayName("TAGFEED-7 every theme links each tag page to the tag's feed")
	void tagPageLinksItsFeed() throws Exception {
		for (String theme : themes()) {
			for (String tag : List.of("java", "a-b")) {
				Map<String, String> links = atomLinks(page(theme, "/blog/tags/" + tag));
				assertThat(links).as(theme + " " + tag).containsKey("/blog/tags/" + tag + "/feed.xml");
				assertThat(links.get("/blog/tags/" + tag + "/feed.xml")).as(theme + " " + tag).contains("rel=\"alternate\"");
			}
			assertThat(atomLinks(page(theme, "/blog"))).as(theme + " index").containsOnlyKeys("/blog/feed.xml");
			assertThat(atomLinks(page(theme, "/blog/plain"))).as(theme + " post").containsOnlyKeys("/blog/feed.xml");
		}
	}

	@Test
	@DisplayName("TAGFEED-8 every theme keeps each tag page's link to the main feed")
	void tagPageKeepsMainFeed() throws Exception {
		for (String theme : themes()) {
			Map<String, String> links = atomLinks(page(theme, "/blog/tags/java"));
			assertThat(links).as(theme).containsKey("/blog/feed.xml");
			assertThat(links.get("/blog/feed.xml")).as(theme).contains("rel=\"alternate\"");
		}
	}

	@Test
	@DisplayName("TAGFEED-9 the static export writes each tag's feed to blog/tags/{tag}/feed.xml")
	void export() throws IOException {
		Path out = Files.createTempDirectory("beansprout-export").resolve("site");
		StaticExportTests.export(out, "--beansprout.posts.location=classpath:test-posts-tagfeeds/").close();

		Set<String> files = StaticExportTests.files(out);
		assertThat(files).contains("blog/tags/java/feed.xml", "blog/tags/spring-boot/feed.xml", "blog/tags/kotlin/feed.xml")
			.doesNotContain("blog/tags/secret/feed.xml", "blog/tags/later/feed.xml");
		assertThat(Files.readString(out.resolve("blog/tags/java/feed.xml"), StandardCharsets.UTF_8))
			.contains("<link rel=\"self\" href=\"https://example.com/blog/tags/java/feed.xml\"/>")
			.contains("<title>Beans on the JVM</title>", "<title>More Java</title>")
			.doesNotContain("<title>Kotlin notes</title>");
	}

}
