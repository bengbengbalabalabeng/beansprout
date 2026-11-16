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

class SeoTests {

	private final Clock clock = Clock.fixed(Instant.parse("2026-11-16T12:00:00Z"), ZoneOffset.UTC);

	private final List<MarkdownFile> files = new ArrayList<>();

	private Posts posts;

	private Pages pages(String root, String author, String photo) {
		posts = new Posts(() -> List.copyOf(files), new PostParser(ZoneOffset.UTC), clock, false);
		return new Pages(posts, new Links(root, "/blog"), new BlogSettings("Bean Notes", "", 2, author, photo))
			.forSite("https://example.com/");
	}

	private Pages pages() {
		return pages("", "", "");
	}

	private void add(String slug, String frontmatter) {
		files.add(new MarkdownFile(slug + ".md", "---\ntitle: Post " + slug + "\ndate: 2026-03-14T09:30:00Z\n" + frontmatter
				+ "\n---\nThe first paragraph of " + slug + ".\n"));
	}

	@Test
	@DisplayName("SEO-1 every page served with 200 has a canonical link to its own absolute address")
	void canonical() {
		add("a", "tags: [java]");
		add("b", "");
		add("c", "");
		Pages pages = pages("/app", "", "");

		assertThat(pages.index(1).orElseThrow().meta().canonical()).isEqualTo("https://example.com/app/blog");
		assertThat(pages.index(2).orElseThrow().meta().canonical()).isEqualTo("https://example.com/app/blog/page/2");
		assertThat(pages.post("a").orElseThrow().meta().canonical()).isEqualTo("https://example.com/app/blog/a");
		assertThat(pages.tag("java", 1).orElseThrow().meta().canonical()).isEqualTo("https://example.com/app/blog/tags/java");
		assertThat(pages.archive().meta().canonical()).isEqualTo("https://example.com/app/blog/archive");
		assertThat(pages.notFound("/app/blog/nope").meta().canonical()).isEmpty();
	}

	@Test
	@DisplayName("SEO-2 the description is the summary on a post and a sentence naming the blog elsewhere")
	void description() {
		add("a", "summary: Shown in search results.\ntags: [java]");
		Pages pages = pages();

		assertThat(pages.post("a").orElseThrow().meta().description()).isEqualTo("Shown in search results.");
		assertThat(pages.index(1).orElseThrow().meta().description()).contains("Bean Notes");
		assertThat(pages.tag("java", 1).orElseThrow().meta().description()).contains("java", "Bean Notes");
		assertThat(pages.archive().meta().description()).contains("Bean Notes");
	}

	@Test
	@DisplayName("SEO-3 every page has a title, description, address, site name and type for Open Graph")
	void openGraph() {
		add("a", "");
		Meta meta = pages().post("a").orElseThrow().meta();

		assertThat(meta.title()).isEqualTo("Post a");
		assertThat(meta.siteName()).isEqualTo("Bean Notes");
		assertThat(meta.canonical()).isEqualTo("https://example.com/blog/a");
		assertThat(pages().index(1).orElseThrow().meta().title()).isEqualTo("Bean Notes");
	}

	@Test
	@DisplayName("SEO-4 og:type is article on posts and website elsewhere")
	void type() {
		add("a", "tags: [java]");
		Pages pages = pages();

		assertThat(pages.post("a").orElseThrow().meta().type()).isEqualTo("article");
		assertThat(List.of(pages.index(1).orElseThrow().meta(), pages.tag("java", 1).orElseThrow().meta(), pages.archive().meta()))
			.extracting(Meta::type).containsOnly("website");
	}

