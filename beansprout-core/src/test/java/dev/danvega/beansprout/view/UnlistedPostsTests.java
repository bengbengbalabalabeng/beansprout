package dev.danvega.beansprout.view;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import dev.danvega.beansprout.content.MarkdownFile;
import dev.danvega.beansprout.content.PostParser;
import dev.danvega.beansprout.content.Posts;
import dev.danvega.beansprout.feed.Feeds;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Posts with {@code unlisted: true}, from specs/unlisted.md. */
class UnlistedPostsTests {

	private static final String SITE = "https://example.com";

	private final List<MarkdownFile> files = new ArrayList<>();

	@BeforeEach
	void posts() {
		add("listed-new", "2026-11-10", "tags: [java]");
		add("keynote", "2026-11-08", "tags: [java, porto]\nunlisted: true\naliases: [/old/keynote]");
		add("listed-old", "2026-11-05", "tags: [java]");
		add("plain", "2026-11-04", "tags: [kotlin]\nunlisted: false");
		add("hidden-draft", "2026-11-07", "tags: [java]\nunlisted: true\ndraft: true");
		add("hidden-later", "2026-12-01", "tags: [java]\nunlisted: true");
	}

	private void add(String slug, String date, String frontmatter) {
		files.add(new MarkdownFile(slug + ".md", "---\ntitle: Post " + slug + "\ndate: " + date + "\n" + frontmatter
				+ "\n---\nThe body of " + slug + ".\n"));
	}

	private Posts posts(String now) {
		Clock clock = Clock.fixed(Instant.parse(now), ZoneOffset.UTC);
		return new Posts(() -> List.copyOf(files), new PostParser(ZoneOffset.UTC), clock, false);
	}

	private Pages pages(Posts posts, int pageSize) {
		return new Pages(posts, new Links("", "/blog"), new BlogSettings("Bean Notes", "", pageSize, "", "", List.of(), 3))
			.forSite(SITE);
	}

	private Pages pages() {
		return pages(posts("2026-11-16T12:00:00Z"), 10);
	}

	private Feeds feeds() {
		Posts posts = posts("2026-11-16T12:00:00Z");
		return new Feeds(posts, new Links("", "/blog"), pages(posts, 10));
	}

	private static List<String> cards(Listing listing) {
		return listing.cards().stream().map(Card::url).toList();
	}

	@Test
	@DisplayName("UNLISTED-1 an unlisted post has a page at its address")
	void servedAtItsAddress() {
		PostPage page = pages().post("keynote").orElseThrow();

		assertThat(page.post().title()).isEqualTo("Post keynote");
		assertThat(page.post().url()).isEqualTo("/blog/keynote");
		assertThat(page.post().body()).contains("The body of keynote.");
		assertThat(posts("2026-11-16T12:00:00Z").find("keynote")).isPresent();
	}

	@Test
	@DisplayName("UNLISTED-1 an unlisted post keeps its aliases, which lead to its address")
	void keepsItsAliases() {
		assertThat(new Aliases(pages()).target("/old/keynote")).contains("/blog/keynote");
	}

	@Test
	@DisplayName("UNLISTED-2 unlisted posts are left out of the index and its numbered pages")
	void notOnTheIndex() {
		assertThat(cards(pages().index(1).orElseThrow().latest())).containsExactly("/blog/listed-new", "/blog/listed-old",
				"/blog/plain");

		Pages paged = pages(posts("2026-11-16T12:00:00Z"), 1);
		assertThat(cards(paged.index(1).orElseThrow().latest())).containsExactly("/blog/listed-new");
		assertThat(cards(paged.index(2).orElseThrow().latest())).containsExactly("/blog/listed-old");
		assertThat(cards(paged.index(3).orElseThrow().latest())).containsExactly("/blog/plain");
		assertThat(paged.index(3).orElseThrow().latest().pages()).isEqualTo("3");
		assertThat(paged.index(4)).isEmpty();
	}

	@Test
	@DisplayName("UNLISTED-3 unlisted posts are left out of tag pages")
	void notOnTagPages() {
		TagPage java = pages().tag("java", 1).orElseThrow();

		assertThat(cards(java.listing())).containsExactly("/blog/listed-new", "/blog/listed-old");
		assertThat(java.count()).isEqualTo("2 posts");
	}

	@Test
	@DisplayName("UNLISTED-4 unlisted posts are left out of the archive")
	void notInTheArchive() {
		Archive archive = pages().archive().archive();

		assertThat(archive.total()).isEqualTo("3 posts");
		assertThat(archive.years()).flatExtracting(Year::posts).extracting(ArchiveRow::url)
			.containsExactly("/blog/listed-new", "/blog/listed-old", "/blog/plain");
	}

	@Test
	@DisplayName("UNLISTED-5 unlisted posts are left out of the feed")
	void notInTheFeed() {
		String atom = feeds().atom(SITE);

		assertThat(atom).contains("<title>Post listed-new</title>", "<title>Post plain</title>");
		assertThat(atom).doesNotContain("keynote");
	}

	@Test
	@DisplayName("UNLISTED-6 unlisted posts are left out of tag feeds")
	void notInTagFeeds() {
		String java = feeds().tagAtom("java", SITE).orElseThrow();

		assertThat(java).contains("<title>Post listed-new</title>", "<title>Post listed-old</title>");
		assertThat(java).doesNotContain("keynote");
		assertThat(feeds().tagAtom("porto", SITE)).isEmpty();
	}

	@Test
	@DisplayName("UNLISTED-7 unlisted posts are left out of the sitemap")
	void notInTheSitemap() {
		assertThat(pages().sitemapUrls()).extracting(PageUrl::path).contains("/blog/listed-new")
			.doesNotContain("/blog/keynote");
		String sitemap = feeds().sitemap(SITE);
		assertThat(sitemap).contains("<loc>https://example.com/blog/listed-new</loc>").doesNotContain("keynote");
	}

