package dev.danvega.beansprout.sample;

import java.io.IOException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;

/** The sample's own posts, with the Longhand theme. */
@SpringBootTest
@AutoConfigureMockMvc
class PostBundleTests {

	private static final String FOLDER = "posts/2026/posts-with-their-pictures/";

	@Autowired
	private MockMvcTester mvc;

	@Test
	@DisplayName("BUNDLE-20 the sample serves a bundle post with its picture and its body image")
	void bundlePost() throws IOException {
		assertThat(mvc.get().uri("/blog/posts-with-their-pictures")).hasStatusOk()
			.bodyText()
			.contains("og:image\" content=\"http://localhost/blog/posts-with-their-pictures/cover.png\"")
			.contains("src=\"/blog/posts-with-their-pictures/cover.png\"")
			.contains("src=\"/blog/posts-with-their-pictures/sprout.png\"");
		assertThat(mvc.get().uri("/blog/posts-with-their-pictures/cover.png")).hasStatusOk()
			.hasContentType("image/png")
			.body()
			.isEqualTo(new ClassPathResource(FOLDER + "cover.png").getContentAsByteArray());
		assertThat(mvc.get().uri("/blog/posts-with-their-pictures/sprout.png")).hasStatusOk()
			.hasContentType("image/png")
			.body()
			.isEqualTo(new ClassPathResource(FOLDER + "sprout.png").getContentAsByteArray());
	}

}