	@Test
	@DisplayName("SEO-5 a post's picture is og:image as an absolute address, with a large card")
	void postPicture() {
		add("path", "picture: /images/a.png\npicture-alt: A thing");
		add("web", "picture: https://cdn.example.org/b.png");
		Pages pages = pages("/app", "", "/images/me.png");

		Meta path = pages.post("path").orElseThrow().meta();
		assertThat(path).extracting(Meta::image, Meta::imageAlt, Meta::twitterCard)
			.containsExactly("https://example.com/app/images/a.png", "A thing", "summary_large_image");
		assertThat(pages.post("web").orElseThrow().meta().image()).isEqualTo("https://cdn.example.org/b.png");
	}

	@Test
	@DisplayName("SEO-6 without a post picture, the writer's photo is og:image with a small card")
	void photoFallback() {
		add("a", "");
		Pages withPhoto = pages("", "", "/images/me.png");

		assertThat(withPhoto.post("a").orElseThrow().meta()).extracting(Meta::image, Meta::twitterCard)
			.containsExactly("https://example.com/images/me.png", "summary");
		assertThat(withPhoto.index(1).orElseThrow().meta().image()).isEqualTo("https://example.com/images/me.png");
		assertThat(pages().post("a").orElseThrow().meta()).extracting(Meta::image, Meta::twitterCard)
			.containsExactly("", "summary");
	}

	@Test
	@DisplayName("SEO-7 a post page carries its published time, modified time and tags")
	void articleTags() {
		add("a", "tags: [java, Spring Boot]");

		Meta meta = pages().post("a").orElseThrow().meta();

		assertThat(meta.publishedTime()).isEqualTo("2026-03-14T09:30:00Z");
		assertThat(meta.modifiedTime()).isEqualTo("2026-03-14T09:30:00Z");
		assertThat(meta.tags()).containsExactly("java", "Spring Boot");
		assertThat(pages().index(1).orElseThrow().meta().publishedTime()).isEmpty();
	}

	@Test
	@DisplayName("SEO-8 an updated date is the modified date, and SEO-9 without one the post's date is")
	void updated() {
		add("edited", "updated: 2026-04-02");
		add("untouched", "");
		Pages pages = pages();

		assertThat(pages.post("edited").orElseThrow().meta().modifiedTime()).isEqualTo("2026-04-02T00:00:00Z");
		assertThat(pages.post("untouched").orElseThrow().meta().modifiedTime()).isEqualTo("2026-03-14T09:30:00Z");
		assertThat(pages.urls()).filteredOn(url -> url.path().equals("/blog/edited")).singleElement()
			.extracting(PageUrl::lastModified).isEqualTo("2026-04-02");
	}

	@Test
	@DisplayName("SEO-10 an updated value that is not a date skips the post with a problem that names it")
	void badUpdated() {
		add("bad", "updated: last week");
		add("fine", "");

		pages();

		assertThat(posts.published()).extracting(p -> p.slug()).containsExactly("fine");
		assertThat(posts.problems()).singleElement().asString().contains("bad.md", "updated");
	}

	@Test
	@DisplayName("SEO-11 the post page says Updated when the updated day is later than the post's day")
	void updatedShown() {
		add("later", "updated: 2026-04-02");
		add("sameday", "updated: 2026-03-14T18:00:00Z");
		Pages pages = pages();

		assertThat(pages.post("later").orElseThrow().post()).extracting(Post::updated, Post::updatedDateTime)
			.containsExactly("2 April 2026", "2026-04-02");
		assertThat(pages.post("sameday").orElseThrow().post().updated()).isEmpty();
	}

	@Test
	@DisplayName("SEO-12 a post page has BlogPosting JSON-LD with headline, description, address and dates")
	void jsonLd() {
		add("a", "summary: About a.\nupdated: 2026-04-02");

		String json = pages().post("a").orElseThrow().meta().jsonLd();

		assertThat(json).contains("\"@context\":\"https://schema.org\"", "\"@type\":\"BlogPosting\"",
				"\"headline\":\"Post a\"", "\"description\":\"About a.\"", "\"url\":\"https://example.com/blog/a\"",
				"\"datePublished\":\"2026-03-14T09:30:00Z\"", "\"dateModified\":\"2026-04-02T00:00:00Z\"")
			.doesNotContain("\"image\"", "\"author\"");
		assertThat(pages().index(1).orElseThrow().meta().jsonLd()).isEmpty();
	}

