package dev.danvega.beansprout.sample;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Every theme, in the real sample app, with the same posts: one with every field and hostile
 * values, one plain. Each theme starts once and serves every test for it.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ThemesTests {

	private static final List<String> THEMES = List.of("longhand", "dateline", "linocut", "marginalia", "small-machines", "plainsong", "onionskin", "calling-card");

	private static final Pattern LINK = Pattern.compile("\\s(?:href|src)=\"([^\"]*)\"");

	private static final Pattern ROOT_BLOCK = Pattern.compile(":root\\s*\\{([^}]*)}");

	private static final Pattern THEME_COLOR = Pattern.compile("--theme-(background|text|heading|muted|link)\\s*:\\s*#([0-9a-fA-F]{6})\\s*;");

	private final HttpClient http = HttpClient.newHttpClient();

	private final Map<String, ConfigurableApplicationContext> apps = new HashMap<>();

	private String base(String theme) {
		ConfigurableApplicationContext app = apps.computeIfAbsent(theme,
				// Arguments, not default properties: application.yaml names a theme, and defaults rank below it.
				name -> new SpringApplicationBuilder(SampleApplication.class).run("--server.port=0",
						"--beansprout.theme=" + name, "--beansprout.posts.location=classpath:test-posts-themes/",
						// One post a page, so every theme draws its pager and the link check follows it.
						"--beansprout.page-size=1"));
		return "http://localhost:" + app.getEnvironment().getProperty("local.server.port");
	}

	@AfterAll
	void stop() {
		apps.values().forEach(ConfigurableApplicationContext::close);
	}

	private HttpResponse<String> get(String url) throws IOException, InterruptedException {
		return http.send(HttpRequest.newBuilder(URI.create(url)).build(), HttpResponse.BodyHandlers.ofString());
	}

	private String page(String theme, String path) throws IOException, InterruptedException {
		HttpResponse<String> response = get(base(theme) + path);
		assertThat(response.statusCode()).as(theme + " " + path).isEqualTo(path.equals("/blog/no-such-post") ? 404 : 200);
		return response.body();
	}

	private static final List<String> PAGES = List.of("/blog", "/blog/page/2", "/blog/rich", "/blog/tags/java",
			"/blog/tags/java/page/2", "/blog/archive", "/blog/no-such-post");

	private static final String TITLE = "Rich &amp; &lt;strong&gt;post&lt;/strong&gt;";

	/** What a reader sees: the page after its head. */
	private static String body(String html) {
		return html.substring(html.indexOf("</head>"));
	}

	@Test
	@DisplayName("THEME-6 the themes longhand, dateline, linocut, marginalia and small-machines are provided")
	void allFiveThemes() {
		Set<String> names = new TreeSet<>();
		ServiceLoader.load(Theme.class).forEach(theme -> names.add(theme.name()));
		assertThat(names).containsExactlyInAnyOrderElementsOf(THEMES);
	}

	@ParameterizedTest(name = "{0}")
	@ValueSource(strings = { "longhand", "dateline", "linocut", "marginalia", "small-machines", "plainsong", "onionskin", "calling-card" })
	@DisplayName("THEME-5 each theme's stylesheet is served at /beansprout/<theme>/, and its pages link only their own")
	void ownStylesheet(String theme) throws Exception {
		HttpResponse<String> css = get(base(theme) + "/beansprout/" + theme + "/" + theme + ".css");
		assertThat(css.statusCode()).isEqualTo(200);
		assertThat(css.headers().firstValue("Content-Type")).get().asString().startsWith("text/css");
		String html = page(theme, "/blog");
		assertThat(html).contains("/beansprout/" + theme + "/" + theme + ".css");
		for (String other : THEMES) {
			if (!other.equals(theme)) {
				assertThat(html).doesNotContain("/beansprout/" + other + "/");
			}
		}
	}

	@ParameterizedTest(name = "{0}")
	@ValueSource(strings = { "longhand", "dateline", "linocut", "marginalia", "small-machines", "plainsong", "onionskin", "calling-card" })
	@DisplayName("THEME-7 each theme renders the index, post, tag, archive and not-found pages from the same posts")
	void everyPage(String theme) throws Exception {
		assertThat(body(page(theme, "/blog"))).contains(TITLE);
		assertThat(body(page(theme, "/blog/page/2"))).contains("Plain post");
		assertThat(body(page(theme, "/blog/rich"))).contains("A post with every field a theme shows.");
		assertThat(body(page(theme, "/blog/tags/java"))).contains(TITLE);
		assertThat(body(page(theme, "/blog/tags/java/page/2"))).contains("Plain post");
		assertThat(body(page(theme, "/blog/archive"))).contains(TITLE, "Plain post");
		// The not-found page points the reader on: the newest posts, or the address that was asked for.
		assertThat(body(page(theme, "/blog/no-such-post"))).containsAnyOf(TITLE, "no-such-post");
	}

	@ParameterizedTest(name = "{0}")
	@ValueSource(strings = { "longhand", "dateline", "linocut", "marginalia", "small-machines", "plainsong", "onionskin", "calling-card" })
	@DisplayName("THEME-8 each theme links only to addresses the app answers with 200")
	void linksWork(String theme) throws Exception {
		String base = base(theme);
		Set<String> links = new LinkedHashSet<>();
		for (String path : PAGES) {
			Matcher matcher = LINK.matcher(page(theme, path));
			while (matcher.find()) {
				String link = matcher.group(1).replace("&amp;", "&");
				int hash = link.indexOf('#');
				link = hash >= 0 ? link.substring(0, hash) : link;
				if (link.startsWith(base)) {
					links.add(link.substring(base.length()));
				}
				else if (link.startsWith("/") && !link.startsWith("//")) {
					links.add(link);
				}
				else if (!link.isEmpty()) {
					assertThat(link).as(theme + " links only inside the app").startsWith("http://localhost");
				}
			}
		}
		assertThat(links).isNotEmpty();
		for (String link : links) {
			assertThat(get(base + link).statusCode()).as(theme + " " + link).isEqualTo(200);
		}
	}

	@ParameterizedTest(name = "{0}")
	@ValueSource(strings = { "longhand", "dateline", "linocut", "marginalia", "small-machines", "plainsong", "onionskin", "calling-card" })
	@DisplayName("THEME-9 each theme puts the title, description, canonical link, Open Graph tags and JSON-LD in its head")
	void head(String theme) throws Exception {
		for (String path : PAGES) {
			String head = page(theme, path).split("</head>")[0];
			assertThat(head).as(theme + " " + path)
				.containsPattern("<title>[^<\\s][^<]*</title>")
				.containsPattern("name=\"description\" content=\"[^\"]+\"")
				.containsPattern("property=\"og:title\" content=\"[^\"]+\"")
				.contains("property=\"og:type\"");
			if (!path.equals("/blog/no-such-post")) {
				assertThat(head).as(theme + " " + path).containsPattern("rel=\"canonical\" href=\"http[^\"]+\"");
			}
		}
		// The post's own values, and the JSON-LD Beansprout writes for a post.
		assertThat(page(theme, "/blog/rich").split("</head>")[0]).containsPattern("<title>[^<]*" + Pattern.quote(TITLE))
			.contains("content=\"Sum &lt;i", "application/ld+json");
	}

	@ParameterizedTest(name = "{0}")
	@ValueSource(strings = { "longhand", "dateline", "linocut", "marginalia", "small-machines", "plainsong", "onionskin", "calling-card" })
	@DisplayName("THEME-10 each theme shows a post's title, date and tags")
	void titleDateTags(String theme) throws Exception {
		assertThat(body(page(theme, "/blog/rich"))).contains(TITLE, "10 November 2026", "a&amp;b", "/blog/tags/java");
	}

	@ParameterizedTest(name = "{0}")
	@ValueSource(strings = { "longhand", "dateline", "linocut", "marginalia", "small-machines", "plainsong", "onionskin", "calling-card" })
	@DisplayName("THEME-11 each theme shows a post's picture with its alt text")
	void picture(String theme) throws Exception {
		// Inside an attribute, a > may stay as it is. The & and < must be escaped.
		assertThat(page(theme, "/blog/rich")).contains("src=\"/images/beansprout.png\"", "alt=\"Alt &amp; &lt;b");
	}

	@ParameterizedTest(name = "{0}")
	@ValueSource(strings = { "longhand", "dateline", "linocut", "marginalia", "small-machines", "plainsong", "onionskin", "calling-card" })
	@DisplayName("THEME-12 each theme shows a post's caption with the picture")
	void caption(String theme) throws Exception {
		assertThat(page(theme, "/blog/rich")).contains("Cap &lt;u&gt;tion&lt;/u&gt; &amp; more");
	}

	@ParameterizedTest(name = "{0}")
	@ValueSource(strings = { "longhand", "dateline", "linocut", "marginalia", "small-machines", "plainsong", "onionskin", "calling-card" })
	@DisplayName("THEME-13 each theme shows a post's updated date")
	void updated(String theme) throws Exception {
		assertThat(page(theme, "/blog/rich")).contains("12 November 2026");
	}

	@ParameterizedTest(name = "{0}")
	@ValueSource(strings = { "longhand", "dateline", "linocut", "marginalia", "small-machines", "plainsong", "onionskin", "calling-card" })
	@DisplayName("THEME-14 each theme links /beansprout/code.css")
	void codeColors(String theme) throws Exception {
		for (String path : PAGES) {
			assertThat(page(theme, path)).as(theme + " " + path).contains("href=\"/beansprout/code.css\"");
		}
	}

	@ParameterizedTest(name = "{0}")
	@ValueSource(strings = { "longhand", "dateline", "linocut", "marginalia", "small-machines", "plainsong", "onionskin", "calling-card" })
	@DisplayName("THEME-15 in each color scheme, text, headings, secondary text and links have 4.5:1 contrast on the background")
	void contrast(String theme) throws Exception {
		String css = get(base(theme) + "/beansprout/" + theme + "/" + theme + ".css").body();
		List<Map<String, String>> schemes = new ArrayList<>();
		Matcher block = ROOT_BLOCK.matcher(css);
		while (block.find()) {
			Map<String, String> colors = new LinkedHashMap<>();
			Matcher color = THEME_COLOR.matcher(block.group(1));
			while (color.find()) {
				colors.put(color.group(1), color.group(2));
			}
			if (!colors.isEmpty()) {
				schemes.add(colors);
			}
		}
		assertThat(schemes).as(theme + " declares its colors").isNotEmpty();
		int declared = 0;
		Matcher any = Pattern.compile("--theme-(?:background|text|heading|muted|link)\\s*:").matcher(css);
		while (any.find()) {
			declared++;
		}
		assertThat(schemes.stream().mapToInt(Map::size).sum()).as(theme + " writes every --theme- color as six hex digits")
			.isEqualTo(declared);
		Map<String, String> first = schemes.getFirst();
		assertThat(first).as(theme + " first scheme").containsKeys("background", "text", "heading", "muted", "link");
		for (Map<String, String> scheme : schemes) {
			Map<String, String> colors = new HashMap<>(first);
			colors.putAll(scheme);
			for (String role : List.of("text", "heading", "muted", "link")) {
				assertThat(ratio(colors.get(role), colors.get("background")))
					.as(theme + " " + role + " #" + colors.get(role) + " on #" + colors.get("background"))
					.isGreaterThanOrEqualTo(4.5);
			}
		}
	}

	@ParameterizedTest(name = "{0}")
	@ValueSource(strings = { "longhand", "dateline", "linocut", "marginalia", "small-machines", "plainsong", "onionskin", "calling-card" })
	@DisplayName("THEME-16 each theme escapes every value it shows, except the post body and the JSON-LD")
	void escaping(String theme) throws Exception {
		for (String path : PAGES) {
			assertThat(page(theme, path)).as(theme + " " + path)
				.doesNotContain("<strong>post</strong>", "<b>text</b>", "<u>tion</u>", "<i>mary</i>");
		}
	}

	@Test
	@DisplayName("THEME-18 beansprout-bom manages the version of every published Beansprout artifact")
	void bomListsEveryArtifact() throws IOException {
		Path root = Path.of("..");
		Set<String> published = new TreeSet<>();
		Matcher module = Pattern.compile("<module>([^<]+)</module>").matcher(Files.readString(root.resolve("pom.xml")));
		while (module.find()) {
			String pom = Files.readString(root.resolve(module.group(1)).resolve("pom.xml"));
			Matcher id = Pattern.compile("<artifactId>([^<]+)</artifactId>").matcher(pom.substring(pom.indexOf("</parent>")));
			assertThat(id.find()).isTrue();
			published.add(id.group(1));
		}
		published.removeAll(Set.of("beansprout-sample", "beansprout-verification", "beansprout-bom"));

		String bom = Files.readString(root.resolve("beansprout-bom/pom.xml"));
		String managed = bom.substring(bom.indexOf("<dependencyManagement>"), bom.indexOf("</dependencyManagement>"));
		Set<String> listed = new TreeSet<>();
		Matcher id = Pattern.compile("<artifactId>([^<]+)</artifactId>").matcher(managed);
		while (id.find()) {
			listed.add(id.group(1));
		}
		assertThat(listed).isEqualTo(published);
		Matcher entry = Pattern.compile("<dependency>(.*?)</dependency>", Pattern.DOTALL).matcher(managed);
		while (entry.find()) {
			assertThat(entry.group(1)).as("every BOM entry has a version").contains("<version>${project.version}</version>");
		}
		assertThat(bom).doesNotContain("spring-boot-dependencies");
	}

	@Test
	@DisplayName("THEME-19 an app that imports beansprout-bom adds the starter and themes with no version")
	void importTheBom() throws IOException {
		String pom = Files.readString(Path.of("pom.xml"));
		assertThat(pom).containsPattern(
				"<artifactId>beansprout-bom</artifactId>\\s*<version>[^<]+</version>\\s*<type>pom</type>\\s*<scope>import</scope>");
		String dependencies = pom.substring(pom.lastIndexOf("<dependencies>"));
		for (String artifact : List.of("beansprout-spring-boot-starter", "beansprout-theme-longhand", "beansprout-theme-dateline",
				"beansprout-theme-linocut", "beansprout-theme-marginalia", "beansprout-theme-small-machines")) {
			Matcher dependency = Pattern
				.compile("<dependency>((?:(?!</dependency>).)*<artifactId>" + artifact + "</artifactId>(?:(?!</dependency>).)*)</dependency>",
						Pattern.DOTALL)
				.matcher(dependencies);
			assertThat(dependency.find()).as(artifact).isTrue();
			assertThat(dependency.group(1)).as(artifact + " has no version of its own").doesNotContain("<version>");
		}
		Set<String> names = new TreeSet<>();
		ServiceLoader.load(Theme.class).forEach(theme -> names.add(theme.name()));
		assertThat(names).contains("dateline", "linocut", "marginalia", "small-machines");
	}

	/** The WCAG contrast ratio of two colors written as six hex digits. */
	private static double ratio(String a, String b) {
		double la = luminance(a);
		double lb = luminance(b);
		return (Math.max(la, lb) + 0.05) / (Math.min(la, lb) + 0.05);
	}

	private static double luminance(String hex) {
		double[] channels = new double[3];
		for (int i = 0; i < 3; i++) {
			double c = Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16) / 255.0;
			channels[i] = c <= 0.03928 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
		}
		return 0.2126 * channels[0] + 0.7152 * channels[1] + 0.0722 * channels[2];
	}

}
