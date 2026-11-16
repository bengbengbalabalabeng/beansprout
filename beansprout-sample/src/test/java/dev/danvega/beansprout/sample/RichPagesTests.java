package dev.danvega.beansprout.sample;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;

/** The Longhand details from specs/rich-pages.md, rendered end to end. */
@SpringBootTest(properties = { "beansprout.posts.location=classpath:test-posts-rich/", "beansprout.author=Ines & Co",
		"beansprout.photo=/images/writer.png" })
@AutoConfigureMockMvc
class RichPagesTests {

	@Autowired
	private MockMvcTester mvc;

	private String body(String uri) throws java.io.UnsupportedEncodingException {
		var result = mvc.get().uri(uri).exchange();
		assertThat(result).hasStatusOk();
		return result.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
	}

	@Test
	@DisplayName("RICH-1 the card shows the post's picture")
	void cardPicture() throws Exception {
		assertThat(body("/blog")).contains("<img class=\"card-picture\" src=\"/images/test-run.svg\"");
	}

	@Test
	@DisplayName("RICH-2 the post page shows the picture above the body, with RICH-4 its caption")
	void postFigure() throws Exception {
		String html = body("/blog/pictured");

		assertThat(html).contains("<img class=\"post-figure-img\" src=\"/images/test-run.svg\"",
				"<figcaption class=\"post-figure-caption\">What four minutes looks like</figcaption>");
		assertThat(html.indexOf("post-figure-img")).isLessThan(html.indexOf("The body of the pictured post."));
		assertThat(body("/blog/plain")).doesNotContain("post-figure");
	}

	@Test
	@DisplayName("RICH-3 the alt text comes from picture-alt, escaped")
	void altText() throws Exception {
		// JTE escapes < and & in attributes, which is enough to keep markup out. > may stay as is.
		assertThat(body("/blog/pictured")).contains("alt=\"Forty &lt;containers").doesNotContain("<containers>");
		assertThat(body("/blog")).contains("<img class=\"card-picture\" src=\"/images/test-run.svg\" alt=\"Forty &lt;containers");
	}

	@Test
	@DisplayName("RICH-8 the frontmatter summary is the lede under the title")
	void lede() throws Exception {
		assertThat(body("/blog/pictured")).contains("<p class=\"post-lede\">Shown under the title.</p>");
		assertThat(body("/blog/plain")).doesNotContain("post-lede");
	}

	@Test
	@DisplayName("RICH-9 the author's name is on each post page")
	void author() throws Exception {
		assertThat(body("/blog/plain")).contains("<p class=\"post-author\">Ines &amp; Co</p>");
	}

	@Test
	@DisplayName("RICH-10 the writer's photo is above the blog's name on every page")
	void photo() throws Exception {
		for (String uri : new String[] { "/blog", "/blog/pictured", "/blog/archive" }) {
			assertThat(body(uri)).contains("<img class=\"site-avatar\" src=\"/images/writer.png\"");
		}
	}

	@Test
	@DisplayName("RICH-11 and RICH-12 the menu marks Latest on the index and Archive on the archive")
	void currentMenuItem() throws Exception {
		assertThat(body("/blog")).contains("<a class=\"nav-link\" href=\"/blog\" aria-current=\"page\">Latest</a>")
			.contains("<a class=\"nav-link\" href=\"/blog/archive\">Archive</a>");
		assertThat(body("/blog/archive")).contains("<a class=\"nav-link\" href=\"/blog/archive\" aria-current=\"page\">Archive</a>")
			.contains("<a class=\"nav-link\" href=\"/blog\">Latest</a>");
	}

}
