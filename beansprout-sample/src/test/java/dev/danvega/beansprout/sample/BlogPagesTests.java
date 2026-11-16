package dev.danvega.beansprout.sample;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;

/** The blog pages, end to end: the sample app, the starter and the Longhand theme. */
@SpringBootTest(properties = { "beansprout.posts.location=classpath:test-posts/", "beansprout.page-size=2",
		"beansprout.title=Test & Blog" })
@AutoConfigureMockMvc
class BlogPagesTests {

	@Autowired
	private MockMvcTester mvc;

	private String page(String uri) throws java.io.UnsupportedEncodingException {
		var result = mvc.get().uri(uri).exchange();
		assertThat(result).hasStatusOk().hasContentTypeCompatibleWith(MediaType.TEXT_HTML);
		return result.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
	}

	private String notFound(String uri) throws java.io.UnsupportedEncodingException {
		var result = mvc.get().uri(uri).exchange();
		assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
		return result.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
	}

	@Test
	@DisplayName("PAGES-17 the index is rendered by the Longhand theme in JTE")
	void longhandRendersTheIndex() throws Exception {
		assertThat(page("/blog")).contains("<!DOCTYPE html>", "class=\"site-header\"", "class=\"cards\"",
				"/beansprout/longhand/longhand.css");
	}

	@Test
	@DisplayName("PAGES-1 the index shows the newest posts as cards, page-size of them")
	void indexShowsNewest() throws Exception {
		assertThat(page("/blog")).contains("Hello &lt;b&gt;world&lt;/b&gt; &amp; friends", "Second post", "14 March 2026",
				"The first test post.").doesNotContain("Third post");
	}

	@Test
	@DisplayName("PAGES-2 page 2 holds the older posts and links back to the index")
	void secondPage() throws Exception {
		assertThat(page("/blog")).contains("href=\"/blog/page/2\"", "Page <span class=\"pager-page\">1</span>");
		assertThat(page("/blog/page/2")).contains("Third post", "href=\"/blog\"").doesNotContain("Second post");
	}

	@Test
	@DisplayName("PAGES-3 page 1 by number redirects permanently to the index")
	void pageOneRedirects() throws Exception {
		assertThat(mvc.get().uri("/blog/page/1")).hasStatus(HttpStatus.MOVED_PERMANENTLY).hasHeader("Location", "/blog");
		assertThat(mvc.get().uri("/blog/tags/java/page/1")).hasStatus(HttpStatus.MOVED_PERMANENTLY)
			.hasHeader("Location", "/blog/tags/java");
	}

	@Test
	@DisplayName("PAGES-4 a page number that is not a whole number in range answers 404")
	void badPageNumbers() throws Exception {
		for (String uri : new String[] { "/blog/page/3", "/blog/page/0", "/blog/page/-1", "/blog/page/abc", "/blog/page/01",
				"/blog/page/99999999999", "/blog/tags/java/page/5" }) {
			notFound(uri);
		}
	}

	@Test
	@DisplayName("PAGES-5 a post page shows the title, date, reading time, tags and body")
	void postPage() throws Exception {
		assertThat(page("/blog/hello")).contains("<h1 class=\"post-title\">Hello &lt;b&gt;world&lt;/b&gt; &amp; friends</h1>",
				"<time class=\"post-date\" datetime=\"2026-03-14\">14 March 2026</time>", "1 min read",
				"href=\"/blog/tags/spring-boot\">Spring Boot</a>", "<p>The body of hello.</p>", "<td>JTE</td>",
				"<h2 id=\"a-heading\">A heading</h2>", "href=\"#a-heading\"");
	}

	@Test
	@DisplayName("PAGES-6 a post page links to the next newer and older post, and says when there is none")
	void newerAndOlder() throws Exception {
		assertThat(page("/blog/second")).contains("href=\"/blog/hello\"", "href=\"/blog/third\"");
		assertThat(page("/blog/hello")).contains("This is the newest post.");
		assertThat(page("/blog/third")).contains("This is the oldest post.");
	}

