package dev.danvega.beansprout.sample;

import dev.danvega.beansprout.view.PostPage;
import dev.danvega.beansprout.web.ThemeRenderer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;

/** The sample has the Longhand theme on its classpath. An app's own renderer still wins. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(OwnRendererTests.OwnTemplates.class)
class OwnRendererTests {

	@Autowired
	private MockMvcTester mvc;

	@Test
	@DisplayName("OWN-2 with the Longhand theme on the classpath, an app's own ThemeRenderer serves the pages")
	void ownRendererBeatsTheme() {
		assertThat(ClassUtils.isPresent("dev.danvega.beansprout.engine.jte.longhand.LonghandTheme", null))
			.as("the Longhand theme is on the sample's classpath")
			.isTrue();
		assertThat(mvc.get().uri("/blog")).hasStatusOk().bodyText().isEqualTo("<p>my index</p>");
		assertThat(mvc.get().uri("/blog/why-jte")).hasStatusOk()
			.bodyText()
			.isEqualTo("<h1>Why JTE is the default</h1>");
	}

	@TestConfiguration(proxyBeanMethods = false)
	static class OwnTemplates {

		@Bean
		ThemeRenderer myTemplates() {
			return (template, page) -> page instanceof PostPage post ? "<h1>" + post.post().title() + "</h1>"
					: "<p>my " + template + "</p>";
		}

	}

}
