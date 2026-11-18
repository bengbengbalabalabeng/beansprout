package dev.danvega.beansprout.sample;

import java.util.Map;

import dev.danvega.beansprout.view.Pages;
import dev.danvega.beansprout.web.ThemeRenderer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/** Each theme's not-found page, the way a static export renders it: with no address. */
class ExportThemesTests {

	/** The element each theme puts the requested address in. */
	private static final Map<String, String> ADDRESS = Map.of("longhand", "class=\"panel-path\"", "dateline",
			"class=\"path\"", "linocut", "class=\"code404\"", "marginalia", "class=\"address\"", "small-machines",
			"class=\"code404\"");

	@ParameterizedTest(name = "{0}")
	@ValueSource(strings = { "longhand", "dateline", "linocut", "marginalia", "small-machines" })
	@DisplayName("EXPORT-26 with an empty address each theme's not-found page leaves out the line that names it")
	void noAddressLine(String theme) {
		try (ConfigurableApplicationContext app = new SpringApplicationBuilder(SampleApplication.class)
			.run("--server.port=0", "--beansprout.theme=" + theme)) {
			ThemeRenderer renderer = app.getBean(ThemeRenderer.class);
			Pages pages = app.getBean(Pages.class);

			String withAddress = renderer.render("not-found", pages.notFound("/blog/zz-asked-for"));
			assertThat(withAddress).contains(ADDRESS.get(theme)).contains("/blog/zz-asked-for");

			String without = renderer.render("not-found", pages.notFound(""));
			assertThat(without).doesNotContain(ADDRESS.get(theme)).contains("404");
		}
	}

}
