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

/** Old addresses that a post's frontmatter names, answered with a redirect to the post. */
class AliasesTests {

	private final Clock clock = Clock.fixed(Instant.parse("2026-11-16T12:00:00Z"), ZoneOffset.UTC);

	private final List<MarkdownFile> files = new ArrayList<>();

	private Posts posts() {
		return new Posts(() -> List.copyOf(files), new PostParser(ZoneOffset.UTC), clock, false);
	}

	private Pages pages(Links links) {
		return new Pages(posts(), links, new BlogSettings("Bean Notes", "", 10, "", ""));
	}

	private Aliases aliases() {
		return new Aliases(pages(new Links("", "/blog")));
	}

	private void add(String path, String frontmatter) {
		String slug = path.substring(path.lastIndexOf('/') + 1).replace(".md", "");
		files.add(new MarkdownFile(path, "---\ntitle: Post " + slug + "\ndate: 2026-03-14\ntags: [java]\n" + frontmatter
				+ "\n---\nThe first paragraph.\n"));
	}

	@Test
	@DisplayName("ALIAS-1 each of a post's aliases points to the post's address")
	void aliasesPointToThePost() {
		add("hello.md", "aliases: [/2019/03/14/hello.html, /posts/hello/]");
		Aliases aliases = aliases();

		assertThat(aliases.all()).containsExactlyInAnyOrder(new Alias("/2019/03/14/hello.html", "/blog/hello"),
				new Alias("/posts/hello/", "/blog/hello"));
		assertThat(aliases.target("/2019/03/14/hello.html")).contains("/blog/hello");
		assertThat(aliases.target("/nothing/here")).isEmpty();
	}

	@Test
	@DisplayName("ALIAS-1 an alias written with %XX escapes matches the path they spell, as a request is read")
	void percentEscapes() {
		add("cafe.md", "aliases: [\"/caf%C3%A9.html\", /m%C3%BCsli/]");
		Aliases aliases = aliases();

		assertThat(aliases.target("/café.html")).contains("/blog/cafe");
		assertThat(aliases.target("/müsli")).contains("/blog/cafe");
		assertThat(aliases.all()).extracting(Alias::path).containsExactly("/café.html", "/müsli/");
	}

	@Test
	@DisplayName("ALIAS-2 each path in redirect_from is an alias")
	void redirectFrom() {
		add("hello.md", "aliases: [/one]\nredirect_from:\n  - /two\n  - /three");

		assertThat(aliases().all()).extracting(Alias::path).containsExactlyInAnyOrder("/one", "/two", "/three");
	}

	@Test
	@DisplayName("ALIAS-3 aliases are read as a [a, b] list, a - item list or a single value")
	void threeShapes() {
		add("a.md", "aliases: [/a1, \"/a2\"]");
		add("b.md", "aliases:\n- /b1\n- '/b2'");
		add("c.md", "redirect_from: /c1");

		assertThat(aliases().all()).extracting(Alias::path).containsExactlyInAnyOrder("/a1", "/a2", "/b1", "/b2", "/c1");
	}

	@Test
	@DisplayName("ALIAS-4 an alias matches with or without a trailing slash")
	void trailingSlash() {
		add("hello.md", "aliases: [/posts/hello/, /old/hello]");
		Aliases aliases = aliases();

		assertThat(aliases.target("/posts/hello/")).contains("/blog/hello");
		assertThat(aliases.target("/posts/hello")).contains("/blog/hello");
		assertThat(aliases.target("/old/hello")).contains("/blog/hello");
		assertThat(aliases.target("/old/hello/")).contains("/blog/hello");
	}

	@Test
	@DisplayName("ALIAS-5 an alias is a path in the app, and it points to the post under the context path")
	void contextPath() {
		add("hello.md", "aliases: [/old/hello]");
		Aliases aliases = new Aliases(pages(new Links("/app", "/blog")));

		assertThat(aliases.target("/old/hello")).contains("/app/blog/hello");
		assertThat(aliases.target("/app/old/hello")).isEmpty();
	}

	@Test
	@DisplayName("ALIAS-6 an alias that does not start with / is ignored with a warning that names the post")
	void relativeAlias() {
		add("hello.md", "aliases: [old-name, /kept]");
		Aliases aliases = aliases();

		assertThat(aliases.all()).extracting(Alias::path).containsExactly("/kept");
		assertThat(aliases.problems()).singleElement().asString().contains("hello.md", "old-name", "starts with /");
	}

	@Test
	@DisplayName("ALIAS-6 an alias that is not a plain path from the root is ignored with a warning")
	void notAPlainPath() {
		add("hello.md", "aliases: [/, /a/../b, /a//b, /./a, \"/a\\\\b\", /a?b=1, /a#top, //example.com/x]");
		Aliases aliases = aliases();

		assertThat(aliases.all()).isEmpty();
		assertThat(aliases.problems()).hasSize(8).allSatisfy(problem -> assertThat(problem).contains("hello.md"));
	}

	@Test
	@DisplayName("ALIAS-6 an alias with a % escape that cannot be read is ignored with a warning")
	void badEscape() {
		add("hello.md", "aliases: [\"/100%\", /a%zz, /a%2Fb, /a%00b, /%C3]");
		Aliases aliases = aliases();

		assertThat(aliases.all()).isEmpty();
		assertThat(aliases.problems()).hasSize(5).allSatisfy(problem -> assertThat(problem).contains("hello.md"));
	}

