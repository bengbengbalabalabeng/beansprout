package dev.danvega.beansprout.content;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

class PostsTests {

	private static final ZoneId UTC = ZoneOffset.UTC;

	private final MovableClock clock = new MovableClock(Instant.parse("2026-11-16T12:00:00Z"));

	private final List<MarkdownFile> files = new ArrayList<>();

	private Posts posts(boolean reload) {
		return new Posts(() -> List.copyOf(files), new PostParser(UTC), clock, reload);
	}

	private void add(String path, String frontmatter) {
		files.add(new MarkdownFile(path, "---\n" + frontmatter + "\n---\nBody of " + path + ".\n"));
	}

	@Test
	@DisplayName("POSTS-5 a bad file is skipped and the problem names it")
	void badFilesAreSkipped() {
		add("good.md", "title: Good\ndate: 2026-11-01");
		add("untitled.md", "date: 2026-11-01");
		files.add(new MarkdownFile("plain.md", "No frontmatter here."));

		Posts posts = posts(false);

		assertThat(posts.published()).extracting(Post::slug).containsExactly("good");
		assertThat(posts.problems()).hasSize(2)
			.anySatisfy(problem -> assertThat(problem).contains("untitled.md", "title"))
			.anySatisfy(problem -> assertThat(problem).contains("plain.md", "frontmatter"));
	}

	@Test
	@DisplayName("POSTS-6 two posts with one slug: the first path wins and the problem names both files")
	void duplicateSlugs() {
		add("b/hello.md", "title: Second\ndate: 2026-11-02");
		add("a/hello.md", "title: First\ndate: 2026-11-01");

		Posts posts = posts(false);

		assertThat(posts.published()).extracting(Post::title).containsExactly("First");
		assertThat(posts.problems()).singleElement().asString().contains("a/hello.md", "b/hello.md", "hello");
	}

	@Test
	@DisplayName("POSTS-7 an empty slug or a route word is skipped and the problem names the file")
	void reservedSlugs() {
		add("archive.md", "title: Archive\ndate: 2026-11-01");
		add("tags.md", "title: Tags\ndate: 2026-11-01");
		add("x.md", "title: Page\ndate: 2026-11-01\nslug: page");
		add("日本語.md", "title: Japanese\ndate: 2026-11-01");
		add("fine.md", "title: Fine\ndate: 2026-11-01");

		Posts posts = posts(false);

		assertThat(posts.published()).extracting(Post::slug).containsExactly("fine");
		assertThat(posts.problems()).hasSize(4)
			.anySatisfy(problem -> assertThat(problem).contains("archive.md"))
			.anySatisfy(problem -> assertThat(problem).contains("tags.md"))
			.anySatisfy(problem -> assertThat(problem).contains("x.md"))
			.anySatisfy(problem -> assertThat(problem).contains("日本語.md"));
	}

	@Test
	@DisplayName("POSTS-9 published posts are listed newest first")
	void newestFirst() {
		add("old.md", "title: Old\ndate: 2025-01-01");
		add("new.md", "title: New\ndate: 2026-11-01");
		add("middle.md", "title: Middle\ndate: 2026-03-14");

		assertThat(posts(false).published()).extracting(Post::slug).containsExactly("new", "middle", "old");
	}

	@Test
	@DisplayName("POSTS-13 posts are read from a folder and its subfolders, only .md files")
	void folderAndSubfolders(@TempDir Path dir) throws IOException {
		Files.createDirectories(dir.resolve("2026/11"));
		Files.writeString(dir.resolve("top.md"), "---\ntitle: Top\ndate: 2026-11-01\n---\n");
		Files.writeString(dir.resolve("2026/11/deep.md"), "---\ntitle: Deep\ndate: 2026-11-02\n---\n");
		Files.writeString(dir.resolve("notes.txt"), "not a post");

		Posts posts = new Posts(new DirectoryPostSource(dir), new PostParser(UTC), clock, false);

		assertThat(posts.published()).extracting(Post::slug).containsExactly("deep", "top");
		assertThat(posts.find("deep")).get().extracting(Post::path).isEqualTo("2026/11/deep.md");
	}

	@Test
	@DisplayName("POSTS-13 a folder that does not exist means no posts, not a crash")
	void missingFolder(@TempDir Path dir) {
		Posts posts = new Posts(new DirectoryPostSource(dir.resolve("nope")), new PostParser(UTC), clock, false);

		assertThat(posts.published()).isEmpty();
	}

