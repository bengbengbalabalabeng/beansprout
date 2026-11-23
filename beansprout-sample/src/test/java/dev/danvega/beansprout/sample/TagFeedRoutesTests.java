package dev.danvega.beansprout.sample;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import static org.assertj.core.api.Assertions.assertThat;

/** A tag's feed at its own address, from specs/tag-feeds.md. */
@SpringBootTest(properties = { "beansprout.posts.location=classpath:test-posts-tagfeeds/", "beansprout.site-url=https://example.com" })
@AutoConfigureMockMvc
class TagFeedRoutesTests {

	@Autowired
	private MockMvcTester mvc;

	private MvcTestResult get(String uri) {
		return mvc.get().uri(uri).exchange();
	}

	@Test
	@DisplayName("TAGFEED-1 each tag's Atom feed is served at /blog/tags/{tag}/feed.xml")
	void served() throws Exception {
		for (String tag : new String[] { "java", "spring-boot", "kotlin" }) {
			MvcTestResult result = get("/blog/tags/" + tag + "/feed.xml");
			assertThat(result).as(tag).hasStatusOk();
			assertThat(result.getResponse().getContentType()).as(tag).startsWith("application/atom+xml");
			assertThat(result.getResponse().getContentAsString(StandardCharsets.UTF_8)).as(tag)
				.startsWith("<?xml")
				.contains("<feed xmlns=\"http://www.w3.org/2005/Atom\">")
				.contains("<link rel=\"self\" href=\"https://example.com/blog/tags/" + tag + "/feed.xml\"/>");
		}
		String java = get("/blog/tags/java/feed.xml").getResponse().getContentAsString(StandardCharsets.UTF_8);
		assertThat(java).contains("<title>Beans on the JVM</title>", "<title>More Java</title>")
			.doesNotContain("<title>Kotlin notes</title>");
	}

	@Test
	@DisplayName("TAGFEED-6 a tag no published post has answers 404 at its feed's address")
	void notFound() {
		for (String tag : new String[] { "nope", "secret", "later", "Java" }) {
			assertThat(get("/blog/tags/" + tag + "/feed.xml")).as(tag).hasStatus(HttpStatus.NOT_FOUND);
		}
	}

	@Test
	@DisplayName("TAGFEED-10 a tag's feed has an ETag and Cache-Control, and answers 304 to a matching If-None-Match")
	void caching() {
		MvcTestResult first = get("/blog/tags/java/feed.xml");
		String etag = first.getResponse().getHeader(HttpHeaders.ETAG);
		assertThat(etag).isNotBlank();
		assertThat(first.getResponse().getHeader(HttpHeaders.CACHE_CONTROL)).isEqualTo("no-cache");

		MvcTestResult again = mvc.get().uri("/blog/tags/java/feed.xml").header(HttpHeaders.IF_NONE_MATCH, etag).exchange();
		assertThat(again).hasStatus(HttpStatus.NOT_MODIFIED);
		assertThat(again.getResponse().getHeader(HttpHeaders.ETAG)).isEqualTo(etag);
		assertThat(again.getResponse().getContentAsByteArray()).isEmpty();

		String kotlin = get("/blog/tags/kotlin/feed.xml").getResponse().getHeader(HttpHeaders.ETAG);
		assertThat(kotlin).isNotEqualTo(etag);
	}

	@Test
	@DisplayName("ALIAS-1 an alias at the feed address of a tag no published post has still answers with a 301 to its post")
	void aliasAtMissingTagFeed() {
		MvcTestResult result = get("/blog/tags/retired/feed.xml");
		assertThat(result).hasStatus(HttpStatus.MOVED_PERMANENTLY);
		assertThat(result.getResponse().getHeader(HttpHeaders.LOCATION)).isEqualTo("/blog/more-java");
	}

}
