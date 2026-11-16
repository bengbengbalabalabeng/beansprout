package dev.danvega.beansprout.sample;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;

/** The feed and the sitemap, served by the sample app. */
class FeedRoutesTests {

	@Nested
	@SpringBootTest(properties = "beansprout.posts.location=classpath:test-posts/")
	@AutoConfigureMockMvc
	class FromTheRequest {

		@Autowired
		private MockMvcTester mvc;

		@Test
		@DisplayName("FEEDS-1 the Atom feed is served at /blog/feed.xml as application/atom+xml")
		void feedRoute() {
			assertThat(mvc.get().uri("/blog/feed.xml")).hasStatusOk()
				.hasContentTypeCompatibleWith("application/atom+xml").bodyText()
				.contains("<feed xmlns=\"http://www.w3.org/2005/Atom\">", "Second post", "Third post");
		}

		@Test
		@DisplayName("FEEDS-3 without beansprout.site-url, addresses use the request's scheme, host and port")
		void addressFromRequest() {
			assertThat(mvc.get().uri("http://blog.example:8080/blog/feed.xml")).bodyText()
				.contains("<id>http://blog.example:8080/blog/hello</id>");
		}

		@Test
		@DisplayName("FEEDS-4 the sitemap is served at /blog/sitemap.xml with every page")
		void sitemapRoute() {
			assertThat(mvc.get().uri("/blog/sitemap.xml")).hasStatusOk().hasContentTypeCompatibleWith("application/xml")
				.bodyText()
				.contains("<loc>http://localhost/blog</loc>", "<loc>http://localhost/blog/hello</loc>",
						"<loc>http://localhost/blog/tags/spring-boot</loc>", "<loc>http://localhost/blog/archive</loc>");
		}

		@Test
		@DisplayName("FEEDS-6 drafts and future posts are not in the served feed or sitemap")
		void hiddenPosts() {
			assertThat(mvc.get().uri("/blog/feed.xml")).bodyText().doesNotContain("Secret draft", "Future post");
			assertThat(mvc.get().uri("/blog/sitemap.xml")).bodyText().doesNotContain("/blog/draft", "/blog/future");
		}

	}

	@Nested
	@SpringBootTest(properties = { "beansprout.posts.location=classpath:test-posts/",
			"beansprout.site-url=https://beans.example.com" })
	@AutoConfigureMockMvc
	class FromTheProperty {

		@Autowired
		private MockMvcTester mvc;

		@Test
		@DisplayName("FEEDS-3 with beansprout.site-url set, addresses use it")
		void addressFromProperty() {
			assertThat(mvc.get().uri("/blog/feed.xml")).bodyText()
				.contains("<id>https://beans.example.com/blog/hello</id>", "href=\"https://beans.example.com/blog/feed.xml\"");
			assertThat(mvc.get().uri("/blog/sitemap.xml")).bodyText().contains("<loc>https://beans.example.com/blog/hello</loc>");
		}

	}

}
