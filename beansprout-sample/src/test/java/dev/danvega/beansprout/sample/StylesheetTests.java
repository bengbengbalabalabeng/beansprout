package dev.danvega.beansprout.sample;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;

/** The code stylesheet and Longhand's colors, read as CSS and measured for contrast. */
@SpringBootTest(properties = "beansprout.posts.location=classpath:test-posts/")
@AutoConfigureMockMvc
class StylesheetTests {

	private static final String CODE_CSS = "META-INF/resources/beansprout/code.css";

	private static final String LONGHAND_CSS = "META-INF/resources/beansprout/longhand/longhand.css";

	private static final List<String> TOKEN_COLORS = List.of("--bs-code-fg", "--bs-code-comment", "--bs-code-keyword",
			"--bs-code-string", "--bs-code-function", "--bs-code-constant", "--bs-code-type", "--bs-code-variable",
			"--bs-code-tag", "--bs-code-attr");

	@Autowired
	private MockMvcTester mvc;

	private static String css(String path) throws IOException {
		try (InputStream in = StylesheetTests.class.getClassLoader().getResourceAsStream(path)) {
			assertThat(in).as(path).isNotNull();
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
	}

	/** Custom properties in the first top-level :root block. */
	private static Map<String, String> light(String css) {
		return vars(css.substring(css.indexOf(":root {")));
	}

	/** Custom properties in the :root block inside the prefers-color-scheme: dark media query. */
	private static Map<String, String> dark(String css) {
		int media = css.indexOf("@media (prefers-color-scheme: dark)");
		assertThat(media).as("a dark scheme media query").isNotNegative();
		return vars(css.substring(css.indexOf(":root {", media)));
	}

	private static Map<String, String> vars(String fromRoot) {
		String block = fromRoot.substring(fromRoot.indexOf('{') + 1, fromRoot.indexOf('}'));
		Map<String, String> vars = new LinkedHashMap<>();
		Matcher m = Pattern.compile("(--[\\w-]+)\\s*:\\s*([^;]+);").matcher(block);
		while (m.find()) {
			vars.put(m.group(1), m.group(2).strip());
		}
		return vars;
	}

	private static double luminance(String hex) {
		String h = hex.replace("#", "");
		if (h.length() == 3) {
			h = "" + h.charAt(0) + h.charAt(0) + h.charAt(1) + h.charAt(1) + h.charAt(2) + h.charAt(2);
		}
		double[] c = new double[3];
		for (int i = 0; i < 3; i++) {
			double v = Integer.parseInt(h.substring(i * 2, i * 2 + 2), 16) / 255.0;
			c[i] = v <= 0.04045 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4);
		}
		return 0.2126 * c[0] + 0.7152 * c[1] + 0.0722 * c[2];
	}

	private static double contrast(String a, String b) {
		double la = luminance(a);
		double lb = luminance(b);
		return (Math.max(la, lb) + 0.05) / (Math.min(la, lb) + 0.05);
	}

	private static void assertContrast(Map<String, String> vars, String fg, String bg, String scheme) {
		assertThat(vars).as(scheme).containsKeys(fg, bg);
		assertThat(contrast(vars.get(fg), vars.get(bg))).as("%s %s on %s", scheme, fg, bg).isGreaterThanOrEqualTo(4.5);
	}

	@Test
	@DisplayName("HL-11 the code stylesheet is served at /beansprout/code.css")
	void codeCssServed() {
		assertThat(mvc.get().uri("/beansprout/code.css")).hasStatusOk().hasContentTypeCompatibleWith("text/css")
			.bodyText().contains(".token.keyword");
	}

	@Test
	@DisplayName("HL-12 the code stylesheet takes every token color from a custom property")
	void tokenColorsAreProperties() throws IOException {
		String css = css(CODE_CSS);
		String rules = css.substring(css.indexOf(".token"));

		assertThat(light(css)).containsKeys(TOKEN_COLORS.toArray(String[]::new)).containsKey("--bs-code-bg");
		assertThat(rules).doesNotContainPattern("color:\\s*#");
	}

	@Test
	@DisplayName("HL-13 the code stylesheet has dark colors for a dark scheme")
	void darkCode() throws IOException {
		String css = css(CODE_CSS);

		assertThat(dark(css)).containsKeys(TOKEN_COLORS.toArray(String[]::new)).containsKey("--bs-code-bg");
		assertThat(dark(css).get("--bs-code-bg")).isNotEqualTo(light(css).get("--bs-code-bg"));
	}

