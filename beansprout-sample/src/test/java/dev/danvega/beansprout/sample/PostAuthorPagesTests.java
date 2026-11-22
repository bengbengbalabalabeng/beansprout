package dev.danvega.beansprout.sample;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;

/** A post's own {@code author}, from specs/post-author.md, end to end. */
@SpringBootTest(properties = { "beansprout.posts.location=classpath:test-posts-author/", "beansprout.author=Ines" })
@AutoConfigureMockMvc
class PostAuthorPagesTests {

	@Autowired
	private MockMvcTester mvc;

	private String body(String uri) throws Exception {
		var result = mvc.get().uri(uri).exchange();
		assertThat(result).hasStatusOk();
		return result.getResponse().getContentAsString(StandardCharsets.UTF_8);
	}

	@Test
	@DisplayName("AUTHOR-1 and AUTHOR-2 a guest post's page shows its author, in the byline and the JSON-LD")
	void guestPage() throws Exception {
		String page = body("/blog/guest");

		assertThat(page).contains("<p class=\"post-author\">Jane Doe</p>", "\"author\":{\"@type\":\"Person\",\"name\":\"Jane Doe\"}")
			.doesNotContain("<p class=\"post-author\">Ines</p>");
	}

	@Test
	@DisplayName("AUTHOR-7 and AUTHOR-8 a post with no author, or a nested one, shows beansprout.author")
	void ownPages() throws Exception {
		assertThat(body("/blog/own")).contains("<p class=\"post-author\">Ines</p>");
		assertThat(body("/blog/seo-tag")).contains("<p class=\"post-author\">Ines</p>");
	}

	@Test
	@DisplayName("AUTHOR-4 and AUTHOR-5 the feed names beansprout.author, and the guest on the guest's entry")
	void feed() throws Exception {
		String atom = body("/blog/feed.xml");
		String head = atom.substring(0, atom.indexOf("<entry>"));

		assertThat(head).contains("<author><name>Ines</name></author>");
		assertThat(atom).containsOnlyOnce("<author><name>Jane Doe</name></author>");
	}

}
