package dev.danvega.beansprout.sample;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

/** The head tags from specs/seo.md, rendered end to end. */
@SpringBootTest(properties = { "beansprout.posts.location=classpath:test-posts-rich/", "beansprout.author=Ines",
		"beansprout.photo=/images/writer.png" })
@AutoConfigureMockMvc
class SeoPagesTests {

	@Autowired
	private MockMvcTester mvc;

	private String body(String uri) throws java.io.UnsupportedEncodingException {
		var result = mvc.get().uri(uri).exchange();
		assertThat(result).hasStatusOk();
		return result.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
	}

	@Test
	@DisplayName("SEO-1 and SEO-16 the canonical link uses the request's address when no site-url is set")
	void canonical() throws Exception {
		assertThat(body("/blog/pictured")).contains("<link rel=\"canonical\" href=\"http://localhost/blog/pictured\">");
		assertThat(body("/blog/archive")).contains("<link rel=\"canonical\" href=\"http://localhost/blog/archive\">");
	}

	@Test
	@DisplayName("SEO-2 and SEO-3 a post page has its description and Open Graph tags")
	void openGraph() throws Exception {
		assertThat(body("/blog/pictured")).contains(
				"<meta name=\"description\" content=\"Shown under the title.\">",
				"<meta property=\"og:type\" content=\"article\">",
				"<meta property=\"og:title\" content=\"A pictured post\">",
				"<meta property=\"og:description\" content=\"Shown under the title.\">",
				"<meta property=\"og:url\" content=\"http://localhost/blog/pictured\">",
				"<meta property=\"og:site_name\" content=\"Beansprout Notes\">");
	}

	@Test
	@DisplayName("SEO-5 a post with a picture gets og:image and a large card, SEO-6 others get the photo")
	void images() throws Exception {
		assertThat(body("/blog/pictured")).contains("<meta property=\"og:image\" content=\"http://localhost/images/test-run.svg\">",
				"<meta name=\"twitter:card\" content=\"summary_large_image\">");
		assertThat(body("/blog/plain")).contains("<meta property=\"og:image\" content=\"http://localhost/images/writer.png\">",
				"<meta name=\"twitter:card\" content=\"summary\">");
	}

	@Test
	@DisplayName("SEO-7 a post page has its published and modified times")
	void articleTimes() throws Exception {
		assertThat(body("/blog/pictured")).contains("<meta property=\"article:published_time\" content=\"2026-03-14T00:00:00",
				"<meta property=\"article:modified_time\" content=\"2026-03-14T00:00:00");
		assertThat(body("/blog")).doesNotContain("article:published_time");
	}

	@Test
	@DisplayName("SEO-12 and SEO-14 the JSON-LD on a post page is valid JSON with the post and its author")
	void jsonLd() throws Exception {
		String html = body("/blog/pictured");
		int start = html.indexOf("<script type=\"application/ld+json\">") + "<script type=\"application/ld+json\">".length();
		String json = html.substring(start, html.indexOf("</script>", start));

		JsonNode node = JsonMapper.builder().build().readTree(json);

		assertThat(node.get("@type").asString()).isEqualTo("BlogPosting");
		assertThat(node.get("headline").asString()).isEqualTo("A pictured post");
		assertThat(node.get("image").asString()).isEqualTo("http://localhost/images/test-run.svg");
		assertThat(node.get("author").get("name").asString()).isEqualTo("Ines");
	}


	@Test
	@DisplayName("SEO-8 and SEO-11 an updated post shows Updated, and the feed and sitemap use the updated date")
	void updatedPost() throws Exception {
		assertThat(body("/blog/edited")).contains("Updated <time class=\"post-updated\" datetime=\"2026-02-20\">20 February 2026</time>",
				"<meta property=\"article:modified_time\" content=\"2026-02-20T00:00:00");
		assertThat(body("/blog/feed.xml")).contains("<updated>2026-02-20T00:00:00");
		assertThat(body("/blog/sitemap.xml")).contains("<loc>http://localhost/blog/edited</loc><lastmod>2026-02-20</lastmod>");
		assertThat(body("/blog/plain")).doesNotContain("post-updated");
	}


	@Test
	@DisplayName("SEO-3 the not-found page has no canonical link and no og:url")
	void notFoundHead() throws Exception {
		var result = mvc.get().uri("/blog/nope").exchange();
		String html = result.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);

		assertThat(result).hasStatus(404);
		assertThat(html).contains("<meta property=\"og:title\" content=\"Page not found\">")
			.doesNotContain("rel=\"canonical\"", "og:url");
	}

	@Test
	@DisplayName("SEO-7 the post page has one article:tag per tag")
	void articleTagsRendered() throws Exception {
		assertThat(body("/blog/tagged")).contains("<meta property=\"article:tag\" content=\"java\">",
				"<meta property=\"article:tag\" content=\"Spring &amp; Boot\">");
	}

	@Test
	@DisplayName("SEO-2 a post without a summary is described by its first paragraph on the page")
	void descriptionFallbackRendered() throws Exception {
		assertThat(body("/blog/plain")).contains("<meta name=\"description\" content=\"No picture, no summary.\">");
	}

}
