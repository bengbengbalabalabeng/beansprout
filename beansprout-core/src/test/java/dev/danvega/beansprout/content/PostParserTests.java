package dev.danvega.beansprout.content;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class PostParserTests {

	private static final ZoneId AMSTERDAM = ZoneId.of("Europe/Amsterdam");

	private final PostParser parser = new PostParser(AMSTERDAM);

	private Post parse(String path, String text) {
		return parser.parse(new MarkdownFile(path, text));
	}

	@Test
	@DisplayName("POSTS-1 a file with title and date frontmatter becomes a post")
	void titleAndDateMakeAPost() {
		Post post = parse("hello.md", """
				---
				title: Hello, Beansprout
				date: 2026-11-16
				---
				The first post.
				""");

		assertThat(post.title()).isEqualTo("Hello, Beansprout");
		assertThat(post.date().toLocalDate()).hasToString("2026-11-16");
		assertThat(post.draft()).isFalse();
		assertThat(post.path()).isEqualTo("hello.md");
	}

	@Test
	@DisplayName("POSTS-2 the body is rendered from Markdown to HTML, tables included")
	void bodyIsHtml() {
		Post post = parse("hello.md", """
				---
				title: Tables
				date: 2026-11-16
				---
				Some *emphasis* and `code`.

				| Engine | Fast |
				| --- | --- |
				| JTE | yes |
				""");

		assertThat(post.html()).contains("<em>emphasis</em>", "<code>code</code>", "<table>", "<td>JTE</td>");
	}

	@Test
	@DisplayName("POSTS-3 the slug comes from frontmatter, or else from the file name")
	void slugFromFrontmatterOrFileName() {
		Post named = parse("notes/2026/hello-world.md", """
				---
				title: Hello
				date: 2026-11-16
				---
				""");
		Post chosen = parse("notes/hello-world.md", """
				---
				title: Hello
				date: 2026-11-16
				slug: greetings
				---
				""");

		assertThat(named.slug()).isEqualTo("hello-world");
		assertThat(chosen.slug()).isEqualTo("greetings");
	}

	@Test
	@DisplayName("POSTS-4 a slug becomes lowercase letters and digits joined by single dashes, accents removed")
	void slugsAreClean() {
		assertThat(Slugs.slugify("Héllo, Wörld!  Again")).isEqualTo("hello-world-again");
		assertThat(Slugs.slugify("--Spring Boot 4.1--")).isEqualTo("spring-boot-4-1");
		assertThat(Slugs.slugify("日本語")).isEmpty();

		Post post = parse("My First Post.md", """
				---
				title: Hello
				date: 2026-11-16
				---
				""");
		assertThat(post.slug()).isEqualTo("my-first-post");
	}

	@Test
	@DisplayName("POSTS-5 a file without frontmatter, title or a readable date is refused with a reason")
	void badFilesAreRefused() {
		assertThatExceptionOfType(InvalidPostException.class)
			.isThrownBy(() -> parse("plain.md", "Just text.\n"))
			.withMessageContaining("frontmatter");
		assertThatExceptionOfType(InvalidPostException.class)
			.isThrownBy(() -> parse("untitled.md", "---\ndate: 2026-11-16\n---\n"))
			.withMessageContaining("title");
		assertThatExceptionOfType(InvalidPostException.class)
			.isThrownBy(() -> parse("undated.md", "---\ntitle: Hi\n---\n"))
			.withMessageContaining("date");
		assertThatExceptionOfType(InvalidPostException.class)
			.isThrownBy(() -> parse("baddate.md", "---\ntitle: Hi\ndate: next tuesday\n---\n"))
			.withMessageContaining("next tuesday");
		assertThatExceptionOfType(InvalidPostException.class)
			.isThrownBy(() -> parse("open.md", "---\ntitle: Hi\ndate: 2026-11-16\n"))
			.withMessageContaining("closing ---");
	}

	@Test
	@DisplayName("POSTS-8 tags are read as [a, b] or as one - item per line")
	void tagsInBothShapes() {
		Post inline = parse("a.md", """
				---
				title: A
				date: 2026-11-16
				tags: [spring, "Spring Boot", 'java']
				---
				""");
		Post block = parse("b.md", """
				---
				title: B
				date: 2026-11-16
				tags:
				  - spring
				  - "Spring Boot"
				- java
				---
				""");

		assertThat(inline.tags()).containsExactly("spring", "Spring Boot", "java");
		assertThat(block.tags()).containsExactly("spring", "Spring Boot", "java");
	}

	@Test
	@DisplayName("POSTS-8 quoted values, comments and unknown keys are handled")
	void quotesAndComments() {
		Post post = parse("a.md", """
				---
				# a comment line
				title: "Colons: they work"   # trailing comment
				date: '2026-11-16'
				mood: happy
				tags: spring
				---
				""");

		assertThat(post.title()).isEqualTo("Colons: they work");
		assertThat(post.tags()).containsExactly("spring");
	}

	@Test
	@DisplayName("POSTS-10 without a summary, the first paragraph is used, cut at a word to 200 characters")
	void summaryFromFirstParagraph() {
		String longParagraph = "word ".repeat(80).strip();
		Post cut = parse("a.md", "---\ntitle: A\ndate: 2026-11-16\n---\n" + longParagraph + "\n\nSecond paragraph.\n");
		Post shortOne = parse("b.md", "---\ntitle: B\ndate: 2026-11-16\n---\n# Heading\n\nA *short* first paragraph.\n");
		Post given = parse("c.md", "---\ntitle: C\ndate: 2026-11-16\nsummary: Chosen by hand.\n---\nIgnored.\n");

		assertThat(cut.summary()).hasSizeLessThanOrEqualTo(200).endsWith("word…").doesNotContain("Second");
		assertThat(shortOne.summary()).isEqualTo("A short first paragraph.");
		assertThat(given.summary()).isEqualTo("Chosen by hand.");
	}

	@Test
	@DisplayName("POSTS-11 reading time is words divided by 200, rounded up, at least one minute")
	void readingTime() {
		Post shortOne = parse("a.md", "---\ntitle: A\ndate: 2026-11-16\n---\nTwo words.\n");
		Post longOne = parse("b.md", "---\ntitle: B\ndate: 2026-11-16\n---\n" + "word ".repeat(450) + "\n");
		Post exact = parse("c.md", "---\ntitle: C\ndate: 2026-11-16\n---\n" + "word ".repeat(400) + "\n");

		assertThat(shortOne.readingMinutes()).isEqualTo(1);
		assertThat(longOne.readingMinutes()).isEqualTo(3);
		assertThat(exact.readingMinutes()).isEqualTo(2);
	}

	@Test
	@DisplayName("POSTS-12 each ## heading gets an id from its text, listed in order")
	void headingsGetIds() {
		Post post = parse("a.md", """
				---
				title: A
				date: 2026-11-16
				---
				## Why it *matters*

				### Not listed

				## Why it matters

				## `Code` too
				""");

		assertThat(post.headings()).extracting(Heading::id)
			.containsExactly("why-it-matters", "why-it-matters-2", "code-too");
		assertThat(post.headings()).extracting(Heading::text)
			.containsExactly("Why it matters", "Why it matters", "Code too");
		assertThat(post.html()).contains("<h2 id=\"why-it-matters\">", "<h2 id=\"why-it-matters-2\">");
	}

	@Test
	@DisplayName("POSTS-15 a date without a time starts at midnight in the blog's zone, and an offset is converted")
	void datesUseTheZone() {
		Post dayOnly = parse("a.md", "---\ntitle: A\ndate: 2026-11-16\n---\n");
		Post withTime = parse("b.md", "---\ntitle: B\ndate: 2026-11-16 09:30\n---\n");
		Post withOffset = parse("c.md", "---\ntitle: C\ndate: 2026-11-16T09:30:00Z\n---\n");

		assertThat(dayOnly.date()).isEqualTo(ZonedDateTime.of(2026, 11, 16, 0, 0, 0, 0, AMSTERDAM));
		assertThat(withTime.date()).isEqualTo(ZonedDateTime.of(2026, 11, 16, 9, 30, 0, 0, AMSTERDAM));
		assertThat(withOffset.date()).isEqualTo(ZonedDateTime.of(2026, 11, 16, 10, 30, 0, 0, AMSTERDAM));
	}

	@Test
	@DisplayName("POSTS-16 draft: true marks the post as a draft")
	void draftIsRead() {
		Post post = parse("a.md", "---\ntitle: A\ndate: 2026-11-16\ndraft: true\n---\n");

		assertThat(post.draft()).isTrue();
	}

	@Test
	@DisplayName("POSTS-18 a draft value other than true or false is refused")
	void oddDraftIsRefused() {
		assertThatExceptionOfType(InvalidPostException.class)
			.isThrownBy(() -> parse("a.md", "---\ntitle: A\ndate: 2026-11-16\ndraft: maybe\n---\n"))
			.withMessageContaining("draft");
	}

}
