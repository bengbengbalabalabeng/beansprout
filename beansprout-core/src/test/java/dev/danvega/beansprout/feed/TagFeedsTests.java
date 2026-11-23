package dev.danvega.beansprout.feed;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import dev.danvega.beansprout.code.CodeHighlighter;
import dev.danvega.beansprout.content.DirectoryPostSource;
import dev.danvega.beansprout.content.MarkdownFile;
import dev.danvega.beansprout.content.PostParser;
import dev.danvega.beansprout.content.Posts;
import dev.danvega.beansprout.view.Aliases;
import dev.danvega.beansprout.view.BlogSettings;
import dev.danvega.beansprout.view.Links;
import dev.danvega.beansprout.view.Pages;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

/** A feed for each tag, from specs/tag-feeds.md. */
class TagFeedsTests {

	private static final String SITE = "https://example.com";

	private final Clock clock = Clock.fixed(Instant.parse("2026-11-16T12:00:00Z"), ZoneOffset.UTC);

	private final List<MarkdownFile> files = new ArrayList<>();

	private Feeds feeds(String root, String title) {
		Posts posts = new Posts(() -> List.copyOf(files), new PostParser(ZoneOffset.UTC, CodeHighlighter.NONE, root, "/blog"),
				clock, false);
		Links links = new Links(root, "/blog");
		return new Feeds(posts, links, new Pages(posts, links, new BlogSettings(title, "Notes on beans", 10, "Ines", "")));
	}

	private Feeds feeds() {
		return feeds("", "Bean Notes");
	}

	private void add(String slug, String date, String frontmatter) {
		files.add(new MarkdownFile(slug + ".md", "---\ntitle: Post " + slug + "\ndate: " + date + "\n" + frontmatter
				+ "\n---\nThe body of " + slug + ".\n"));
	}

	private static List<String> entryTitles(String atom) {
		return Pattern.compile("<entry>\\s*<title>([^<]*)</title>").matcher(atom).results().map(m -> m.group(1)).toList();
	}

	/** The entry whose title is given, from {@code <entry>} to {@code </entry>}. */
	private static String entry(String atom, String title) {
		int at = atom.indexOf("<title>" + title + "</title>");
		assertThat(at).as(title).isNotNegative();
		int start = atom.lastIndexOf("<entry>", at);
		return atom.substring(start, atom.indexOf("</entry>", at) + "</entry>".length());
	}

	/** The feed before its first entry. */
	private static String head(String atom) {
		int entry = atom.indexOf("<entry>");
		return entry < 0 ? atom : atom.substring(0, entry);
	}

	@Test
	@DisplayName("TAGFEED-2 a tag's feed lists the 20 newest published posts with that tag")
	void newestTwenty() {
		for (int day = 1; day <= 25; day++) {
			add("java-" + day, String.format("2026-10-%02d", day), "tags: [Java]");
		}
		add("kotlin", "2026-11-01", "tags: [kotlin]");
		add("draft", "2026-11-02", "tags: [Java]\ndraft: true");
		add("future", "2026-12-01", "tags: [Java]");
		add("both", "2026-11-03", "tags: [kotlin, java]");

		String atom = feeds().tagAtom("java", SITE).orElseThrow();

		List<String> titles = entryTitles(atom);
		assertThat(titles).hasSize(20);
		assertThat(titles.getFirst()).isEqualTo("Post both");
		assertThat(titles.get(1)).isEqualTo("Post java-25");
		assertThat(titles.getLast()).isEqualTo("Post java-7");
		assertThat(titles).doesNotContain("Post kotlin", "Post draft", "Post future");
	}

	@Test
	@DisplayName("TAGFEED-3 a tag's feed is titled with the blog's title and the tag's name")
	void title() {
		add("one", "2026-11-01", "tags: [Spring Boot]");
		add("two", "2026-11-02", "tags: [\"C&C++\"]");

		assertThat(head(feeds().tagAtom("spring-boot", SITE).orElseThrow()))
			.contains("<title>Bean Notes: Spring Boot</title>");
		assertThat(head(feeds("", "Beans & <Notes>").tagAtom("c-c", SITE).orElseThrow()))
			.contains("<title>Beans &amp; &lt;Notes&gt;: C&amp;C++</title>");
	}

