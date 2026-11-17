package dev.danvega.beansprout.autoconfigure;

import java.util.ArrayList;
import java.util.List;

import dev.danvega.beansprout.view.ArchivePage;
import dev.danvega.beansprout.view.IndexPage;
import dev.danvega.beansprout.view.NotFoundPage;
import dev.danvega.beansprout.view.PostPage;
import dev.danvega.beansprout.view.TagPage;
import dev.danvega.beansprout.web.ThemeRenderer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.ClassUtils;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/** An app that renders Beansprout's pages with its own templates, and no theme jar. */
class OwnRendererTests {

	private final WebApplicationContextRunner web = new WebApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(BeansproutAutoConfiguration.class,
				BeansproutJteAutoConfiguration.class, BeansproutWebAutoConfiguration.class))
		.withUserConfiguration(OwnTemplates.class);

	@Test
	@DisplayName("OWN-1 with no theme jar, an app's own ThemeRenderer serves the blog pages")
	void ownRendererWithoutTheme() {
		assertThat(ClassUtils.isPresent("dev.danvega.beansprout.engine.jte.longhand.LonghandTheme", null))
			.as("no theme jar on this module's classpath")
			.isFalse();
		web.run(context -> {
			MockMvcTester mvc = mvc(context);
			assertThat(mvc.get().uri("/blog")).hasStatusOk().bodyText().isEqualTo("<p>my index</p>");
			assertThat(mvc.get().uri("/blog/hello")).hasStatusOk().bodyText().isEqualTo("<p>my post</p>");
		});
	}

	@Test
	@DisplayName("OWN-3 the renderer gets each page's template name and view record")
	void templateNamesAndRecords() {
		web.run(context -> {
			MockMvcTester mvc = mvc(context);
			mvc.get().uri("/blog").exchange();
			mvc.get().uri("/blog/hello").exchange();
			mvc.get().uri("/blog/tags/spring").exchange();
			mvc.get().uri("/blog/archive").exchange();
			mvc.get().uri("/blog/no-such-post").exchange();

			assertThat(context.getBean(OwnTemplates.class).seen).containsExactly(
					"index " + IndexPage.class.getSimpleName(), "post " + PostPage.class.getSimpleName(),
					"tag " + TagPage.class.getSimpleName(), "archive " + ArchivePage.class.getSimpleName(),
					"not-found " + NotFoundPage.class.getSimpleName());
		});
	}

	@Test
	@DisplayName("OWN-4 a page that does not exist answers 404 with the app's not-found page")
	void notFound() {
		web.run(context -> assertThat(mvc(context).get().uri("/blog/no-such-post")).hasStatus(404)
			.bodyText()
			.isEqualTo("<p>my not-found</p>"));
	}

	private static MockMvcTester mvc(WebApplicationContext context) {
		return MockMvcTester.create(MockMvcBuilders.webAppContextSetup(context).build());
	}

	@Configuration(proxyBeanMethods = false)
	static class OwnTemplates {

		final List<String> seen = new ArrayList<>();

		@Bean
		ThemeRenderer myTemplates() {
			return (template, page) -> {
				seen.add(template + " " + page.getClass().getSimpleName());
				return "<p>my " + template + "</p>";
			};
		}

	}

}