	@Test
	@DisplayName("SEO-13 a post's picture is the JSON-LD image")
	void jsonLdImage() {
		add("a", "picture: /images/a.png");

		assertThat(pages().post("a").orElseThrow().meta().jsonLd()).contains("\"image\":\"https://example.com/images/a.png\"");
	}

	@Test
	@DisplayName("SEO-14 beansprout.author is the JSON-LD author as a Person")
	void jsonLdAuthor() {
		add("a", "");

		assertThat(pages("", "Ines Kovač", "").post("a").orElseThrow().meta().jsonLd())
			.contains("\"author\":{\"@type\":\"Person\",\"name\":\"Ines Kovač\",\"url\":\"https://example.com/blog\"}");
	}

	@Test
	@DisplayName("SEO-15 no value can end the script element or break the JSON")
	void jsonLdEscaping() {
		files.add(new MarkdownFile("x.md", "---\ntitle: \"</script><script>alert(1)</script> \\\"q\\\" \\\\ & \u2028 end\"\n"
				+ "date: 2026-03-14\nslug: x\n---\nBody.\n"));

		String json = pages().post("x").orElseThrow().meta().jsonLd();

		assertThat(json).doesNotContain("</script>", "<script>", "\u2028").contains("\\u003c/script\\u003e", "\\\"q\\\"",
				"\\\\", "\\u0026", "\\u2028");
	}

	@Test
	@DisplayName("SEO-16 absolute addresses come from the site address given to the pages")
	void siteAddress() {
		add("a", "");
		Pages base = new Pages(new Posts(() -> List.copyOf(files), new PostParser(ZoneOffset.UTC), clock, false),
				new Links("", "/blog"), new BlogSettings("B", "", 10));

		assertThat(base.forSite("http://localhost:8080").post("a").orElseThrow().meta().canonical())
			.isEqualTo("http://localhost:8080/blog/a");
		assertThat(base.forSite("https://blog.example.com/").post("a").orElseThrow().meta().canonical())
			.isEqualTo("https://blog.example.com/blog/a");
	}


	@Test
	@DisplayName("SEO-2 the index uses beansprout.description when set, and the not-found page names the blog")
	void descriptionsNameTheBlog() {
		add("a", "");
		posts = new Posts(() -> List.copyOf(files), new PostParser(ZoneOffset.UTC), clock, false);
		Pages withBio = new Pages(posts, new Links("", "/blog"), new BlogSettings("Bean Notes", "Notes on beans.", 2))
			.forSite("https://example.com");

		assertThat(withBio.index(1).orElseThrow().meta().description()).isEqualTo("Notes on beans.");
		assertThat(pages().index(1).orElseThrow().meta().description()).isEqualTo("The latest posts from Bean Notes.");
		assertThat(pages().notFound("/blog/x").meta().description()).contains("Bean Notes");
	}

	@Test
	@DisplayName("SEO-2 a post without a summary is described by its first paragraph")
	void descriptionFallback() {
		add("a", "");

		assertThat(pages().post("a").orElseThrow().meta().description()).isEqualTo("The first paragraph of a.");
	}

	@Test
	@DisplayName("SEO-16 spaces and extra trailing slashes in the site address are ignored")
	void tidySiteAddress() {
		add("a", "");
		Pages base = new Pages(new Posts(() -> List.copyOf(files), new PostParser(ZoneOffset.UTC), clock, false),
				new Links("", "/blog"), new BlogSettings("B", "", 10));

		assertThat(base.forSite("  https://example.com//  ").post("a").orElseThrow().meta().canonical())
			.isEqualTo("https://example.com/blog/a");
	}

}