	@Test
	@DisplayName("ALIAS-7 an alias at /robots.txt is ignored with a warning, since the blog serves robots.txt there")
	void robotsTxt() {
		add("hello.md", "aliases: [/robots.txt]");
		Aliases aliases = aliases();

		assertThat(aliases.all()).isEmpty();
		assertThat(aliases.problems()).singleElement().asString().contains("hello.md", "/robots.txt");
	}

	@Test
	@DisplayName("ALIAS-7 an alias that is a page the blog serves is ignored with a warning that names both")
	void blogPage() {
		add("hello.md", "aliases: [/blog/other, /blog/archive, /blog/feed.xml, /blog/sitemap.xml, /blog, /blog/, /blog/tags/java/, /blog/hello]");
		add("other.md", "");
		Aliases aliases = aliases();

		assertThat(aliases.all()).isEmpty();
		assertThat(aliases.problems()).hasSize(8);
		assertThat(aliases.problems()).anySatisfy(problem -> assertThat(problem).contains("hello.md", "/blog/other"));
		assertThat(aliases.problems()).anySatisfy(problem -> assertThat(problem).contains("hello.md", "/blog/tags/java/"));
	}

	@Test
	@DisplayName("ALIAS-7 page 1 of the index or of a tag is a page the blog serves: it answers with a redirect of its own")
	void firstPages() {
		add("hello.md", "aliases: [/blog/page/1, /blog/tags/java/page/1/, /blog/tags/spring/page/1]");
		Aliases aliases = aliases();

		assertThat(aliases.all()).as("spring is no tag of this blog, so its page 1 is a 404 the alias can take")
			.containsExactly(new Alias("/blog/tags/spring/page/1", "/blog/hello"));
		assertThat(aliases.problems()).hasSize(2);
		assertThat(aliases.problems()).anySatisfy(problem -> assertThat(problem).contains("hello.md", "/blog/page/1"));
		assertThat(aliases.problems()).anySatisfy(problem -> assertThat(problem).contains("hello.md", "/blog/tags/java/page/1/"));
	}

	@Test
	@DisplayName("ALIAS-8 two posts with the same alias: the first by path gets it, with a warning that names both")
	void sameAlias() {
		add("b.md", "aliases: [/shared/]");
		add("a.md", "aliases: [/shared]");
		Aliases aliases = aliases();

		assertThat(aliases.all()).containsExactly(new Alias("/shared", "/blog/a"));
		assertThat(aliases.problems()).singleElement().asString().contains("a.md", "b.md", "/shared");
	}

	@Test
	@DisplayName("ALIAS-8 a post that names one alias twice keeps it once, with no warning")
	void sameAliasInOnePost() {
		add("a.md", "aliases: [/shared]\nredirect_from: /shared/");
		Aliases aliases = aliases();

		assertThat(aliases.all()).hasSize(1);
		assertThat(aliases.problems()).isEmpty();
	}

	@Test
	@DisplayName("ALIAS-9 a draft's aliases and a post dated later have no answer")
	void draftsAndLaterPosts() {
		add("draft.md", "draft: true\naliases: [/draft-old]");
		files.add(new MarkdownFile("later.md", "---\ntitle: Later\ndate: 2027-01-01\naliases: [/later-old]\n---\nSoon.\n"));
		Aliases aliases = aliases();

		assertThat(aliases.all()).isEmpty();
		assertThat(aliases.target("/draft-old")).isEmpty();
		assertThat(aliases.target("/later-old")).isEmpty();
	}

	@Test
	@DisplayName("ALIAS-12 a post whose slug left the date out of its name keeps the full name as an alias")
	void datedName() {
		files.add(new MarkdownFile("2024-05-01-first-post.md", "---\ntitle: First\n---\nBody.\n"));
		files.add(new MarkdownFile("2024-06-01-trip/index.md", "---\ntitle: Trip\n---\nBody.\n"));
		files.add(new MarkdownFile("2024-07-01-own.md", "---\ntitle: Own\nslug: own-slug\n---\nBody.\n"));
		Aliases aliases = aliases();

		assertThat(aliases.target("/blog/2024-05-01-first-post")).contains("/blog/first-post");
		assertThat(aliases.target("/blog/2024-06-01-trip")).contains("/blog/trip");
		assertThat(aliases.target("/blog/2024-07-01-own")).as("a slug line names the address itself").isEmpty();
		assertThat(aliases.problems()).isEmpty();
	}

	@Test
	@DisplayName("ALIAS-15 aliases are left out of the sitemap")
	void notInSitemap() {
		add("hello.md", "aliases: [/2019/03/14/hello.html]");
		files.add(new MarkdownFile("2024-05-01-dated.md", "---\ntitle: Dated\n---\nBody.\n"));
		Posts posts = posts();
		Pages pages = new Pages(posts, new Links("", "/blog"), new BlogSettings("Bean Notes", "", 10, "", ""));

		String sitemap = new Feeds(posts, new Links("", "/blog"), pages).sitemap("https://example.com");

		assertThat(sitemap).contains("/blog/hello", "/blog/dated").doesNotContain("hello.html", "2024-05-01-dated");
	}

}
