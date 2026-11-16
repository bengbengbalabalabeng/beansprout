package dev.danvega.beansprout.content;

import java.time.ZoneOffset;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/** Frontmatter shapes found by the adversarial review. */
class FrontmatterEdgeTests {

	private final PostParser parser = new PostParser(ZoneOffset.UTC);

	private Post parse(String frontmatter) {
		return parser.parse(new MarkdownFile("a.md", "---\ntitle: A\ndate: 2026-01-01\n" + frontmatter + "\n---\nBody.\n"));
	}

	@Test
	@DisplayName("POSTS-8 a quoted tag may hold a comma")
	void quotedCommaInATag() {
		assertThat(parse("tags: [\"Spring, Boot\", 'a, b', java]").tags()).containsExactly("Spring, Boot", "a, b", "java");
	}

	@Test
	@DisplayName("POSTS-8 an apostrophe in a plain value does not hide a later comment")
	void apostropheThenComment() {
		Post post = parser.parse(new MarkdownFile("a.md",
				"---\ntitle: It's a test   # comment\ndate: 2026-01-01\ntags: [it's, java] # x\n---\n"));

		assertThat(post.title()).isEqualTo("It's a test");
		assertThat(post.tags()).containsExactly("it's", "java");
	}

	@Test
	@DisplayName("POSTS-18 an empty draft value or a list is refused")
	void emptyOrListDraft() {
		assertThatExceptionOfType(InvalidPostException.class).isThrownBy(() -> parse("draft:")).withMessageContaining("draft");
		assertThatExceptionOfType(InvalidPostException.class).isThrownBy(() -> parse("draft: [true]"))
			.withMessageContaining("draft");
		assertThat(parse("draft: false").draft()).isFalse();
	}

	@Test
	@DisplayName("POSTS-10 a cut summary never splits a character in two")
	void cutKeepsWholeCharacters() {
		String emoji = "🌱".repeat(150);

		Post post = parser.parse(new MarkdownFile("a.md", "---\ntitle: A\ndate: 2026-01-01\n---\n" + emoji + "\n"));

		String summary = post.summary();
		assertThat(summary).hasSizeLessThanOrEqualTo(200).endsWith("…");
		assertThat(Character.isHighSurrogate(summary.charAt(summary.length() - 2))).isFalse();
	}

}
