package dev.danvega.beansprout.sample;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The verifier: the sample app crawled from its sitemap, with an evidence report under
 * {@code target/crawl/}. The second test proves the crawl can fail.
 *
 * <p>To see the first test go red on purpose, point it at the broken posts:
 * {@code ./mvnw -q -pl beansprout-sample -am test -Dtest=SitemapCrawlTests -Dsurefire.failIfNoSpecifiedTests=false
 * -Dcrawl.posts=classpath:test-posts-broken/}
 */
class SitemapCrawlTests {

	private static final String SITEMAP = "/blog/sitemap.xml";

	private static final Path EVIDENCE = Path.of(System.getProperty("basedir", "."), "target", "crawl");

	private static ConfigurableApplicationContext start(String posts) {
		List<String> args = new ArrayList<>(List.of("--server.port=0"));
		if (!posts.isEmpty()) {
			args.add("--beansprout.posts.location=" + posts);
		}
		return new SpringApplicationBuilder(SampleApplication.class).run(args.toArray(String[]::new));
	}

	private static String base(ConfigurableApplicationContext app) {
		return "http://localhost:" + app.getEnvironment().getProperty("local.server.port");
	}

	@Test
	@DisplayName("every sitemap URL answers 200 and every internal link and asset on those pages resolves")
	void sampleCrawl() throws Exception {
		String posts = System.getProperty("crawl.posts", "");
		try (ConfigurableApplicationContext app = start(posts)) {
			SitemapCrawl.Result result = new SitemapCrawl(base(app)).crawl(SITEMAP);
			// Evidence first, so a failing run still leaves its report behind.
			CrawlReport.write(result, EVIDENCE, posts.isEmpty() ? "the sample's own posts (classpath:posts/)" : posts);

			assertThat(result.sitemapStatus()).as(SITEMAP).isEqualTo(200);
			assertThat(result.pages()).as("URLs in the sitemap").isNotEmpty();
			assertThat(result.pages()).allSatisfy(page -> assertThat(page.status()).as(page.path()).isEqualTo(200));
			assertThat(result.failures()).as("dead internal links, see " + EVIDENCE.resolve(CrawlReport.REPORT)).isEmpty();
		}
	}

	@Test
	@DisplayName("the crawl reports a dead internal link and a missing picture in a post")
	void crawlCanFail() throws Exception {
		try (ConfigurableApplicationContext app = start("classpath:test-posts-broken/")) {
			SitemapCrawl.Result result = new SitemapCrawl(base(app)).crawl(SITEMAP);

			assertThat(result.pages()).extracting(SitemapCrawl.Page::path).contains("/blog/dead-links");
			assertThat(result.pages()).allSatisfy(page -> assertThat(page.status()).as(page.path()).isEqualTo(200));
			assertThat(result.failures()).extracting(SitemapCrawl.Failure::link)
				.containsExactlyInAnyOrder("/blog/no-such-post", "/images/no-such-picture.png");
			assertThat(result.failures()).allSatisfy(failure -> {
				assertThat(failure.page()).isEqualTo("/blog/dead-links");
				assertThat(failure.status()).isEqualTo(404);
			});
			assertThat(result.passed()).isFalse();
			// The good link on the same page still resolves, so the crawl is not just failing everything.
			assertThat(result.targets()).containsEntry("/blog/fine", 200);
		}
	}

}
