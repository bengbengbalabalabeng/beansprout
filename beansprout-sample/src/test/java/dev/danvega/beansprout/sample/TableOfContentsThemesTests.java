package dev.danvega.beansprout.sample;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/** The "On this page" list in the five themes that had none, from specs/table-of-contents.md. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TableOfContentsThemesTests {

	private static final List<String> THEMES = List.of("calling-card", "dateline", "linocut", "marginalia", "onionskin");

	private static final Pattern TOC = Pattern.compile("<nav class=\"toc\"[^>]*>(.*?)</nav>", Pattern.DOTALL);

	private static final Pattern LINK = Pattern.compile("<a href=\"([^\"]*)\">([^<]*)</a>");

	private static final Pattern ROOT_BLOCK = Pattern.compile(":root\\s*\\{([^}]*)}");

	private static final Pattern THEME_COLOR = Pattern.compile("--theme-(background|text|heading|muted|link)\\s*:\\s*#([0-9a-fA-F]{6})\\s*;");

	private static final Pattern RULE = Pattern.compile("([^{}]+)\\{([^{}]*)}");

	private final HttpClient http = HttpClient.newHttpClient();

	private final Map<String, ConfigurableApplicationContext> apps = new HashMap<>();

	@AfterAll
	void stop() {
		apps.values().forEach(ConfigurableApplicationContext::close);
		http.close();
	}

	private String page(String theme, String path) throws IOException, InterruptedException {
		ConfigurableApplicationContext app = apps.computeIfAbsent(theme,
				key -> new SpringApplicationBuilder(SampleApplication.class).run("--server.port=0",
						"--beansprout.theme=" + theme, "--beansprout.posts.location=classpath:test-posts-toc/"));
		String url = "http://localhost:" + app.getEnvironment().getProperty("local.server.port") + path;
		HttpResponse<String> response = http.send(HttpRequest.newBuilder(URI.create(url)).build(),
				HttpResponse.BodyHandlers.ofString());
		assertThat(response.statusCode()).as(theme + " " + path).isEqualTo(200);
		return response.body();
	}

	/** The one list on the page, or empty when there is none. */
	private static List<String> tocs(String html) {
		return TOC.matcher(html).results().map(m -> m.group(1)).toList();
	}

	/** The list's links, as href and text. */
	private static List<String[]> links(String toc) {
		return LINK.matcher(toc).results().map(m -> new String[] { m.group(1), m.group(2) }).toList();
	}

	private void showsTheList(String theme) throws Exception {
		String html = page(theme, "/blog/long");
		List<String> tocs = tocs(html);
		assertThat(tocs).as(theme + " one list").hasSize(1);
		String toc = tocs.getFirst();
		assertThat(toc).as(theme).contains("On this page");
		assertThat(links(toc)).as(theme).extracting(link -> link[1])
			.containsExactly("First part", "Beans &amp; more", "Last words");
		assertThat(toc).as(theme + " only the ## headings").doesNotContain("A deeper point");
	}

	@Test
	@DisplayName("TOC-1 when a post has headings, Dateline shows an On this page list of them")
	void dateline() throws Exception {
		showsTheList("dateline");
	}

	@Test
	@DisplayName("TOC-2 when a post has headings, Linocut shows an On this page list of them")
	void linocut() throws Exception {
		showsTheList("linocut");
	}

	@Test
	@DisplayName("TOC-3 when a post has headings, Marginalia shows an On this page list of them")
	void marginalia() throws Exception {
		showsTheList("marginalia");
	}

	@Test
	@DisplayName("TOC-7 when a post has headings, Onionskin shows an On this page list of them")
	void onionskin() throws Exception {
		showsTheList("onionskin");
	}

	@Test
	@DisplayName("TOC-8 when a post has headings, Calling Card shows an On this page list of them")
	void callingCard() throws Exception {
		showsTheList("calling-card");
	}

	@Test
	@DisplayName("TOC-4 each entry in the list links to its heading's id on the page")
	void linksToHeadings() throws Exception {
		for (String theme : THEMES) {
			String html = page(theme, "/blog/long");
			List<String> ids = Pattern.compile("<h2 id=\"([^\"]+)\"").matcher(html).results().map(m -> m.group(1)).toList();
			assertThat(ids).as(theme + " the post's headings").hasSize(3);
			List<String[]> links = links(tocs(html).getFirst());
			assertThat(links).as(theme).extracting(link -> link[0])
				.containsExactlyElementsOf(ids.stream().map(id -> "#" + id).toList());
		}
	}

	@Test
	@DisplayName("TOC-5 when a post has no headings, Calling Card, Dateline, Linocut, Marginalia and Onionskin show no list")
	void noListWithoutHeadings() throws Exception {
		for (String theme : THEMES) {
			String html = page(theme, "/blog/short");
			assertThat(html).as(theme).contains("A short note with no sections.");
			assertThat(tocs(html)).as(theme).isEmpty();
			assertThat(html).as(theme).doesNotContain("class=\"toc").doesNotContain("On this page");
		}
	}

	@Test
	@DisplayName("TOC-6 the list's text reaches 4.5 to 1: it uses only the theme's measured colors, on its page background")
	void contrast() throws Exception {
		for (String theme : THEMES) {
			String css = page(theme, "/beansprout/" + theme + "/" + theme + ".css");
			List<String[]> rules = RULE.matcher(css).results()
				.filter(m -> m.group(1).contains(".toc"))
				.map(m -> new String[] { m.group(1).strip(), m.group(2) })
				.toList();
			assertThat(rules).as(theme + " styles the list").isNotEmpty();
			List<String> colors = new ArrayList<>();
			boolean background = false;
			for (String[] rule : rules) {
				Matcher declaration = Pattern.compile("(?:^|;)\\s*(color|background|background-color)\\s*:\\s*([^;]+)")
					.matcher(rule[1]);
				while (declaration.find()) {
					String value = declaration.group(2).strip();
					if (declaration.group(1).equals("color")) {
						assertThat(value).as(theme + " " + rule[0] + " color")
							.matches("var\\(--theme-(text|heading|muted|link)\\)");
						colors.add(value.substring("var(--theme-".length(), value.length() - 1));
					}
					else {
						assertThat(value).as(theme + " " + rule[0] + " background").isEqualTo("var(--theme-background)");
						background |= rule[0].equals(".toc");
					}
				}
			}
			assertThat(background).as(theme + " .toc sets the page background").isTrue();
			assertThat(colors).as(theme + " the title and the links set a color").hasSizeGreaterThanOrEqualTo(2);
			for (Map<String, String> scheme : schemes(css)) {
				for (String role : colors) {
					assertThat(ratio(scheme.get(role), scheme.get("background")))
						.as(theme + " " + role + " #" + scheme.get(role) + " on #" + scheme.get("background"))
						.isGreaterThanOrEqualTo(4.5);
				}
			}
		}
	}

	/** Each :root block's theme colors, over the first one's. */
	private static List<Map<String, String>> schemes(String css) {
		List<Map<String, String>> schemes = new ArrayList<>();
		Matcher block = ROOT_BLOCK.matcher(css);
		while (block.find()) {
			Map<String, String> colors = new LinkedHashMap<>();
			Matcher color = THEME_COLOR.matcher(block.group(1));
			while (color.find()) {
				colors.put(color.group(1), color.group(2));
			}
			if (!colors.isEmpty()) {
				Map<String, String> merged = new HashMap<>(schemes.isEmpty() ? Map.of() : schemes.getFirst());
				merged.putAll(colors);
				schemes.add(merged);
			}
		}
		assertThat(schemes).isNotEmpty();
		return schemes;
	}

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
