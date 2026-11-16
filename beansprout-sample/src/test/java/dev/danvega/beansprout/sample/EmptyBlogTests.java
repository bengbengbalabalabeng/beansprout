package dev.danvega.beansprout.sample;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;

/** A blog whose posts folder does not exist yet. */
@SpringBootTest(properties = "beansprout.posts.location=classpath:no-posts-here/")
@AutoConfigureMockMvc
class EmptyBlogTests {

	@Autowired
	private MockMvcTester mvc;

	@Test
	@DisplayName("PAGES-12 with no posts the index answers 200 and shows no cards")
	void emptyIndex() {
		assertThat(mvc.get().uri("/blog")).hasStatusOk().bodyText().contains("No posts yet.").doesNotContain("class=\"card\"");
		assertThat(mvc.get().uri("/blog/archive")).hasStatusOk().bodyText().contains("0 posts");
		assertThat(mvc.get().uri("/blog/feed.xml")).hasStatusOk().bodyText().contains("<feed").doesNotContain("<entry>");
	}

	@Test
	@DisplayName("PAGES-13 the blog's name comes from beansprout.title in application.yaml")
	void titleFromApplicationYaml() {
		assertThat(mvc.get().uri("/blog")).bodyText()
			.contains("<a class=\"site-name-link\" href=\"/blog\">Beansprout Notes</a>");
	}

}
