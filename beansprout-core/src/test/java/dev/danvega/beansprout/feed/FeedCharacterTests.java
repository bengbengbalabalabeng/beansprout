package dev.danvega.beansprout.feed;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FeedCharacterTests {

	@Test
	@DisplayName("FEEDS-7 characters XML 1.0 forbids are dropped, and valid pairs are kept")
	void forbiddenCharactersAreDropped() {
		assertThat(Feeds.escape("a\u0001b￾c￿d\uD800e")).isEqualTo("abcde");
		assertThat(Feeds.escape("bean 🌱\tok")).isEqualTo("bean 🌱\tok");
	}

}
