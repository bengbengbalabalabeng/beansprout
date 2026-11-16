package dev.danvega.beansprout.view;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import dev.danvega.beansprout.content.MarkdownFile;
import dev.danvega.beansprout.content.PostParser;
import dev.danvega.beansprout.content.Posts;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PagesTests {

	private final Clock clock = Clock.fixed(Instant.parse("2026-11-16T12:00:00Z"), ZoneOffset.UTC);

	private final List<MarkdownFile> files = new ArrayList<>();

	private Pages pages(int pageSize) {
		Posts posts = new Posts(() -> List.copyOf(files), new PostParser(ZoneOffset.UTC), clock, false);
		return new Pages(posts, new Links("", "/blog"), new BlogSettings("Bean Notes", "Notes on beans.", pageSize));
	}

	private void add(String slug, String date, String... tags) {
		files.add(new MarkdownFile(slug + ".md", "---\ntitle: Post " + slug + "\ndate: " + date + "\ntags: ["
				+ String.join(", ", tags) + "]\n---\nThe body of " + slug + ".\n\n## A heading\n"));
	}

	private void addMany(int count) {
		for (int i = 1; i <= count; i++) {
			add("p" + i, "2026-01-%02d".formatted(i), "java");
		}
	}

	@Test
	@DisplayName("PAGES-1 the index shows the newest posts as cards, page size from settings")
	void indexShowsNewestCards() {
		addMany(5);

		IndexPage page = pages(3).index(1).orElseThrow();

		assertThat(page.latest().cards()).extracting(Card::title).containsExactly("Post p5", "Post p4", "Post p3");
		Card card = page.latest().cards().getFirst();
		assertThat(card.url()).isEqualTo("/blog/p5");
		assertThat(card.date()).isEqualTo("5 January 2026");
		assertThat(card.dateTime()).isEqualTo("2026-01-05");
		assertThat(card.reading()).isEqualTo("1 min read");
		assertThat(card.excerpt()).isEqualTo("The body of p5.");
		assertThat(card.tags()).containsExactly(new Tag("java", "/blog/tags/java"));
	}

	@Test
	@DisplayName("PAGES-2 later pages link to the newer and the older page")
	void paginationLinks() {
		addMany(7);
		Pages pages = pages(3);

		Listing first = pages.index(1).orElseThrow().latest();
		Listing second = pages.index(2).orElseThrow().latest();
		Listing last = pages.index(3).orElseThrow().latest();

		assertThat(first).extracting(Listing::page, Listing::pages, Listing::newer, Listing::older)
			.containsExactly("1", "3", "", "/blog/page/2");
		assertThat(second).extracting(Listing::newer, Listing::older).containsExactly("/blog", "/blog/page/3");
		assertThat(last).extracting(Listing::newer, Listing::older).containsExactly("/blog/page/2", "");
		assertThat(last.cards()).extracting(Card::title).containsExactly("Post p1");
	}

	@Test
	@DisplayName("PAGES-4 a page number below 1 or past the last page has no page")
	void outOfRangePages() {
		addMany(4);
		Pages pages = pages(3);

		assertThat(pages.index(0)).isEmpty();
		assertThat(pages.index(-1)).isEmpty();
		assertThat(pages.index(3)).isEmpty();
		assertThat(pages.tag("java", 3)).isEmpty();
	}

	@Test
	@DisplayName("PAGES-5 the post page carries title, date, reading time, tags and body")
	void postPage() {
		add("hello", "2026-03-14", "Spring Boot", "java");

		Post post = pages(10).post("hello").orElseThrow().post();

		assertThat(post.title()).isEqualTo("Post hello");
		assertThat(post.date()).isEqualTo("14 March 2026");
		assertThat(post.dateTime()).isEqualTo("2026-03-14");
		assertThat(post.reading()).isEqualTo("1 min read");
		assertThat(post.tags()).containsExactly(new Tag("Spring Boot", "/blog/tags/spring-boot"), new Tag("java", "/blog/tags/java"));
		assertThat(post.crumb()).isEqualTo(new Tag("Spring Boot", "/blog/tags/spring-boot"));
		assertThat(post.body()).contains("<p>The body of hello.</p>");
		assertThat(post.headings()).containsExactly(new Heading("a-heading", "A heading"));
	}

	@Test
	@DisplayName("PAGES-6 the post page links to the next newer and older post, empty at either end")
	void newerAndOlder() {
		add("old", "2026-01-01");
		add("middle", "2026-02-01");
		add("new", "2026-03-01");
		Pages pages = pages(10);

		Post middle = pages.post("middle").orElseThrow().post();
		Post newest = pages.post("new").orElseThrow().post();
		Post oldest = pages.post("old").orElseThrow().post();

		assertThat(middle.newer()).isEqualTo(new PostLink("Post new", "/blog/new"));
		assertThat(middle.older()).isEqualTo(new PostLink("Post old", "/blog/old"));
		assertThat(newest.newer()).isEqualTo(PostLink.NONE);
		assertThat(oldest.older()).isEqualTo(PostLink.NONE);
		assertThat(oldest.crumb()).isEqualTo(new Tag("Latest", "/blog"));
	}

	@Test
	@DisplayName("PAGES-7 a tag page lists that tag's posts newest first, paginated like the index")
	void tagPage() {
		add("a", "2026-01-01", "java");
		add("b", "2026-01-02", "spring");
		add("c", "2026-01-03", "java");
		add("d", "2026-01-04", "java");
		Pages pages = pages(2);

		TagPage first = pages.tag("java", 1).orElseThrow();
		TagPage second = pages.tag("java", 2).orElseThrow();

		assertThat(first.name()).isEqualTo("java");
		assertThat(first.count()).isEqualTo("3 posts");
		assertThat(first.listing().cards()).extracting(Card::title).containsExactly("Post d", "Post c");
		assertThat(first.listing().older()).isEqualTo("/blog/tags/java/page/2");
		assertThat(second.listing().newer()).isEqualTo("/blog/tags/java");
		assertThat(second.listing().cards()).extracting(Card::title).containsExactly("Post a");
		assertThat(first.site().tags()).filteredOn(TagCount::current).extracting(TagCount::name).containsExactly("java");
		assertThat(pages.tag("nope", 1)).isEmpty();
	}

	@Test
	@DisplayName("PAGES-9 every tag shows its count of published posts, most used first")
	void tagCounts() {
		add("a", "2026-01-01", "java");
		add("b", "2026-01-02", "spring", "java");

		Site site = pages(10).index(1).orElseThrow().site();

		assertThat(site.tags()).containsExactly(
				new TagCount("java", "/blog/tags/java", "2", false),
				new TagCount("spring", "/blog/tags/spring", "1", false));
	}

	@Test
	@DisplayName("PAGES-10 the archive groups every published post by year, newest year first")
	void archive() {
		add("a", "2024-03-23");
		add("b", "2025-06-01");
		add("c", "2025-07-04");
		add("d", "2026-11-01");

		Archive archive = pages(10).archive().archive();

		assertThat(archive.total()).isEqualTo("4 posts");
		assertThat(archive.since()).isEqualTo("March 2024");
		assertThat(archive.years()).extracting(Year::year, Year::count).containsExactly(
				org.assertj.core.groups.Tuple.tuple("2026", "1 post"),
				org.assertj.core.groups.Tuple.tuple("2025", "2 posts"),
				org.assertj.core.groups.Tuple.tuple("2024", "1 post"));
		assertThat(archive.years().get(1).posts()).extracting(ArchiveRow::title, ArchiveRow::date, ArchiveRow::reading)
			.containsExactly(
					org.assertj.core.groups.Tuple.tuple("Post c", "4 July", "1 min"),
					org.assertj.core.groups.Tuple.tuple("Post b", "1 June", "1 min"));
	}

	@Test
	@DisplayName("PAGES-11 a missing or hidden post or tag has no page, and the not-found page lists recent posts")
	void missingThings() {
		add("live", "2026-01-01", "java");
		files.add(new MarkdownFile("draft.md", "---\ntitle: Draft\ndate: 2026-01-02\ndraft: true\ntags: [secret]\n---\n"));
		Pages pages = pages(10);

		assertThat(pages.post("draft")).isEmpty();
		assertThat(pages.post("nope")).isEmpty();
		assertThat(pages.tag("secret", 1)).isEmpty();

		NotFoundPage notFound = pages.notFound("/blog/nope");
		assertThat(notFound.path()).isEqualTo("/blog/nope");
		assertThat(notFound.postCount()).isEqualTo("1 post");
		assertThat(notFound.recent()).extracting(MiniPost::title).containsExactly("Post live");
	}

	@Test
	@DisplayName("PAGES-12 with no posts the index is page 1 of 1 with no cards")
	void emptyBlog() {
		Pages pages = pages(10);

		Listing listing = pages.index(1).orElseThrow().latest();

		assertThat(listing.cards()).isEmpty();
		assertThat(listing).extracting(Listing::page, Listing::pages, Listing::newer, Listing::older)
			.containsExactly("1", "1", "", "");
		assertThat(pages.archive().archive()).extracting(Archive::total, Archive::since).containsExactly("0 posts", "");
	}

	@Test
	@DisplayName("PAGES-13 every page carries the blog's name and description")
	void siteName() {
		add("hello", "2026-01-01");
		Pages pages = pages(10);

		assertThat(List.of(pages.index(1).orElseThrow().site(), pages.post("hello").orElseThrow().site(),
				pages.archive().site(), pages.notFound("/x").site()))
			.allSatisfy(site -> {
				assertThat(site.name()).isEqualTo("Bean Notes");
				assertThat(site.description()).isEqualTo("Notes on beans.");
				assertThat(site.year()).isEqualTo("2026");
				assertThat(site.home()).isEqualTo("/blog");
				assertThat(site.archive()).isEqualTo("/blog/archive");
				assertThat(site.feed()).isEqualTo("/blog/feed.xml");
			});
	}

	@Test
	@DisplayName("PAGES-18 a theme only gets view records made of strings, booleans, lists and view records")
	void viewRecordsArePlain() {
		Set<Class<?>> seen = new HashSet<>();
		for (Class<?> page : List.of(IndexPage.class, PostPage.class, TagPage.class, ArchivePage.class, NotFoundPage.class)) {
			assertPlain(page, seen);
		}
		assertThat(seen).allSatisfy(type -> assertThat(type.getPackageName()).isEqualTo("dev.danvega.beansprout.view"));
	}

	private static void assertPlain(Class<?> type, Set<Class<?>> seen) {
		if (!seen.add(type)) {
			return;
		}
		assertThat(type.isRecord()).as("%s is a record", type.getSimpleName()).isTrue();
		for (RecordComponent component : type.getRecordComponents()) {
			checkType(component.getGenericType(), type.getSimpleName() + "." + component.getName(), seen);
		}
	}

	private static void checkType(Type type, String where, Set<Class<?>> seen) {
		if (type instanceof ParameterizedType list) {
			assertThat(list.getRawType()).as(where).isEqualTo(List.class);
			checkType(list.getActualTypeArguments()[0], where, seen);
			return;
		}
		Class<?> raw = (Class<?>) type;
		if (raw == String.class || raw == boolean.class) {
			return;
		}
		assertPlain(raw, seen);
	}

	@Test
	@DisplayName("FEEDS-4 the page list has the index pages, every post, every tag page and the archive")
	void pageList() {
		add("a", "2026-01-01", "java");
		add("b", "2026-01-02", "java");
		add("c", "2026-01-03", "spring");
		files.add(new MarkdownFile("draft.md", "---\ntitle: Draft\ndate: 2026-01-04\ndraft: true\n---\n"));

		List<PageUrl> urls = pages(2).urls();

		assertThat(urls).extracting(PageUrl::path).containsExactly(
				"/blog", "/blog/page/2", "/blog/archive",
				"/blog/c", "/blog/b", "/blog/a",
				"/blog/tags/java", "/blog/tags/spring");
	}

	@Test
	@DisplayName("FEEDS-5 every post in the page list carries its date as lastmod")
	void pageListDates() {
		add("a", "2026-01-01");

		assertThat(pages(10).urls()).filteredOn(url -> url.path().equals("/blog/a"))
			.singleElement().extracting(PageUrl::lastModified).isEqualTo("2026-01-01");
	}

}
