package dev.danvega.beansprout.sample;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import dev.danvega.beansprout.web.ThemeRenderer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import static org.assertj.core.api.Assertions.assertThat;

/** ETags, 304s and Cache-Control on every blog response. */
@SpringBootTest(properties = "beansprout.posts.location=classpath:test-posts/")
@AutoConfigureMockMvc
class CachingTests {

	/** One of each blog response: index, archive, tag page, post page, feed, sitemap and robots.txt. */
	static final List<String> RESPONSES = List.of("/blog", "/blog/archive", "/blog/tags/java", "/blog/hello",
			"/blog/feed.xml", "/blog/sitemap.xml", "/robots.txt");

	static final AtomicInteger RENDERS = new AtomicInteger();

	@Autowired
	private MockMvcTester mvc;

	private MvcTestResult get(String path) {
		return mvc.get().uri(path).exchange();
	}

	private MvcTestResult get(String path, String ifNoneMatch) {
		return mvc.get().uri(path).header("If-None-Match", ifNoneMatch).exchange();
	}

	private String etag(String path) {
		MvcTestResult result = get(path);
		assertThat(result).hasStatusOk();
		String etag = result.getResponse().getHeader("ETag");
		assertThat(etag).as("ETag of " + path).isNotNull();
		return etag;
	}

	@Test
	@DisplayName("CACHE-1 every blog response with status 200 has a weak ETag")
	void weakEtag() {
		for (String path : RESPONSES) {
			assertThat(etag(path)).as(path).matches("W/\"[0-9a-f]{16,}\"");
		}
	}

	@Test
	@DisplayName("CACHE-2 a request whose If-None-Match holds the current ETag gets 304 with no body")
	void notModified() {
		for (String path : RESPONSES) {
			MvcTestResult result = get(path, etag(path));
			assertThat(result).as(path).hasStatus(304);
			assertThat(result.getResponse().getContentAsByteArray()).as(path).isEmpty();
		}
	}

	@Test
	@DisplayName("CACHE-2 an If-None-Match that does not match gets the full page")
	void modified() {
		for (String path : RESPONSES) {
			assertThat(get(path, "W/\"0000000000000000\"")).as(path).hasStatusOk();
		}
	}

	@Test
	@DisplayName("CACHE-3 a 304 has the same ETag as the 200")
	void notModifiedEtag() {
		for (String path : RESPONSES) {
			String etag = etag(path);
			assertThat(get(path, etag).getResponse().getHeader("ETag")).as(path).isEqualTo(etag);
		}
	}

	@Test
	@DisplayName("CACHE-12 a 304 has the same Cache-Control as the 200")
	void notModifiedCacheControl() {
		for (String path : RESPONSES) {
			MvcTestResult ok = get(path);
			assertThat(ok.getResponse().getHeader("Cache-Control")).as(path).isNotNull();
			MvcTestResult notModified = get(path, ok.getResponse().getHeader("ETag"));
			assertThat(notModified).as(path).hasStatus(304);
			assertThat(notModified.getResponse().getHeader("Cache-Control")).as(path)
				.isEqualTo(ok.getResponse().getHeader("Cache-Control"));
		}
	}

	@Test
	@DisplayName("CACHE-4 a theme page answered with 304 is not rendered")
	void noRenderOn304() {
		for (String path : List.of("/blog", "/blog/archive", "/blog/tags/java", "/blog/hello")) {
			String etag = etag(path);
			int before = RENDERS.get();
			assertThat(get(path, etag)).as(path).hasStatus(304);
			assertThat(RENDERS.get()).as("renders for a 304 of " + path).isEqualTo(before);
		}
		int before = RENDERS.get();
		etag("/blog/hello");
		assertThat(RENDERS.get()).as("a 200 still renders").isEqualTo(before + 1);
	}

	@Test
	@DisplayName("CACHE-5 when nothing has changed, a page has the same ETag on every request")
	void stable() {
		for (String path : RESPONSES) {
			assertThat(etag(path)).as(path).isEqualTo(etag(path));
		}
	}

	@Test
	@DisplayName("CACHE-9 without beansprout.site-url, the same path has a different ETag for each host")
	void perHost() {
		for (String path : RESPONSES) {
			assertThat(etag("http://one.example" + path)).as(path).isNotEqualTo(etag("http://two.example" + path));
		}
	}

	@Test
	@DisplayName("CACHE-10 every blog response has Cache-Control: no-cache")
	void noCache() {
		for (String path : RESPONSES) {
			assertThat(get(path).getResponse().getHeader("Cache-Control")).as(path).isEqualTo("no-cache");
		}
	}

	@Test
	@DisplayName("CACHE-11 a 404 and a redirect have no ETag")
	void noEtagOnErrorsOrRedirects() {
		MvcTestResult missing = get("/blog/no-such-post");
		assertThat(missing).hasStatus(404);
		assertThat(missing.getResponse().getHeader("ETag")).isNull();
		MvcTestResult redirect = get("/blog/page/1");
		assertThat(redirect).hasStatus(301);
		assertThat(redirect.getResponse().getHeader("ETag")).isNull();
	}

	/** Counts theme renders, so a test can tell a 304 skipped the theme. */
	@TestConfiguration
	static class CountRenders {

		@Bean
		static BeanPostProcessor countThemeRenders() {
			return new BeanPostProcessor() {
				@Override
				public Object postProcessAfterInitialization(Object bean, String name) {
					if (bean instanceof ThemeRenderer theme) {
						return (ThemeRenderer) (template, page) -> {
							RENDERS.incrementAndGet();
							return theme.render(template, page);
						};
					}
					return bean;
				}
			};
		}

	}

}