	@Test
	@DisplayName("UNLISTED-8 unlisted posts are left out of related posts")
	void notRelated() {
		assertThat(pages().post("listed-new").orElseThrow().post().related()).extracting(PostLink::url)
			.containsExactly("/blog/listed-old");
		assertThat(pages().post("listed-old").orElseThrow().post().related()).extracting(PostLink::url)
			.containsExactly("/blog/listed-new");
	}

	@Test
	@DisplayName("UNLISTED-9 unlisted posts are left out of other posts' newer and older links")
	void notANeighbour() {
		Post newer = pages().post("listed-new").orElseThrow().post();
		Post older = pages().post("listed-old").orElseThrow().post();

		assertThat(newer.older().url()).isEqualTo("/blog/listed-old");
		assertThat(older.newer().url()).isEqualTo("/blog/listed-new");
	}

	@Test
	@DisplayName("UNLISTED-10 an unlisted post's own page has no newer or older links")
	void noNeighbours() {
		Post keynote = pages().post("keynote").orElseThrow().post();

		assertThat(keynote.newer()).isEqualTo(PostLink.NONE);
		assertThat(keynote.older()).isEqualTo(PostLink.NONE);
	}

	@Test
	@DisplayName("UNLISTED-11 unlisted posts are left out of tag counts, so a tag only unlisted posts use has no page")
	void notInTagCounts() {
		Posts posts = posts("2026-11-16T12:00:00Z");

		assertThat(posts.tags()).extracting(tag -> tag.slug() + "=" + tag.count()).containsExactly("java=2", "kotlin=1");
		assertThat(posts.tag("porto")).isEmpty();
		assertThat(pages().tag("porto", 1)).isEmpty();
		assertThat(pages().index(1).orElseThrow().site().tags()).extracting(TagCount::name, TagCount::count)
			.containsExactly(org.assertj.core.groups.Tuple.tuple("java", "2"), org.assertj.core.groups.Tuple.tuple("kotlin", "1"));
		assertThat(pages().urls()).extracting(PageUrl::path).doesNotContain("/blog/tags/porto");
	}

	@Test
	@DisplayName("UNLISTED-14 an unlisted post's page has a robots value of noindex")
	void noindex() {
		Pages pages = pages();

		assertThat(pages.post("keynote").orElseThrow().meta().robots()).isEqualTo("noindex");
		assertThat(pages.post("listed-new").orElseThrow().meta().robots()).isEmpty();
		assertThat(pages.post("plain").orElseThrow().meta().robots()).isEmpty();
		assertThat(pages.index(1).orElseThrow().meta().robots()).isEmpty();
		assertThat(pages.tag("java", 1).orElseThrow().meta().robots()).isEmpty();
		assertThat(pages.archive().meta().robots()).isEmpty();
		assertThat(pages.notFound("/blog/nope").meta().robots()).isEmpty();
	}

	@Test
	@DisplayName("UNLISTED-16 the pages the static export writes include every live unlisted post")
	void exportedPages() {
		assertThat(pages().urls()).extracting(PageUrl::path).contains("/blog/keynote", "/blog/listed-new")
			.doesNotContain("/blog/hidden-draft", "/blog/hidden-later");
	}

	@Test
	@DisplayName("UNLISTED-17 a post whose unlisted is not true or false is skipped")
	void badValue() {
		add("odd", "2026-11-09", "unlisted: yes");
		add("empty", "2026-11-09", "unlisted:");
		Posts posts = posts("2026-11-16T12:00:00Z");

		assertThat(posts.find("odd")).isEmpty();
		assertThat(posts.find("empty")).isEmpty();
		assertThat(posts.problems()).anySatisfy(problem -> assertThat(problem).contains("odd.md").contains("unlisted")
			.contains("'yes'").contains("true or false"));
		assertThat(posts.problems()).anySatisfy(problem -> assertThat(problem).contains("empty.md").contains("unlisted"));
		assertThat(posts.find("plain")).isPresent();
	}

	@Test
	@DisplayName("UNLISTED-18 an unlisted post that is a draft or dated in the future is not served")
	void notWhileHidden() {
		Pages now = pages();

		assertThat(now.post("hidden-draft")).isEmpty();
		assertThat(now.post("hidden-later")).isEmpty();
		assertThat(posts("2026-11-16T12:00:00Z").find("hidden-later")).isEmpty();

		Pages later = pages(posts("2026-12-02T00:00:00Z"), 10);
		assertThat(later.post("hidden-later")).isPresent();
		assertThat(later.post("hidden-draft")).isEmpty();
		assertThat(cards(later.index(1).orElseThrow().latest())).doesNotContain("/blog/hidden-later");
	}

	@Test
	@DisplayName("UNLISTED-19 a post whose unlisted is not true or false is named in a logged warning")
	void warns() {
		List<LogRecord> records = new CopyOnWriteArrayList<>();
		Handler handler = new Handler() {
			@Override
			public void publish(LogRecord record) {
				records.add(record);
			}

			@Override
			public void flush() {
			}

			@Override
			public void close() {
			}
		};
		Logger logger = Logger.getLogger(Posts.class.getName());
		logger.addHandler(handler);
		try {
			add("odd", "2026-11-09", "unlisted: yes");
			posts("2026-11-16T12:00:00Z").find("odd");
		}
		finally {
			logger.removeHandler(handler);
		}

		assertThat(records).anySatisfy(record -> {
			assertThat(record.getLevel()).isEqualTo(Level.WARNING);
			assertThat(record.getMessage()).contains("odd.md").contains("unlisted").contains("'yes'");
		});
	}

}
