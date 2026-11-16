package dev.danvega.beansprout.sample;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;

/** Tag routes with more than one page. */
@SpringBootTest(properties = { "beansprout.posts.location=classpath:test-posts/", "beansprout.page-size=1" })
@AutoConfigureMockMvc
class TagRoutesTests {

	@Autowired
	private MockMvcTester mvc;

	@Test
	@DisplayName("PAGES-7 page 2 of a tag is served with the tag's older posts")
	void secondTagPage() {
		assertThat(mvc.get().uri("/blog/tags/java/page/2")).hasStatusOk().bodyText()
			.contains("Second post", "href=\"/blog/tags/java\"").doesNotContain("Hello &lt;b&gt;");
	}

	@Test
	@DisplayName("PAGES-11 page 1 of a tag that does not exist answers 404, not a redirect")
	void unknownTagPageOne() {
		assertThat(mvc.get().uri("/blog/tags/nope/page/1")).hasStatus(HttpStatus.NOT_FOUND).bodyText()
			.contains("There’s no page at this address");
	}

}
