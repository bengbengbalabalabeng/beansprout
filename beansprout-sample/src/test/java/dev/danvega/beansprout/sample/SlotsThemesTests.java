package dev.danvega.beansprout.sample;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
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

import dev.danvega.beansprout.theme.Theme;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.DefaultResourceLoader;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The app's slot files in every theme, from specs/theme-slots.md. The files sit in a folder that a
 * class loader of the test's own puts on the classpath, so no other test sees them.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SlotsThemesTests {

	private static final String HEAD = "<script>window.beanStats = \"a & b\";</script>";

	private static final String AFTER_POST = "<aside class=\"signup\">New posts by <b>email</b> & more.</aside>";

	private static final String FOOTER = "<p class=\"owner\">Hosted by <a href=\"https://example.org/me\">me</a></p>";

	/** Every kind of page, with the status each answers. */
	private static final Map<String, Integer> PAGES = Map.of("/blog", 200, "/blog/long", 200, "/blog/tags/java", 200,
			"/blog/archive", 200, "/blog/no-such-post", 404);

	private final HttpClient http = HttpClient.newHttpClient();

	private final Map<String, ConfigurableApplicationContext> apps = new HashMap<>();

	private Path slots;

	@BeforeAll
	void slotFiles() throws IOException {
		slots = Files.createTempDirectory("beansprout-slots");
		Files.createDirectories(slots.resolve("beansprout"));
		Files.writeString(slots.resolve("beansprout/head.html"), HEAD, StandardCharsets.UTF_8);
		Files.writeString(slots.resolve("beansprout/after-post.html"), AFTER_POST, StandardCharsets.UTF_8);
		Files.writeString(slots.resolve("beansprout/footer.html"), FOOTER, StandardCharsets.UTF_8);
	}

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

	private SpringApplicationBuilder builder(boolean withSlots) {
		SpringApplicationBuilder builder = new SpringApplicationBuilder(SampleApplication.class);
		if (withSlots) {
			try {
				URLClassLoader loader = new URLClassLoader(new URL[] { slots.toUri().toURL() }, getClass().getClassLoader());
				builder.resourceLoader(new DefaultResourceLoader(loader));
			}
			catch (IOException ex) {
				throw new IllegalStateException(ex);
			}
		}
		return builder;
	}

	private String page(String theme, boolean withSlots, String path) throws IOException, InterruptedException {
		ConfigurableApplicationContext app = apps.computeIfAbsent(theme + withSlots,
				key -> builder(withSlots).run("--server.port=0", "--beansprout.theme=" + theme,
						"--beansprout.site-url=https://example.com", "--beansprout.posts.location=classpath:test-posts-toc/"));
		String url = "http://localhost:" + app.getEnvironment().getProperty("local.server.port") + path;
		HttpResponse<String> response = http.send(HttpRequest.newBuilder(URI.create(url)).build(),
				HttpResponse.BodyHandlers.ofString());
		assertThat(response.statusCode()).as(theme + " " + path).isEqualTo(PAGES.getOrDefault(path, 200));
		return response.body();
	}

	private String page(String theme, String path) throws IOException, InterruptedException {
		return page(theme, true, path);
	}

	private static void inFooter(String html, String where) {
		int open = html.lastIndexOf("<footer");
		int slot = html.indexOf(FOOTER);
		assertThat(open).as(where + " has a footer").isPositive();
		assertThat(slot).as(where + " footer slot inside the footer").isGreaterThan(open)
			.isLessThan(html.indexOf("</footer>", open));
	}

	private static void endOfHead(String html, String where) {
		int slot = html.indexOf(HEAD);
		assertThat(slot).as(where + " head slot").isPositive();
		assertThat(html.substring(slot + HEAD.length(), html.indexOf("</head>"))).as(where + " nothing after it in the head")
			.isBlank();
	}

	@Test
	@DisplayName("SLOT-1 every theme puts beansprout/head.html at the end of every page's head")
	void head() throws Exception {
		for (String theme : themes()) {
			for (String path : PAGES.keySet()) {
				endOfHead(page(theme, path), theme + " " + path);
			}
		}
	}

	@Test
	@DisplayName("SLOT-2 every theme puts beansprout/after-post.html after the body of every post, and on no other page")
	void afterPost() throws Exception {
		for (String theme : themes()) {
			String post = page(theme, "/blog/long");
			assertThat(post.indexOf(AFTER_POST)).as(theme + " after the body").isGreaterThan(post.indexOf("The end."));
			assertThat(post.indexOf(AFTER_POST)).as(theme + " once").isEqualTo(post.lastIndexOf(AFTER_POST));
			assertThat(page(theme, "/blog/short")).as(theme + " a second post").contains(AFTER_POST);
			for (String path : List.of("/blog", "/blog/tags/java", "/blog/archive", "/blog/no-such-post")) {
				assertThat(page(theme, path)).as(theme + " " + path).doesNotContain(AFTER_POST);
			}
		}
	}

	@Test
	@DisplayName("SLOT-3 every theme puts beansprout/footer.html in every page's footer")
	void footer() throws Exception {
		for (String theme : themes()) {
			for (String path : PAGES.keySet()) {
				inFooter(page(theme, path), theme + " " + path);
			}
		}
	}

	@Test
	@DisplayName("SLOT-4 every theme writes each slot as it is, without escaping it")
	void notEscaped() throws Exception {
		for (String theme : themes()) {
			String post = page(theme, "/blog/long");
			assertThat(post).as(theme).contains(HEAD, AFTER_POST, FOOTER);
			assertThat(post).as(theme).doesNotContain("&quot;a &amp; b&quot;", "&lt;aside", "&lt;p class=&quot;owner");
		}
	}

	@Test
	@DisplayName("SLOT-5 without slot files every theme renders nothing in their place")
	void nothingWithoutFiles() throws Exception {
		for (String theme : themes()) {
			for (String path : PAGES.keySet()) {
				String with = page(theme, true, path);
				String without = page(theme, false, path);
				assertThat(without).as(theme + " " + path).doesNotContain("beanStats", "class=\"signup\"", "class=\"owner\"");
				assertThat(with.replace(HEAD, "").replace(AFTER_POST, "").replace(FOOTER, "")).as(theme + " " + path)
					.isEqualTo(without);
			}
		}
	}

	@Test
	@DisplayName("SLOT-8 every theme puts the head and footer slots on the not-found page")
	void notFoundPage() throws Exception {
		for (String theme : themes()) {
			String html = page(theme, "/blog/no-such-post");
			endOfHead(html, theme + " not found");
			inFooter(html, theme + " not found");
		}
	}

	@Test
	@DisplayName("SLOT-9 the static export writes the slots into every exported page, as the app serves them")
	void export() throws IOException {
		Path out = Files.createTempDirectory("beansprout-export").resolve("site");
		builder(true).run("--beansprout.export.enabled=true", "--beansprout.export.output=" + out,
				"--beansprout.site-url=https://example.com", "--beansprout.posts.location=classpath:test-posts-toc/")
			.close();

		for (String file : List.of("blog.html", "blog/long.html", "blog/short.html", "blog/tags/java.html",
				"blog/archive.html", "404.html")) {
			String html = Files.readString(out.resolve(file), StandardCharsets.UTF_8);
			endOfHead(html, file);
			inFooter(html, file);
		}
		assertThat(Files.readString(out.resolve("blog/long.html"), StandardCharsets.UTF_8)).contains(AFTER_POST);
		assertThat(Files.readString(out.resolve("blog.html"), StandardCharsets.UTF_8)).doesNotContain(AFTER_POST);
	}

}