	@Test
	@DisplayName("PAGES-7 a tag page lists that tag's posts, newest first")
	void tagPage() throws Exception {
		String java = page("/blog/tags/java");

		assertThat(java).contains("<h1 class=\"tag-title\">java</h1>", "2 posts").doesNotContain("Third post");
		assertThat(java.indexOf("Hello &lt;b&gt;")).isLessThan(java.indexOf("Second post"));
	}

	@Test
	@DisplayName("PAGES-8 two spellings of a tag are one tag page")
	void tagSpellings() throws Exception {
		assertThat(page("/blog/tags/spring-boot")).contains("<h1 class=\"tag-title\">Spring Boot</h1>", "Hello &lt;b&gt;",
				"Third post");
	}

	@Test
	@DisplayName("PAGES-9 the sidebar shows each tag with its count, most used first")
	void sidebarTags() throws Exception {
		String html = page("/blog/archive");

		assertThat(html).contains("<span class=\"tag-link-name\">java</span> <span class=\"tag-link-count\">2</span>",
				"<span class=\"tag-link-name\">Spring Boot</span> <span class=\"tag-link-count\">2</span>")
			.doesNotContain("secret");
	}

	@Test
	@DisplayName("PAGES-10 the archive lists every published post by year")
	void archivePage() throws Exception {
		assertThat(page("/blog/archive")).contains("3 posts", "since <span class=\"archive-since\">January 2026</span>",
				"<span class=\"archive-year-number\">2026</span>", "14 March", "Third post")
			.doesNotContain("Secret draft", "Future post");
	}

	@Test
	@DisplayName("PAGES-11 a missing, draft or future post, or an unknown tag, answers 404 with the theme's page")
	void notFoundPages() throws Exception {
		for (String uri : new String[] { "/blog/nope", "/blog/draft", "/blog/future", "/blog/tags/nope", "/blog/tags/secret" }) {
			assertThat(notFound(uri)).contains("There’s no page at this address", "<code class=\"panel-path\">" + uri + "</code>",
					"Second post");
		}
	}

	@Test
	@DisplayName("PAGES-13 every page shows the blog's name")
	void blogNameEverywhere() throws Exception {
		for (String uri : new String[] { "/blog", "/blog/hello", "/blog/tags/java", "/blog/archive" }) {
			assertThat(page(uri)).contains("<a class=\"site-name-link\" href=\"/blog\">Test &amp; Blog</a>");
		}
		assertThat(notFound("/blog/nope")).contains("Test &amp; Blog");
	}

	@Test
	@DisplayName("PAGES-14 every value is HTML-escaped except the post body")
	void escaping() throws Exception {
		String post = page("/blog/hello");

		assertThat(post).doesNotContain("<b>world</b>", "Test & Blog").contains("<td>JTE</td>");
		// MockMvc percent-encodes < and >, but not &, so & is what reaches the page raw.
		assertThat(notFound("/blog/tom&jerry")).contains("<code class=\"panel-path\">/blog/tom&amp;jerry</code>")
			.doesNotContain("/blog/tom&jerry<");
	}

	@Test
	@DisplayName("PAGES-19 the Longhand stylesheet is served from the theme jar")
	void stylesheet() throws Exception {
		assertThat(mvc.get().uri("/beansprout/longhand/longhand.css")).hasStatusOk()
			.hasContentTypeCompatibleWith("text/css").bodyText().contains("--accent");
	}

	@Test
	@DisplayName("FEEDS-8 every page links to the Atom feed")
	void feedLink() throws Exception {
		for (String uri : new String[] { "/blog", "/blog/hello", "/blog/tags/java", "/blog/archive" }) {
			assertThat(page(uri)).contains("<link rel=\"alternate\" type=\"application/atom+xml\" title=\"Test &amp; Blog\" href=\"/blog/feed.xml\">");
		}
	}

}