	@Test
	@DisplayName("POSTS-14 with reload on, added, changed and removed files show on the next call")
	void reloadSeesChanges(@TempDir Path dir) throws IOException {
		Path one = dir.resolve("one.md");
		Files.writeString(one, "---\ntitle: One\ndate: 2026-11-01\n---\n");
		Posts reloading = new Posts(new DirectoryPostSource(dir), new PostParser(UTC), clock, true);
		Posts cached = new Posts(new DirectoryPostSource(dir), new PostParser(UTC), clock, false);
		assertThat(reloading.published()).hasSize(1);
		assertThat(cached.published()).hasSize(1);

		Files.writeString(dir.resolve("two.md"), "---\ntitle: Two\ndate: 2026-11-02\n---\n");
		Files.writeString(one, "---\ntitle: One, edited\ndate: 2026-11-01\n---\n");
		assertThat(reloading.published()).extracting(Post::title).containsExactly("Two", "One, edited");
		assertThat(cached.published()).extracting(Post::title).containsExactly("One");

		Files.delete(one);
		assertThat(reloading.published()).extracting(Post::title).containsExactly("Two");
	}

	@Test
	@DisplayName("POSTS-16 a draft is not listed and not found by slug")
	void draftsAreHidden() {
		add("draft.md", "title: Draft\ndate: 2026-11-01\ndraft: true");
		add("live.md", "title: Live\ndate: 2026-11-01\ndraft: false");

		Posts posts = posts(false);

		assertThat(posts.published()).extracting(Post::slug).containsExactly("live");
		assertThat(posts.find("draft")).isEmpty();
		assertThat(posts.find("live")).isPresent();
	}

	@Test
	@DisplayName("POSTS-17 a post dated in the future appears at that moment, with no reload")
	void scheduledPosts() {
		add("later.md", "title: Later\ndate: 2026-11-16T15:00:00Z");
		add("now.md", "title: Now\ndate: 2026-11-16T12:00:00Z");
		Posts posts = posts(false);

		assertThat(posts.published()).extracting(Post::slug).containsExactly("now");
		assertThat(posts.find("later")).isEmpty();

		clock.advance(Duration.ofHours(3));

		assertThat(posts.published()).extracting(Post::slug).containsExactly("later", "now");
		assertThat(posts.find("later")).isPresent();
	}

	@Test
	@DisplayName("POSTS-18 a file with an odd draft value is skipped and the problem names it")
	void oddDraftIsSkipped() {
		add("odd.md", "title: Odd\ndate: 2026-11-01\ndraft: yes please");

		Posts posts = posts(false);

		assertThat(posts.published()).isEmpty();
		assertThat(posts.problems()).singleElement().asString().contains("odd.md", "draft");
	}

	@Test
	@DisplayName("PAGES-8 tags are grouped by slug and keep the spelling of the newest post")
	void tagsGroupBySlug() {
		add("old.md", "title: Old\ndate: 2026-01-01\ntags: [spring boot]");
		add("new.md", "title: New\ndate: 2026-11-01\ntags: [Spring Boot, Java]");

		Posts posts = posts(false);

		assertThat(posts.tags()).extracting(TagSummary::slug, TagSummary::name, TagSummary::count)
			.containsExactly(
					org.assertj.core.groups.Tuple.tuple("spring-boot", "Spring Boot", 2),
					org.assertj.core.groups.Tuple.tuple("java", "Java", 1));
		assertThat(posts.tagged("spring-boot")).extracting(Post::slug).containsExactly("new", "old");
	}

	@Test
	@DisplayName("PAGES-9 tags are counted from published posts only, most used first")
	void tagCountsSkipHiddenPosts() {
		add("a.md", "title: A\ndate: 2026-11-01\ntags: [java]");
		add("b.md", "title: B\ndate: 2026-11-02\ntags: [spring, java]");
		add("c.md", "title: C\ndate: 2026-11-03\ntags: [spring]\ndraft: true");
		add("d.md", "title: D\ndate: 2027-01-01\ntags: [spring]");

		assertThat(posts(false).tags()).extracting(TagSummary::slug, TagSummary::count)
			.containsExactly(
					org.assertj.core.groups.Tuple.tuple("java", 2),
					org.assertj.core.groups.Tuple.tuple("spring", 1));
	}

	/** A clock a test can move forward. */
	static final class MovableClock extends Clock {

		private Instant now;

		MovableClock(Instant now) {
			this.now = now;
		}

		void advance(Duration duration) {
			now = now.plus(duration);
		}

		@Override
		public ZoneId getZone() {
			return UTC;
		}

		@Override
		public Clock withZone(ZoneId zone) {
			return this;
		}

		@Override
		public Instant instant() {
			return now;
		}

	}

}
