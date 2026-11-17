package dev.danvega.beansprout.autoconfigure;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;

import dev.danvega.beansprout.theme.Theme;
import dev.danvega.beansprout.web.ThemeRenderer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/** Which theme renders the blog when the classpath holds none, one or several. */
class ThemeSelectionTests {

	@TempDir
	Path services;

	/** An app whose classpath registers these themes, the way a theme jar does. */
	private WebApplicationContextRunner app(Class<?>... themes) throws IOException {
		Path file = services.resolve("META-INF/services/" + Theme.class.getName());
		Files.createDirectories(file.getParent());
		StringBuilder lines = new StringBuilder();
		for (Class<?> theme : themes) {
			lines.append(theme.getName()).append('\n');
		}
		Files.writeString(file, lines);
		ClassLoader loader = new URLClassLoader(new URL[] { services.toUri().toURL() }, getClass().getClassLoader());
		return new WebApplicationContextRunner().withClassLoader(loader)
			.withConfiguration(AutoConfigurations.of(BeansproutAutoConfiguration.class,
					BeansproutJteAutoConfiguration.class, BeansproutWebAutoConfiguration.class));
	}

	@Test
	@DisplayName("THEME-1 with exactly one theme on the classpath, the blog renders with it")
	void oneTheme() throws IOException {
		app(Alpha.class).run(context -> {
			assertThat(context).hasNotFailed().hasSingleBean(ThemeRenderer.class);
			assertThat(context.getBean(Theme.class).name()).isEqualTo("alpha");
		});
	}

	@Test
	@DisplayName("THEME-2 beansprout.theme picks the theme the blog renders with")
	void namedTheme() throws IOException {
		app(Alpha.class, Beta.class).withPropertyValues("beansprout.theme=beta").run(context -> {
			assertThat(context).hasNotFailed();
			assertThat(context.getBean(Theme.class).name()).isEqualTo("beta");
		});
	}

	@Test
	@DisplayName("THEME-2 two theme jars with the name beansprout.theme picks fail the start instead of picking one")
	void sameNameTwice() throws IOException {
		app(Alpha.class, AlphaAgain.class).withPropertyValues("beansprout.theme=alpha").run(context -> assertThat(context)
			.hasFailed()
			.getFailure()
			.rootCause()
			.hasMessageContaining("alpha")
			.hasMessageContaining("more than one"));
	}

	@Test
	@DisplayName("THEME-3 with two themes and no beansprout.theme, the app fails to start and lists them")
	void twoThemesAndNoChoice() throws IOException {
		app(Alpha.class, Beta.class).run(context -> assertThat(context).hasFailed()
			.getFailure()
			.rootCause()
			.hasMessageContaining("beansprout.theme")
			.hasMessageContaining("alpha")
			.hasMessageContaining("beta"));
	}

	@Test
	@DisplayName("THEME-4 a beansprout.theme that is not on the classpath fails the start, naming it and the themes found")
	void unknownTheme() throws IOException {
		app(Alpha.class, Beta.class).withPropertyValues("beansprout.theme=gamma").run(context -> assertThat(context)
			.hasFailed()
			.getFailure()
			.rootCause()
			.hasMessageContaining("gamma")
			.hasMessageContaining("alpha")
			.hasMessageContaining("beta"));
	}

	@Test
	@DisplayName("THEME-4 a beansprout.theme with no theme jar on the classpath at all fails the start, naming it")
	void namedThemeButNone() throws IOException {
		app().withPropertyValues("beansprout.theme=gamma").run(context -> assertThat(context).hasFailed()
			.getFailure()
			.rootCause()
			.hasMessageContaining("gamma")
			.hasMessageContaining("no theme"));
	}

	@Test
	@DisplayName("THEME-1 with no theme jar and no beansprout.theme, the app starts without the blog pages")
	void noThemeNoChoice() throws IOException {
		app().run(context -> assertThat(context).hasNotFailed().doesNotHaveBean(ThemeRenderer.class));
	}

	@Test
	@DisplayName("THEME-17 an app's own ThemeRenderer starts with any theme jars present, with no beansprout.theme check")
	void ownRendererSkipsTheChoice() throws IOException {
		app(Alpha.class, Beta.class).withPropertyValues("beansprout.theme=gamma")
			.withUserConfiguration(OwnRenderer.class)
			.run(context -> {
				assertThat(context).hasNotFailed();
				assertThat(context.getBean(ThemeRenderer.class).render("index", "page")).isEqualTo("mine");
			});
	}

	@Configuration(proxyBeanMethods = false)
	static class OwnRenderer {

		@Bean
		ThemeRenderer myRenderer() {
			return (template, page) -> "mine";
		}

	}

	/** A theme with no templates: enough to be found and chosen. */
	public static class Alpha implements Theme {

		@Override
		public String name() {
			return "alpha";
		}

		@Override
		public String engine() {
			return "jte";
		}

		@Override
		public String templates() {
			return "dev.danvega.beansprout.test.alpha";
		}

		@Override
		public String stylesheet() {
			return "/beansprout/alpha/alpha.css";
		}

	}

	/** A second jar that calls itself alpha too. */
	public static class AlphaAgain extends Alpha {

	}

	public static class Beta extends Alpha {

		@Override
		public String name() {
			return "beta";
		}

	}

}
