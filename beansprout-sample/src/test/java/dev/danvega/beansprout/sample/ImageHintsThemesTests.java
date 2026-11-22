package dev.danvega.beansprout.sample;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
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

/** A picture's size in every theme, from specs/image-hints.md. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ImageHintsThemesTests {

	private static final Pattern IMG = Pattern.compile("(?i)<img\\b[^>]*>");

	private static final Pattern RULE = Pattern.compile("([^{}]+)\\{([^{}]*)\\}");

	private static final Pattern CLASS_ATTRIBUTE = Pattern.compile("class=\"([^\"]*)\"");

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

	/** The post page with one theme, after its head. */
	private String postPage(String theme) throws IOException, InterruptedException {
		ConfigurableApplicationContext app = apps.computeIfAbsent(theme,
				name -> new SpringApplicationBuilder(SampleApplication.class).run("--server.port=0",
						"--beansprout.theme=" + name, "--beansprout.posts.location=classpath:test-posts-images/"));
		String url = "http://localhost:" + app.getEnvironment().getProperty("local.server.port") + "/blog/sized";
		HttpResponse<String> response = http.send(HttpRequest.newBuilder(URI.create(url)).build(),
				HttpResponse.BodyHandlers.ofString());
		assertThat(response.statusCode()).as(theme + " /blog/sized").isEqualTo(200);
		String html = response.body();
		return html.substring(html.indexOf("</head>"));
	}

	private static List<String> imgs(String html, String src) {
		List<String> tags = new ArrayList<>();
		Matcher tag = IMG.matcher(html);
		while (tag.find()) {
			if (tag.group().contains("src=\"" + src + "\"")) {
				tags.add(tag.group());
			}
		}
		return tags;
	}

	private static String css(String theme) throws IOException {
		String path = "META-INF/resources/beansprout/" + theme + "/" + theme + ".css";
		try (InputStream in = ImageHintsThemesTests.class.getClassLoader().getResourceAsStream(path)) {
			assertThat(in).as(path).isNotNull();
			return new String(in.readAllBytes(), StandardCharsets.UTF_8).replaceAll("(?s)/\\*.*?\\*/", "");
		}
	}

	private static Set<String> classes(String tagOrPage) {
		Set<String> out = new TreeSet<>();
		Matcher attribute = CLASS_ATTRIBUTE.matcher(tagOrPage);
		while (attribute.find()) {
			out.addAll(List.of(attribute.group(1).trim().split("\\s+")));
		}
		out.remove("");
		return out;
	}

	/**
	 * The declarations of every rule that can reach an {@code <img>} with these classes on this
	 * page: its last part is {@code img} or the image's own classes, and every class it names is on
	 * the page.
	 */
	private static Map<String, String> rulesFor(String css, Set<String> imgClasses, Set<String> pageClasses) {
		Map<String, String> found = new LinkedHashMap<>();
		Matcher rule = RULE.matcher(css);
		while (rule.find()) {
			for (String selector : rule.group(1).split(",")) {
				String trimmed = selector.strip();
				if (trimmed.startsWith("@") || trimmed.isEmpty()) {
					continue;
				}
				String[] parts = trimmed.split("[\\s>+~]+");
				String subject = parts[parts.length - 1].replaceAll("::?[\\w-]+(\\([^)]*\\))?", "");
				Matcher name = Pattern.compile("^[a-zA-Z*]+").matcher(subject);
				String element = name.find() ? name.group() : "";
				Set<String> subjectClasses = classes("class=\"" + String.join(" ", subject.replaceAll("^[a-zA-Z*]+", "")
					.replaceAll("\\[[^]]*]", "")
					.split("\\.")) + "\"");
				boolean reaches = (element.equalsIgnoreCase("img") || (element.isEmpty() && !subjectClasses.isEmpty()))
						&& imgClasses.containsAll(subjectClasses) && !subject.contains("#");
				Matcher named = Pattern.compile("\\.([\\w-]+)").matcher(trimmed);
				while (reaches && named.find()) {
					reaches = pageClasses.contains(named.group(1));
				}
				if (reaches) {
					found.merge(trimmed, rule.group(2), (a, b) -> a + ";" + b);
				}
			}
		}
		return found;
	}

	private static void inProportion(String theme, String what, Map<String, String> rules) {
		String all = String.join(";", rules.values()).replaceAll("\\s+", " ");
		assertThat(rules).as(theme + " " + what + ": some rule styles it").isNotEmpty();
		assertThat(all).as(theme + " " + what + " " + rules).containsPattern("(^|[;{ ])height: ?auto");
		for (Map.Entry<String, String> entry : rules.entrySet()) {
			String declarations = entry.getValue().replaceAll("\\s+", " ");
			assertThat(declarations).as(theme + " " + what + " " + entry.getKey())
				.doesNotContainPattern("(^|[;{ ])aspect-ratio:")
				.doesNotContainPattern("(^|[;{ ])(min-|max-)?height:\\s*+(?!auto\\b)[^;\\s]");
		}
	}

	private static void insideColumn(String theme, String what, Map<String, String> rules) {
		String all = String.join(";", rules.values()).replaceAll("\\s+", " ");
		assertThat(rules).as(theme + " " + what + ": some rule styles it").isNotEmpty();
		assertThat(all).as(theme + " " + what + " " + rules).containsPattern("(^|[;{ ])(max-)?width: ?100%");
		for (Map.Entry<String, String> entry : rules.entrySet()) {
			String declarations = entry.getValue().replaceAll("\\s+", " ");
			// A width in fixed units, even a max-width, can be wider than a narrow column.
			assertThat(declarations).as(theme + " " + what + " " + entry.getKey())
				.doesNotContainPattern("(^|[;{ ])(min-|max-)?width:\\s*+[0-9.]+(px|rem|em|ch|vw|cm|mm|in|pt)");
		}
	}

	/** Each theme's CSS rules that reach the sized picture, then those that reach a body image. */
	private void eachTheme(Check check) throws Exception {
		for (String theme : themes()) {
			String page = postPage(theme);
			String css = css(theme);
			Set<String> pageClasses = classes(page);
			String picture = imgs(page, "/blog/sized/cover.png").getFirst();
			check.apply(theme, "picture", rulesFor(css, classes(picture), pageClasses));
			check.apply(theme, "body image", rulesFor(css, Set.of(), pageClasses));
		}
	}

	@FunctionalInterface
	private interface Check {

		void apply(String theme, String what, Map<String, String> rules);

	}

	@Test
	@DisplayName("IMG-9 every theme sets the width and height of a picture whose size it has")
	void pictureSize() throws Exception {
		for (String theme : themes()) {
			String page = postPage(theme);
			assertThat(imgs(page, "/blog/sized/cover.png")).as(theme)
				.isNotEmpty()
				.allSatisfy(tag -> assertThat(tag).contains("width=\"320\"", "height=\"200\""));
			assertThat(imgs(page, "/blog/sized/shot.png")).as(theme)
				.singleElement()
				.satisfies(tag -> assertThat(tag).contains("width=\"200\"", "height=\"100\"", "loading=\"lazy\""));
			assertThat(imgs(page, "/images/whiteboard-queue.svg")).as(theme)
				.singleElement()
				.satisfies(tag -> assertThat(tag).doesNotContain("width", "height"));
		}
	}

	@Test
	@DisplayName("IMG-10 every theme keeps a sized picture and a sized body image in proportion")
	void inProportion() throws Exception {
		eachTheme(ImageHintsThemesTests::inProportion);
	}

	@Test
	@DisplayName("IMG-11 every theme keeps a sized picture and a sized body image no wider than its column")
	void insideColumn() throws Exception {
		eachTheme(ImageHintsThemesTests::insideColumn);
	}

}
