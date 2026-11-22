package dev.danvega.beansprout.view;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import dev.danvega.beansprout.content.MarkdownFile;
import dev.danvega.beansprout.content.PostParser;
import dev.danvega.beansprout.content.Posts;
import dev.danvega.beansprout.feed.Feeds;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** A post's own {@code author}, from specs/post-author.md. */
class PostAuthorTests {

	private final Clock clock = Clock.fixed(Instant.parse("2026-11-16T12:00:00Z"), ZoneOffset.UTC);

	private final List<MarkdownFile> files = new ArrayList<>();

	private Posts posts() {
		return new Posts(() -> List.copyOf(files), new PostParser(ZoneOffset.UTC), clock, false);
	}

	private Pages pages(String author) {
		return new Pages(posts(), new Links("", "/blog"), new BlogSettings("Bean Notes", "", 10, author, ""))
			.forSite("https://example.com");
	}

	private Feeds feeds(String author) {
		Posts posts = posts();
		Links links = new Links("", "/blog");
		return new Feeds(posts, links, new Pages(posts, links, new BlogSettings("Bean Notes", "", 10, author, "")));
	}

	private void add(String slug, String frontmatter) {
		files.add(new MarkdownFile(slug + ".md", "---\ntitle: Post " + slug + "\ndate: 2026-01-01\n" + frontmatter
				+ "\n---\nThe body of " + slug + ".\n"));
	}

	private String jsonLd(String author, String slug) {
		return pages(author).post(slug).orElseThrow().meta().jsonLd();
	}

	@Test
	@DisplayName("AUTHOR-1 a post's author is shown on its page in place of beansprout.author")
	void shownOnThePage() {
		add("guest", "author: Jane Doe");
		add("quoted", "author: \"Ana O'Neil\"");
		files.add(new MarkdownFile("hugo.md", "+++\ntitle = \"Hugo\"\ndate = 2026-01-02\nauthor = \"Kai Ito\"\n+++\nBody.\n"));

		assertThat(pages("Ines Kovač").post("guest").orElseThrow().post().author()).isEqualTo("Jane Doe");
		assertThat(pages("").post("guest").orElseThrow().post().author()).isEqualTo("Jane Doe");
		assertThat(pages("Ines Kovač").post("quoted").orElseThrow().post().author()).isEqualTo("Ana O'Neil");
		assertThat(pages("Ines Kovač").post("hugo").orElseThrow().post().author()).isEqualTo("Kai Ito");
	}

	@Test
	@DisplayName("AUTHOR-2 a post's author is the name of its JSON-LD Person")
	void jsonLdName() {
		add("guest", "author: Jane Doe");

		assertThat(jsonLd("Ines Kovač", "guest")).contains("\"author\":{\"@type\":\"Person\",\"name\":\"Jane Doe\"")
			.doesNotContain("Ines Kovač");
		assertThat(jsonLd("", "guest")).contains("\"author\":{\"@type\":\"Person\",\"name\":\"Jane Doe\"");
	}

	@Test
	@DisplayName("AUTHOR-3 a post's author who is not beansprout.author has no url in the JSON-LD Person")
	void jsonLdNoUrl() {
		add("guest", "author: Jane Doe");
		add("own", "author: Ines Kovač");

		assertThat(jsonLd("Ines Kovač", "guest")).contains("\"author\":{\"@type\":\"Person\",\"name\":\"Jane Doe\"}");
		assertThat(jsonLd("", "guest")).contains("\"author\":{\"@type\":\"Person\",\"name\":\"Jane Doe\"}");
		assertThat(jsonLd("Ines Kovač", "own"))
			.contains("\"author\":{\"@type\":\"Person\",\"name\":\"Ines Kovač\",\"url\":\"https://example.com/blog\"}");
	}

	@Test
	@DisplayName("AUTHOR-4 a post's author is the author of its feed entry")
	void feedEntry() {
		add("guest", "author: Jane & Joe");
		add("plain", "");

		String atom = feeds("Ines").atom("https://example.com");
		String guest = entry(atom, "Post guest");
		String plain = entry(atom, "Post plain");

		assertThat(guest).contains("<author><name>Jane &amp; Joe</name></author>");
		assertThat(plain).doesNotContain("<author>");
	}

	@Test
	@DisplayName("AUTHOR-5 beansprout.author is the feed's author")
	void feedAuthor() {
		add("plain", "");

		String atom = feeds("Ines Kovač").atom("https://example.com");

		assertThat(head(atom)).contains("<author><name>Ines Kovač</name></author>").doesNotContain("<name>Bean Notes</name>");
	}

	@Test
	@DisplayName("AUTHOR-6 with no beansprout.author the blog's title is the feed's author")
	void feedAuthorTitle() {
		add("plain", "");
		add("guest", "author: Jane Doe");

		assertThat(head(feeds("").atom("https://example.com"))).contains("<author><name>Bean Notes</name></author>");
	}

	@Test
	@DisplayName("AUTHOR-7 a post with no author gets beansprout.author")
	void fallback() {
		add("plain", "");
		add("empty", "author:");
		add("blank", "author: \"  \"");

		for (String slug : List.of("plain", "empty", "blank")) {
			assertThat(pages("Ines Kovač").post(slug).orElseThrow().post().author()).as(slug).isEqualTo("Ines Kovač");
			assertThat(jsonLd("Ines Kovač", slug)).as(slug)
				.contains("\"author\":{\"@type\":\"Person\",\"name\":\"Ines Kovač\",\"url\":\"https://example.com/blog\"}");
			assertThat(pages("").post(slug).orElseThrow().post().author()).as(slug).isEmpty();
			assertThat(jsonLd("", slug)).as(slug).doesNotContain("\"author\"");
		}
	}

	@Test
	@DisplayName("AUTHOR-8 an author with indented lines under it, as jekyll-seo-tag writes it, is ignored and the post kept")
	void nested() {
		add("seo-tag", "author:\n  name: Jane Doe\n  twitter: janedoe");
		add("one-line", "author: Jane Doe\n  picture: /jane.png");
		files.add(new MarkdownFile("table.md",
				"+++\ntitle = \"Table\"\ndate = 2026-01-02\nauthor = { name = \"Jane\" }\n+++\nBody.\n"));
		Pages pages = pages("Ines Kovač");

		assertThat(pages.post("seo-tag")).hasValueSatisfying(page -> assertThat(page.post().author()).isEqualTo("Ines Kovač"));
		assertThat(pages.post("one-line")).hasValueSatisfying(page -> assertThat(page.post().author()).isEqualTo("Ines Kovač"));
		assertThat(pages.post("table")).hasValueSatisfying(page -> assertThat(page.post().author()).isEqualTo("Ines Kovač"));
		assertThat(posts().problems()).isEmpty();
	}

	/** The feed before its first entry. */
	private static String head(String atom) {
		return atom.substring(0, atom.indexOf("<entry>"));
	}

	/** The entry whose title is given. */
	private static String entry(String atom, String title) {
		int start = atom.indexOf("<title>" + title + "</title>");
		return atom.substring(atom.lastIndexOf("<entry>", start), atom.indexOf("</entry>", start));
	}

}
