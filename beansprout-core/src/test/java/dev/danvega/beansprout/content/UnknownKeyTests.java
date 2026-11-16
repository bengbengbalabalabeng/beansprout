package dev.danvega.beansprout.content;

import java.time.ZoneOffset;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Unknown keys of every shape, from the third adversarial review. */
class UnknownKeyTests {

	private final PostParser parser = new PostParser(ZoneOffset.UTC);

	private Post parse(String text) {
		return parser.parse(new MarkdownFile("a.md", text));
	}

	@Test
	@DisplayName("POSTS-1 an unknown key with brackets in its value is ignored")
	void bracketsUnderAnUnknownKey() {
		Post post = parse("---\ntitle: Hi\ndate: 2026-11-16\nstatus: [WIP] needs review\nnote: [WIP\n---\n");

		assertThat(post.title()).isEqualTo("Hi");
	}

	@Test
	@DisplayName("POSTS-1 nested data under an unknown key is ignored, even when it reuses a known key's name")
	void nestedKnownNamesUnderAnUnknownKey() {
		Post post = parse("---\ntitle: Hi\nimage:\n  title: pic\n  date: x\ndate: 2026-11-16\n---\n");

		assertThat(post.title()).isEqualTo("Hi");
		assertThat(post.date().toLocalDate()).hasToString("2026-11-16");
	}

	@Test
	@DisplayName("POSTS-1 a title may start with a bracket")
	void titleWithBrackets() {
		assertThat(parse("---\ntitle: [WIP] Notes [part 2]\ndate: 2026-11-16\n---\n").title()).isEqualTo("[WIP] Notes [part 2]");
	}

	@Test
	@DisplayName("POSTS-1 a file saved with a byte order mark and Windows line endings is read")
	void bomAndCrlf() {
		Post post = parse("﻿---\r\ntitle: Windows\r\ndate: 2026-11-16\r\ntags: [a, b]\r\n---\r\nBody.\r\n");

		assertThat(post.title()).isEqualTo("Windows");
		assertThat(post.tags()).containsExactly("a", "b");
	}

}
