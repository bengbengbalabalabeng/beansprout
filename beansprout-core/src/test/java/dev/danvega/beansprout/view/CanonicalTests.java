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

class CanonicalTests {

	private static final String DEV_TO = "https://dev.to/jane/testcontainers-without-the-waiting";

	private final Clock clock = Clock.fixed(Instant.parse("2026-11-16T12:00:00Z"), ZoneOffset.UTC);

	private final List<MarkdownFile> files = new ArrayList<>();

	private Posts posts() {
		return new Posts(() -> List.copyOf(files), new PostParser(ZoneOffset.UTC), clock, false);
	}

	private Pages pages(Posts posts) {
		return new Pages(posts, new Links("", "/blog"), new BlogSettings("Bean Notes", "", 10, "", ""))
			.forSite("https://example.com");
	}

	private Pages pages() {
		return pages(posts());
	}

	private void add(String slug, String frontmatter) {
		files.add(new MarkdownFile(slug + ".md", "---\ntitle: Post " + slug + "\ndate: 2026-03-14T09:30:00Z\ntags: [java]\n"
				+ frontmatter + "\n---\nThe first paragraph of " + slug + ".\n"));
	}

	@Test
	@DisplayName("CANON-1 a post's canonical is its page's canonical link")
	void canonicalLink() {
		add("moved", "canonical: " + DEV_TO);

		assertThat(pages().post("moved").orElseThrow().meta().canonical()).isEqualTo(DEV_TO);
	}

	@Test
	@DisplayName("CANON-2 a post's canonical is its page's og:url")
	void openGraphUrl() {
		add("moved", "canonical: " + DEV_TO);

		// Every theme writes meta.canonical as both the canonical link and og:url. CanonicalPagesTests
		// checks the rendered tags in all eight themes.
		assertThat(pages().post("moved").orElseThrow().meta().canonical()).isEqualTo(DEV_TO);
	}

	@Test
	@DisplayName("CANON-3 a post's canonical is the url and mainEntityOfPage of its JSON-LD")
	void jsonLd() {
		add("moved", "canonical: " + DEV_TO);

		String json = pages().post("moved").orElseThrow().meta().jsonLd();

		assertThat(json).contains("\"url\":\"" + DEV_TO + "\"", "\"mainEntityOfPage\":\"" + DEV_TO + "\"");
		assertThat(json).doesNotContain("example.com/blog/moved");
	}

	@Test
	@DisplayName("CANON-4 a post with a canonical is left out of the sitemap")
	void notInSitemap() {
		add("moved", "canonical: " + DEV_TO);
		add("home", "");
		Posts posts = posts();
		Pages pages = pages(posts);

		String sitemap = new Feeds(posts, new Links("", "/blog"), pages).sitemap("https://example.com");

		assertThat(sitemap).contains("<loc>https://example.com/blog/home</loc>").doesNotContain("/blog/moved");
		assertThat(pages.urls()).extracting(PageUrl::path).as("the static export still writes it").contains("/blog/moved");
	}

	@Test
	@DisplayName("CANON-5 a post with a canonical stays in the feed")
	void staysInFeed() {
		add("moved", "canonical: " + DEV_TO);
		Posts posts = posts();

		String feed = new Feeds(posts, new Links("", "/blog"), pages(posts)).atom("https://example.com");

		assertThat(feed).contains("<title>Post moved</title>");
	}

	@Test
	@DisplayName("CANON-10 a post with a canonical stays in the index")
	void staysInIndex() {
		add("moved", "canonical: " + DEV_TO);

		assertThat(pages().index(1).orElseThrow().latest().cards()).extracting(Card::title).contains("Post moved");
	}

	@Test
	@DisplayName("CANON-11 a post with a canonical stays on its tag pages")
	void staysOnTagPages() {
		add("moved", "canonical: " + DEV_TO);

		assertThat(pages().tag("java", 1).orElseThrow().listing().cards()).extracting(Card::title).contains("Post moved");
	}

	@Test
	@DisplayName("CANON-12 a post with a canonical stays in the archive")
	void staysInArchive() {
		add("moved", "canonical: " + DEV_TO);

		assertThat(pages().archive().archive().years().getFirst().posts()).extracting(ArchiveRow::title).contains("Post moved");
	}

	@Test
	@DisplayName("CANON-9 the view post carries its own address, not the canonical")
	void viewPostUrl() {
		add("moved", "canonical: " + DEV_TO);

		assertThat(pages().post("moved").orElseThrow().post().url()).isEqualTo("/blog/moved");
	}

	@Test
	@DisplayName("CANON-6 a canonical that is not an absolute http or https address skips the post with a warning")
	void badCanonicalSkips() {
		add("relative", "canonical: /elsewhere");
		add("ftp", "canonical: ftp://example.com/a");
		add("hostless", "canonical: https://");
		add("portonly", "canonical: https://:443/x");
		add("useronly", "canonical: https://@");
		add("fine", "");
		Posts posts = posts();

		assertThat(posts.published()).extracting(post -> post.slug()).containsExactly("fine");
	}

	@Test
	@DisplayName("CANON-6 an absolute address keeps the post, a host outside ASCII included")
	void absoluteCanonicalKeeps() {
		add("idn", "canonical: https://bücher.de/post");
		add("upper", "canonical: HTTPS://Example.com/x");

		assertThat(posts().published()).extracting(post -> post.slug()).containsExactlyInAnyOrder("idn", "upper");
	}

	@Test
	@DisplayName("CANON-8 a post skipped for its canonical is logged with a warning that names it")
	void skippedCanonicalIsWarned() {
		add("relative", "canonical: /elsewhere");
		Posts posts = posts();

		// Posts logs every problem at WARNING, so the problem is the warning's text.
		assertThat(posts.problems()).singleElement().asString().contains("relative.md", "canonical", "/elsewhere");
	}

	@Test
	@DisplayName("CANON-7 a post with no canonical keeps its own address everywhere")
	void noCanonical() {
		add("home", "");
		Pages pages = pages();

		Meta meta = pages.post("home").orElseThrow().meta();

		assertThat(meta.canonical()).isEqualTo("https://example.com/blog/home");
		assertThat(meta.jsonLd()).contains("\"url\":\"https://example.com/blog/home\"");
		assertThat(pages.urls()).extracting(PageUrl::path).contains("/blog/home");
	}

}
