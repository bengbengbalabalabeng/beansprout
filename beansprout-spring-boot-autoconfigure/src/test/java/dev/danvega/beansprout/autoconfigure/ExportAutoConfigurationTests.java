package dev.danvega.beansprout.autoconfigure;

import java.util.Map;

import dev.danvega.beansprout.export.StaticExport;
import dev.danvega.beansprout.web.ThemeRenderer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

/** The export's switch, its defaults, and the settings it refuses before it writes anything. */
class ExportAutoConfigurationTests {

	private final WebApplicationContextRunner web = new WebApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(BeansproutAutoConfiguration.class, BeansproutJteAutoConfiguration.class,
				BeansproutWebAutoConfiguration.class, BeansproutExportAutoConfiguration.class));

	private final WebApplicationContextRunner themed = web.withUserConfiguration(PlainTheme.class);

	@Test
	@DisplayName("EXPORT-4 without beansprout.export.enabled there is no export")
	void offByDefault() {
		themed.withPropertyValues("beansprout.site-url=https://example.com")
			.run(context -> assertThat(context).hasNotFailed().doesNotHaveBean(StaticExport.class));
	}

	@Test
	@DisplayName("EXPORT-4 beansprout.export.enabled=false means no export")
	void offWhenFalse() {
		themed.withPropertyValues("beansprout.export.enabled=false", "beansprout.site-url=https://example.com")
			.run(context -> assertThat(context).hasNotFailed().doesNotHaveBean(StaticExport.class));
	}

	@Test
	@DisplayName("EXPORT-1 beansprout.export.enabled=true sets up the export")
	void onWhenTrue() {
		themed.withPropertyValues("beansprout.export.enabled=true", "beansprout.site-url=https://example.com")
			.run(context -> assertThat(context).hasNotFailed().hasSingleBean(StaticExport.class));
	}

	@Test
	@DisplayName("EXPORT-3 the output folder is target/blog unless beansprout.export.output says otherwise")
	void outputFolder() {
		themed.withPropertyValues("beansprout.export.enabled=true", "beansprout.site-url=https://example.com")
			.run(context -> assertThat(context.getBean(StaticExport.class).output().toString())
				.endsWith("target/blog".replace('/', java.io.File.separatorChar)));
		themed.withPropertyValues("beansprout.export.enabled=true", "beansprout.site-url=https://example.com",
				"beansprout.export.output=build/site")
			.run(context -> assertThat(context.getBean(StaticExport.class).output().toString())
				.endsWith("build/site".replace('/', java.io.File.separatorChar)));
	}

	@Test
	@DisplayName("EXPORT-10 an export without beansprout.site-url stops and says to set it")
	void needsSiteUrl() {
		themed.withPropertyValues("beansprout.export.enabled=true")
			.run(context -> assertThat(context).hasFailed()
				.getFailure()
				.rootCause()
				.hasMessageContaining("beansprout.site-url")
				.hasMessageContaining("Set it"));
	}

	@Test
	@DisplayName("EXPORT-11 a site-url with a path stops and says to use server.servlet.context-path")
	void siteUrlWithPath() {
		themed.withPropertyValues("beansprout.export.enabled=true", "beansprout.site-url=https://dan.github.io/notes")
			.run(context -> assertThat(context).hasFailed()
				.getFailure()
				.rootCause()
				.hasMessageContaining("server.servlet.context-path=/notes")
				.hasMessageContaining("https://dan.github.io"));
	}

	@Test
	@DisplayName("EXPORT-11 a site-url that ends in a slash has no path")
	void siteUrlWithSlash() {
		themed.withPropertyValues("beansprout.export.enabled=true", "beansprout.site-url=https://dan.github.io/")
			.run(context -> assertThat(context).hasNotFailed().hasSingleBean(StaticExport.class));
	}

	@Test
	@DisplayName("EXPORT-12 an export with no theme stops and says to add one")
	void needsTheme() {
		web.withPropertyValues("beansprout.export.enabled=true", "beansprout.site-url=https://example.com")
			.run(context -> assertThat(context).hasFailed()
				.getFailure()
				.rootCause()
				.hasMessageContaining("no theme")
				.hasMessageContaining("beansprout-theme-longhand"));
	}

	@Test
	@DisplayName("EXPORT-5 an export serves the app on a free port, over the app's own server.port")
	void freePort() {
		StandardEnvironment environment = environment(Map.of("beansprout.export.enabled", "true", "server.port", "8080"));
		new ExportDefaultsEnvironmentPostProcessor().postProcessEnvironment(environment, new SpringApplication());
		assertThat(environment.getProperty("server.port")).isEqualTo("0");
	}

	@Test
	@DisplayName("EXPORT-6 an export listens only on 127.0.0.1")
	void loopbackOnly() {
		StandardEnvironment environment = environment(Map.of("beansprout.export.enabled", "true", "server.address", "0.0.0.0"));
		new ExportDefaultsEnvironmentPostProcessor().postProcessEnvironment(environment, new SpringApplication());
		assertThat(environment.getProperty("server.address")).isEqualTo("127.0.0.1");
	}

	@Test
	@DisplayName("EXPORT-4 without the export the app keeps its own port and address")
	void portUntouched() {
		StandardEnvironment environment = environment(Map.of("server.port", "8080"));
		new ExportDefaultsEnvironmentPostProcessor().postProcessEnvironment(environment, new SpringApplication());
		assertThat(environment.getProperty("server.port")).isEqualTo("8080");
		assertThat(environment.getProperty("server.address")).isNull();
	}

	private static StandardEnvironment environment(Map<String, Object> values) {
		StandardEnvironment environment = new StandardEnvironment();
		environment.getPropertySources().addFirst(new MapPropertySource("commandLineArgs", values));
		return environment;
	}

	@Configuration(proxyBeanMethods = false)
	static class PlainTheme {

		@Bean
		ThemeRenderer plainTheme() {
			return (template, page) -> "<p>" + template + "</p>";
		}

	}

}
