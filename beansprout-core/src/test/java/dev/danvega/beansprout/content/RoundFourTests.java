package dev.danvega.beansprout.content;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Inputs found by the fourth adversarial review. */
class RoundFourTests {

	private final PostParser parser = new PostParser(ZoneOffset.UTC);

	private Post parse(String frontmatter, String body) {
		return parser.parse(new MarkdownFile("hello.md", "---\n" + frontmatter + "\n---\n" + body));
	}

	@Test
	@DisplayName("POSTS-1 a value with a pasted line separator is still read")
	void lineSeparatorInAValue() {
		Post post = parse("title: Hello World\ndate: 2026-11-16\ntags:\n  - a\u0085b", "");

		assertThat(post.title()).isEqualTo("Hello World");
		assertThat(post.tags()).containsExactly("a\u0085b");
	}

	@Test
	@DisplayName("POSTS-8 a doubled apostrophe inside single quotes is an apostrophe, not the end of the value")
	void doubledApostrophe() {
		assertThat(parse("title: 'Don''t # panic'   # a real comment\ndate: 2026-11-16", "").title()).isEqualTo("Don't # panic");
		assertThat(parse("title: A\ndate: 2026-11-16\ntags: ['it''s, x', b]", "").tags()).containsExactly("it's, x", "b");
	}

	@Test
	@DisplayName("POSTS-7 an explicit empty slug is skipped with a problem, not replaced by the file name")
	void emptySlugIsSkipped() {
		Clock clock = Clock.fixed(Instant.parse("2026-11-16T12:00:00Z"), ZoneOffset.UTC);
		Posts posts = new Posts(() -> List.of(
				new MarkdownFile("hello.md", "---\ntitle: A\ndate: 2026-01-01\nslug: \"\"\n---\n"),
				new MarkdownFile("other.md", "---\ntitle: B\ndate: 2026-01-01\nslug:\n---\n")),
				parser, clock, false);

		assertThat(posts.published()).isEmpty();
		assertThat(posts.problems()).hasSize(2).allSatisfy(problem -> assertThat(problem).contains("slug is empty"));
	}

	@Test
	@DisplayName("POSTS-11 a post with no words still reads in one minute")
	void emptyBodyIsOneMinute() {
		assertThat(parse("title: A\ndate: 2026-11-16", "").readingMinutes()).isEqualTo(1);
	}

}