	@Test
	@DisplayName("TAGFEED-4 each entry of a tag's feed matches its entry in the main feed")
	void sameEntries(@TempDir Path folder) throws IOException {
		add("guest", "2026-11-01", "tags: [java, beans]\nauthor: Jane & Joe\nsummary: A <b>short</b> summary");
		add("plain", "2026-11-02", "tags: [java]\nupdated: 2026-11-05");
		Feeds feeds = feeds();
		String main = feeds.atom(SITE);
		String tag = feeds.tagAtom("java", SITE).orElseThrow();
		for (String title : List.of("Post guest", "Post plain")) {
			assertThat(entry(tag, title)).as(title).isEqualTo(entry(main, title));
		}

		Path index = folder.resolve("shots/index.md");
		Files.createDirectories(index.getParent());
		Files.writeString(index, "---\ntitle: Shots\ndate: 2026-11-03\ntags: [java]\n---\n![A shot](a.png)\n");
		Posts bundles = new Posts(new DirectoryPostSource(folder),
				new PostParser(ZoneOffset.UTC, CodeHighlighter.NONE, "/app", "/blog"), clock, false);
		Links links = new Links("/app", "/blog");
		Feeds withImages = new Feeds(bundles, links, new Pages(bundles, links, new BlogSettings("Bean Notes", "", 10)));
		String shots = entry(withImages.tagAtom("java", SITE).orElseThrow(), "Shots");
		assertThat(shots).isEqualTo(entry(withImages.atom(SITE), "Shots"))
			.contains("src=&quot;https://example.com/app/blog/shots/a.png&quot;");
	}

	@Test
	@DisplayName("TAGFEED-5 a tag's feed has a self link to its own address")
	void selfLink() {
		add("one", "2026-11-01", "tags: [Spring Boot]");

		String atom = head(feeds().tagAtom("spring-boot", SITE + "/").orElseThrow());
		assertThat(atom).contains("<link rel=\"self\" href=\"https://example.com/blog/tags/spring-boot/feed.xml\"/>")
			.contains("<link href=\"https://example.com/blog/tags/spring-boot\"/>")
			.doesNotContain("href=\"https://example.com/blog/feed.xml\"");
		assertThat(head(feeds("/app", "Bean Notes").tagAtom("spring-boot", SITE).orElseThrow()))
			.contains("<link rel=\"self\" href=\"https://example.com/app/blog/tags/spring-boot/feed.xml\"/>");
	}

	@Test
	@DisplayName("TAGFEED-6 a tag no published post has gets no feed")
	void noFeed() {
		add("one", "2026-11-01", "tags: [java]");
		add("draft", "2026-11-02", "tags: [secret]\ndraft: true");
		add("future", "2026-12-01", "tags: [later]");

		Feeds feeds = feeds();
		assertThat(feeds.tagAtom("java", SITE)).isPresent();
		for (String slug : List.of("nope", "secret", "later", "Java", "", "java/feed")) {
			assertThat(feeds.tagAtom(slug, SITE)).as(slug).isEmpty();
		}
	}

	@Test
	@DisplayName("ALIAS-7 an alias at a live tag's feed is a page the blog serves, so it is ignored with a warning")
	void aliasAtTagFeed() {
		add("hello", "2026-11-01", "tags: [java]\naliases: [/blog/tags/java/feed.xml, /blog/tags/gone/feed.xml]");
		Posts posts = new Posts(() -> List.copyOf(files), new PostParser(ZoneOffset.UTC), clock, false);
		Aliases aliases = new Aliases(new Pages(posts, new Links("", "/blog"), new BlogSettings("Bean Notes", "", 10)));

		assertThat(aliases.all()).extracting(alias -> alias.path()).containsExactly("/blog/tags/gone/feed.xml");
		assertThat(aliases.problems()).singleElement().asString().contains("hello.md", "/blog/tags/java/feed.xml");
	}

}
