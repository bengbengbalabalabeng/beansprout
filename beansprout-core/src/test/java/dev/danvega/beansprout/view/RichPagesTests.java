package dev.danvega.beansprout.view;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import dev.danvega.beansprout.content.MarkdownFile;
import dev.danvega.beansprout.content.PostParser;
import dev.danvega.beansprout.content.Posts;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class RichPagesTests {

	private final Clock clock = Clock.fixed(Instant.parse("2026-11-16T12:00:00Z"), ZoneOffset.UTC);

	private final List<MarkdownFile> files = new ArrayList<>();

	private Posts posts;

	private Pages pages(String root, String author, String photo) {
		posts = new Posts(() -> List.copyOf(files), new PostParser(ZoneOffset.UTC), clock, false);
		return new Pages(posts, new Links(root, "/blog"), new BlogSettings("Notes", "", 10, author, photo));
	}

	private Pages pages() {
		return pages("", "", "");
	}

	private void add(String slug, String frontmatter) {
		files.add(new MarkdownFile(slug + ".md", "---\ntitle: Post " + slug + "\ndate: 2026-01-01\n" + frontmatter
				+ "\n---\nThe first paragraph of " + slug + ".\n"));
	}

	@Test
	@DisplayName("RICH-1 a post's picture is shown on its card")
	void cardPicture() {
		add("pic", "picture: /images/pic.svg\npicture-alt: A picture");
		add("plain", "");

		List<Card> cards = pages().index(1).orElseThrow().latest().cards();

		assertThat(cards).extracting(Card::url, Card::picture, Card::pictureAlt).containsExactlyInAnyOrder(
				org.assertj.core.groups.Tuple.tuple("/blog/pic", "/images/pic.svg", "A picture"),
				org.assertj.core.groups.Tuple.tuple("/blog/plain", "", ""));
	}

	@Test
	@DisplayName("RICH-2 a post's picture is shown above its body")
	void postPicture() {
		add("pic", "picture: /images/pic.svg");

		assertThat(pages().post("pic").orElseThrow().post().picture()).isEqualTo("/images/pic.svg");
	}

	@Test
	@DisplayName("RICH-3 the alt text comes from picture-alt, or is empty")
	void altText() {
		add("with", "picture: /a.svg\npicture-alt: Forty containers");
		add("without", "picture: /b.svg");
		Pages pages = pages();

		assertThat(pages.post("with").orElseThrow().post().pictureAlt()).isEqualTo("Forty containers");
		assertThat(pages.post("without").orElseThrow().post().pictureAlt()).isEmpty();
	}

	@Test
	@DisplayName("RICH-4 a caption is shown under the picture, and only with a picture")
	void caption() {
		add("with", "picture: /a.svg\ncaption: What four minutes looks like");
		add("orphan", "caption: No picture here");
		Pages pages = pages();

		assertThat(pages.post("with").orElseThrow().post().caption()).isEqualTo("What four minutes looks like");
		assertThat(pages.post("orphan").orElseThrow().post().caption()).isEmpty();
	}

	@Test
	@DisplayName("RICH-5 a picture path starting with / gets the app's context path")
	void contextPath() {
		add("pic", "picture: /images/pic.svg");

		Pages pages = pages("/app", "", "/images/me.png");

		assertThat(pages.post("pic").orElseThrow().post().picture()).isEqualTo("/app/images/pic.svg");
		assertThat(pages.index(1).orElseThrow().site().photo()).isEqualTo("/app/images/me.png");
	}

	@Test
	@DisplayName("RICH-6 a full http or https address is used as written")
	void fullAddress() {
		add("secure", "picture: https://cdn.example.com/a.png");
		add("plain", "picture: http://cdn.example.com/b.png");

		Pages pages = pages("/app", "", "");

		assertThat(pages.post("secure").orElseThrow().post().picture()).isEqualTo("https://cdn.example.com/a.png");
		assertThat(pages.post("plain").orElseThrow().post().picture()).isEqualTo("http://cdn.example.com/b.png");
	}

	@Test
	@DisplayName("RICH-7 any other picture value skips the post with a problem that names it")
	void badImage() {
		add("relative", "picture: images/a.png");
		add("script", "picture: javascript:alert(1)");
		add("fine", "");

		Pages pages = pages();

		assertThat(posts.published()).extracting(p -> p.slug()).containsExactly("fine");
		assertThat(posts.problems()).hasSize(2)
			.anySatisfy(problem -> assertThat(problem).contains("relative.md", "picture"))
			.anySatisfy(problem -> assertThat(problem).contains("script.md", "picture"));
		assertThat(pages.post("relative")).isEmpty();
	}

	@Test
	@DisplayName("RICH-7 a backslash after the first slash, or an address with no host, is refused")
	void sneakyAddresses() {
		add("backslash", "picture: /\\evil.example/x.png");
		add("nohost", "picture: https://");
		add("fine", "");

		pages();

		assertThat(posts.published()).extracting(p -> p.slug()).containsExactly("fine");
		assertThatIllegalArgumentException().isThrownBy(() -> new BlogSettings("B", "", 10, "", "/\\evil.example/me.png"));
	}

	@Test
	@DisplayName("RICH-5 a card's picture gets the context path too")
	void cardContextPath() {
		add("pic", "picture: /images/pic.svg");

		assertThat(pages("/app", "", "").index(1).orElseThrow().latest().cards().getFirst().picture())
			.isEqualTo("/app/images/pic.svg");
	}

	@Test
	@DisplayName("RICH-8 a summary written in frontmatter is the lede, a summary taken from the body is not")
	void lede() {
		add("given", "summary: Shown under the title.");
		add("taken", "");
		Pages pages = pages();

		assertThat(pages.post("given").orElseThrow().post().lede()).isEqualTo("Shown under the title.");
		assertThat(pages.post("taken").orElseThrow().post().lede()).isEmpty();
	}

	@Test
	@DisplayName("RICH-9 beansprout.author is shown on each post page")
	void author() {
		add("hello", "");

		assertThat(pages("", "Ines Kovač", "").post("hello").orElseThrow().post().author()).isEqualTo("Ines Kovač");
		assertThat(pages().post("hello").orElseThrow().post().author()).isEmpty();
	}

	@Test
	@DisplayName("RICH-10 beansprout.photo is on every page, and a bad value is refused at startup")
	void photo() {
		add("hello", "");
		Pages pages = pages("", "", "/images/me.png");

		assertThat(List.of(pages.index(1).orElseThrow().site(), pages.post("hello").orElseThrow().site(),
				pages.archive().site(), pages.notFound("/x").site()))
			.allSatisfy(site -> assertThat(site.photo()).isEqualTo("/images/me.png"));
		assertThatIllegalArgumentException().isThrownBy(() -> new BlogSettings("B", "", 10, "", "me.png"))
			.withMessageContaining("beansprout.photo");
	}

	@Test
	@DisplayName("RICH-11 index pages mark Latest as the current menu item")
	void latestIsCurrent() {
		for (int i = 1; i <= 12; i++) {
			add("p" + i, "");
		}
		Pages pages = pages();

		assertThat(pages.index(1).orElseThrow().site().section()).isEqualTo("latest");
		assertThat(pages.index(2).orElseThrow().site().section()).isEqualTo("latest");
		assertThat(pages.post("p1").orElseThrow().site().section()).isEmpty();
		files.add(new MarkdownFile("tagged.md", "---\ntitle: Tagged\ndate: 2026-01-01\ntags: [java]\n---\n"));
		assertThat(pages().tag("java", 1).orElseThrow().site().section()).isEmpty();
	}

	@Test
	@DisplayName("RICH-12 the archive marks Archive as the current menu item")
	void archiveIsCurrent() {
		assertThat(pages().archive().site().section()).isEqualTo("archive");
		assertThat(pages().notFound("/x").site().section()).isEmpty();
	}

}
