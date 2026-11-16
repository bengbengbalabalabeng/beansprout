package dev.danvega.beansprout.sample;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;

/** beansprout.site-url, set the way a deployed app sets it. */
@SpringBootTest(properties = { "beansprout.posts.location=classpath:test-posts-rich/",
		"beansprout.site-url=https://beans.example.com/" })
@AutoConfigureMockMvc
class SeoSiteUrlTests {

	@Autowired
	private MockMvcTester mvc;

	@Test
	@DisplayName("SEO-16 with beansprout.site-url set, pages use it, not the request's address")
	void siteUrlOnPages() {
		assertThat(mvc.get().uri("http://internal-host:9000/blog/pictured")).hasStatusOk().bodyText()
			.contains("<link rel=\"canonical\" href=\"https://beans.example.com/blog/pictured\">",
					"<meta property=\"og:image\" content=\"https://beans.example.com/images/test-run.svg\">")
			.doesNotContain("internal-host");
	}

}
