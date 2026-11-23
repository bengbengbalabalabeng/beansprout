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

/** Posts that share a post's tags, from specs/related-posts.md. */
class RelatedPostsTests {

	private final Clock clock = Clock.fixed(Instant.parse("2026-11-16T12:00:00Z"), ZoneOffset.UTC);

	private final List<MarkdownFile> files = new ArrayList<>();

	private void add(String slug, String date, String frontmatter) {
		files.add(new MarkdownFile(slug + ".md", "---\ntitle: Post " + slug + "\ndate: " + date + "\n" + frontmatter
				+ "\n---\nThe body of " + slug + ".\n"));
	}

	private Pages pages(BlogSettings settings) {
		Posts posts = new Posts(() -> List.copyOf(files), new PostParser(ZoneOffset.UTC), clock, false);
		return new Pages(posts, new Links("", "/blog"), settings);
	}

	private static BlogSettings settings(int related) {
		return new BlogSettings("Bean Notes", "", 10, "", "", List.of(), related);
	}

	private List<PostLink> related(Pages pages, String slug) {
		return pages.post(slug).orElseThrow().post().related();
	}

	private List<PostLink> related(String slug) {
		return related(pages(settings(3)), slug);
	}

	private static List<String> titles(List<PostLink> links) {
		return links.stream().map(PostLink::title).toList();
	}

	@Test
	@DisplayName("RELATED-1 a post page gets up to beansprout.related-posts other published posts that share a tag")
	void upToTheSetting() {
		add("main", "2026-11-10", "tags: [java]");
		for (int day = 1; day <= 5; day++) {
			add("java-" + day, "2026-11-0" + day, "tags: [java]");
		}
		add("kotlin", "2026-11-06", "tags: [kotlin]");
		add("none", "2026-11-07", "");

		List<PostLink> three = related("main");
		assertThat(titles(three)).containsExactly("Post java-5", "Post java-4", "Post java-3");
		assertThat(three.getFirst().url()).isEqualTo("/blog/java-5");
		assertThat(titles(related(pages(settings(2)), "main"))).containsExactly("Post java-5", "Post java-4");
		assertThat(titles(related(pages(settings(10)), "main"))).containsExactly("Post java-5", "Post java-4",
				"Post java-3", "Post java-2", "Post java-1");
		assertThat(titles(related("kotlin"))).isEmpty();
	}

	@Test
	@DisplayName("RELATED-2 related posts are ranked by how many tags they share, most first")
	void mostSharedFirst() {
		add("main", "2026-11-10", "tags: [java, spring, testing]");
		add("one", "2026-11-09", "tags: [java]");
		add("three", "2026-11-01", "tags: [Testing, Spring, JAVA]");
		add("two", "2026-11-05", "tags: [spring, java, kotlin]");

		assertThat(titles(related("main"))).containsExactly("Post three", "Post two", "Post one");
	}

	@Test
	@DisplayName("RELATED-3 related posts that share as many tags are listed newest first")
	void newestFirstOnATie() {
		add("main", "2026-11-10", "tags: [java, spring]");
		add("old", "2026-10-01", "tags: [java]");
		add("new", "2026-11-09", "tags: [spring]");
		add("mid", "2026-10-15", "tags: [java]");
		add("newer-than-main", "2026-11-12", "tags: [java]");

		assertThat(titles(related("main"))).containsExactly("Post newer-than-main", "Post new", "Post mid");
	}

	@Test
	@DisplayName("RELATED-4 a post is never related to itself")
	void notItself() {
		add("main", "2026-11-10", "tags: [java, java, Java]");
		add("other", "2026-11-09", "tags: [java]");

		assertThat(titles(related("main"))).containsExactly("Post other");
		assertThat(titles(related("other"))).containsExactly("Post main");
	}

	@Test
	@DisplayName("RELATED-5 drafts and posts dated in the future are left out of related posts")
	void publishedOnly() {
		add("main", "2026-11-10", "tags: [java]");
		add("draft", "2026-11-09", "tags: [java]\ndraft: true");
		add("future", "2026-12-01", "tags: [java]");
		add("live", "2026-11-01", "tags: [java]");

		assertThat(titles(related("main"))).containsExactly("Post live");
	}

	@Test
	@DisplayName("RELATED-6 a post with no tags has no related posts")
	void noTags() {
		add("bare", "2026-11-10", "");
		add("empty", "2026-11-09", "tags: []");
		add("other", "2026-11-08", "tags: [java]");

		assertThat(related("bare")).isEmpty();
		assertThat(related("empty")).isEmpty();
		assertThat(titles(related("other"))).isEmpty();
	}

	@Test
	@DisplayName("RELATED-7 with beansprout.related-posts not set, a post gets up to 3 related posts")
	void threeByDefault() {
		for (int day = 1; day <= 6; day++) {
			add("java-" + day, "2026-11-0" + day, "tags: [java]");
		}
		assertThat(new BlogSettings("Bean Notes", "", 10).relatedPosts()).isEqualTo(3);
		assertThat(new BlogSettings("Bean Notes", "", 10, "Ines", "", List.of()).relatedPosts()).isEqualTo(3);
		assertThat(related(pages(new BlogSettings("Bean Notes", "", 10)), "java-6")).hasSize(3);
	}

	@Test
	@DisplayName("RELATED-8 with beansprout.related-posts at 0, no post gets related posts")
	void zeroTurnsItOff() {
		add("main", "2026-11-10", "tags: [java]");
		add("other", "2026-11-09", "tags: [java]");

		assertThat(related(pages(settings(0)), "main")).isEmpty();
		assertThat(related(pages(settings(0)), "other")).isEmpty();
	}

	@Test
	@DisplayName("RELATED-9 a beansprout.related-posts below 0 is refused with a message that names the setting")
	void belowZeroIsRefused() {
		assertThatIllegalArgumentException().isThrownBy(() -> settings(-1))
			.withMessageContaining("beansprout.related-posts")
			.withMessageContaining("-1");
	}

}
