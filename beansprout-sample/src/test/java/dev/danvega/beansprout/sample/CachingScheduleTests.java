package dev.danvega.beansprout.sample;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import dev.danvega.beansprout.content.PostParser;
import dev.danvega.beansprout.content.PostSource;
import dev.danvega.beansprout.content.Posts;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import static org.assertj.core.api.Assertions.assertThat;

/** A scheduled post going live changes the ETags of the pages that list it, with no restart. */
@SpringBootTest(properties = "beansprout.posts.location=classpath:test-posts-caching/")
@AutoConfigureMockMvc
class CachingScheduleTests {

	/** later.md is dated 2026-08-01. The clock starts a month before, in the same year. */
	static final MovableClock CLOCK = new MovableClock(Instant.parse("2026-07-01T12:00:00Z"));

	static final List<String> LISTINGS = List.of("/blog", "/blog/archive", "/blog/tags/java", "/blog/feed.xml",
			"/blog/sitemap.xml");

	@Autowired
	private MockMvcTester mvc;

	private Map<String, String> etags() {
		Map<String, String> etags = new LinkedHashMap<>();
		for (String path : LISTINGS) {
			MvcTestResult result = mvc.get().uri(path).exchange();
			assertThat(result).as(path).hasStatusOk();
			etags.put(path, result.getResponse().getHeader("ETag"));
		}
		return etags;
	}

	@Test
	@DisplayName("CACHE-7 when a scheduled post's time passes, the listings get new ETags with no restart")
	void goingLiveChangesEtags() {
		Map<String, String> before = etags();
		assertThat(mvc.get().uri("/blog/later")).hasStatus(404);

		CLOCK.advance(Duration.ofDays(1));
		assertThat(etags()).as("a day later, still before the post's date").isEqualTo(before);

		CLOCK.advance(Duration.ofDays(31));
		assertThat(mvc.get().uri("/blog/later")).hasStatusOk();
		Map<String, String> after = etags();
		for (String path : LISTINGS) {
			assertThat(after.get(path)).as(path).isNotEqualTo(before.get(path));
		}
	}

	/** The app's own Posts, read with a clock the test can move. */
	@TestConfiguration
	static class MovedClock {

		@Bean
		Posts posts(PostSource source) {
			return new Posts(source, new PostParser(ZoneOffset.UTC), CLOCK, false);
		}

	}

	/** A clock a test can move forward. */
	static final class MovableClock extends Clock {

		private volatile Instant now;

		MovableClock(Instant now) {
			this.now = now;
		}

		void advance(Duration duration) {
			now = now.plus(duration);
		}

		@Override
		public ZoneId getZone() {
			return ZoneOffset.UTC;
		}

		@Override
		public Clock withZone(ZoneId zone) {
			return this;
		}

		@Override
		public Instant instant() {
			return now;
		}

	}

}