	@Test
	@DisplayName("HL-14 Longhand links the code stylesheet")
	void longhandLinksIt() {
		assertThat(mvc.get().uri("/blog")).bodyText().contains("<link rel=\"stylesheet\" href=\"/beansprout/code.css\">");
	}

	@Test
	@DisplayName("HL-15 the colors are Shiki's GitHub Light Default and GitHub Dark Default")
	void githubColors() throws IOException {
		String css = css(CODE_CSS);

		assertThat(light(css)).containsEntry("--bs-code-bg", "#ffffff").containsEntry("--bs-code-keyword", "#cf222e")
			.containsEntry("--bs-code-string", "#0a3069").containsEntry("--bs-code-comment", "#6e7781");
		assertThat(dark(css)).containsEntry("--bs-code-bg", "#0d1117").containsEntry("--bs-code-keyword", "#ff7b72")
			.containsEntry("--bs-code-string", "#a5d6ff").containsEntry("--bs-code-comment", "#8b949e");
	}

	@Test
	@DisplayName("HL-16 every token color reaches 4.5:1 against the code background, light and dark")
	void tokenContrast() throws IOException {
		String css = css(CODE_CSS);
		for (String token : TOKEN_COLORS) {
			assertContrast(light(css), token, "--bs-code-bg", "light");
			assertContrast(dark(css), token, "--bs-code-bg", "dark");
		}
	}

	@Test
	@DisplayName("HL-17 every code block, with or without a language, gets its colors at zero specificity")
	void zeroSpecificity() throws IOException {
		String css = css(CODE_CSS);

		assertThat(css).contains(":where(pre:has(> code)) {");
		assertThat(css.lines().filter(line -> line.contains("background")).filter(line -> !line.strip().startsWith("--")))
			.allSatisfy(line -> assertThat(line).doesNotContain("!important"));
	}

	@Test
	@DisplayName("DARK-1 Longhand has dark colors for a dark scheme")
	void longhandDark() throws IOException {
		String css = css(LONGHAND_CSS);

		assertThat(dark(css)).containsKeys("--paper", "--surface", "--text", "--ink", "--accent");
		assertThat(dark(css).get("--paper")).isNotEqualTo(light(css).get("--paper"));
	}

	@Test
	@DisplayName("DARK-2 the scheme switches with CSS alone: the theme loads no script")
	void noScript() {
		assertThat(mvc.get().uri("/blog/hello")).bodyText().doesNotContain("<script src", "<script>")
			.contains("<script type=\"application/ld+json\">");
	}

	@Test
	@DisplayName("DARK-3 Longhand declares color-scheme: light dark")
	void colorScheme() throws IOException {
		assertThat(css(LONGHAND_CSS)).contains("color-scheme: light dark;");
	}

	@Test
	@DisplayName("DARK-4 text, headings, secondary text and links reach 4.5:1 on page and card, light and dark")
	void longhandContrast() throws IOException {
		String css = css(LONGHAND_CSS);
		for (String fg : List.of("--text", "--ink", "--ink-2", "--ink-3", "--accent")) {
			for (String bg : List.of("--paper", "--surface")) {
				assertContrast(light(css), fg, bg, "light");
				assertContrast(dark(css), fg, bg, "dark");
			}
		}
	}

	@Test
	@DisplayName("DARK-5 text on the accent reaches 4.5:1, light and dark")
	void accentContrast() throws IOException {
		String css = css(LONGHAND_CSS);

		assertContrast(light(css), "--on-accent", "--accent", "light");
		assertContrast(dark(css), "--on-accent", "--accent", "dark");
	}

	@Test
	@DisplayName("DARK-6 Longhand's code colors come from the code stylesheet")
	void longhandUsesCodeColors() throws IOException {
		Map<String, String> longhand = light(css(LONGHAND_CSS));

		assertThat(longhand.get("--code-bg")).startsWith("var(--bs-code-bg");
		assertThat(longhand.get("--code-text")).startsWith("var(--bs-code-fg");
		assertThat(longhand.get("--code-keyword")).startsWith("var(--bs-code-keyword");
	}

}
