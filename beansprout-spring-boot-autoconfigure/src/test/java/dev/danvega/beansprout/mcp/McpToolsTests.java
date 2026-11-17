package dev.danvega.beansprout.mcp;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import dev.danvega.beansprout.code.CodeHighlighter;
import dev.danvega.beansprout.content.DirectoryPostSource;
import dev.danvega.beansprout.content.Post;
import dev.danvega.beansprout.content.PostParser;
import dev.danvega.beansprout.content.Posts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** The tools an agent calls, on a folder of posts. */
class McpToolsTests {

	private final Clock clock = Clock.fixed(Instant.parse("2026-11-17T12:00:00Z"), ZoneOffset.UTC);

	@TempDir
	Path folder;

	private Posts posts;

	private BlogTools blog;

	private DraftTools drafts;

	@BeforeEach
	void posts() throws IOException {
		write("hello.md", "---\ntitle: Hello\ndate: 2026-11-01\ntags: [java, Spring Boot]\nsummary: The first post.\n---\nHi *there*.\n");
		write("2026/newer.md", "---\ntitle: Newer\ndate: 2026-11-10\nupdated: 2026-11-12\ntags: [java]\npicture: /images/a.png\npicture-alt: A\n---\nNewer body.\n");
		write("secret.md", "---\ntitle: Secret\ndate: 2026-11-05\ndraft: true\n---\nNot yet.\n");
		write("later.md", "---\ntitle: Later\ndate: 2999-01-01\n---\nOne day.\n");
		DirectoryPostSource source = new DirectoryPostSource(folder);
		posts = new Posts(source, new PostParser(ZoneOffset.UTC, CodeHighlighter.NONE), clock, false);
		blog = new BlogTools(posts, source, () -> "https://example.com", new dev.danvega.beansprout.view.Links("", "/blog"));
		drafts = new DraftTools(folder, posts);
	}

	private void write(String path, String text) throws IOException {
		Path file = folder.resolve(path);
		Files.createDirectories(file.getParent());
		Files.writeString(file, text);
	}

	@Test
	@DisplayName("MCP-4 list_posts returns published posts newest first, with slug, title, date, tags, summary and address")
	void listPosts() {
		List<BlogTools.PostSummary> list = blog.listPosts();

		assertThat(list).extracting(BlogTools.PostSummary::slug).containsExactly("newer", "hello");
		BlogTools.PostSummary hello = list.get(1);
		assertThat(hello.title()).isEqualTo("Hello");
		assertThat(hello.date()).isEqualTo("2026-11-01");
		assertThat(hello.tags()).containsExactly("java", "Spring Boot");
		assertThat(hello.summary()).isEqualTo("The first post.");
		assertThat(hello.url()).isEqualTo("https://example.com/blog/hello");
	}

	@Test
	@DisplayName("MCP-5 list_tags returns the tags on published posts with their counts, most used first")
	void listTags() {
		assertThat(blog.listTags()).extracting(BlogTools.TagCount::name, BlogTools.TagCount::posts)
			.containsExactly(org.assertj.core.groups.Tuple.tuple("java", 2), org.assertj.core.groups.Tuple.tuple("Spring Boot", 1));
	}

	@Test
	@DisplayName("MCP-6 find_posts_by_tag finds published posts by the tag's name or its slug")
	void findByTag() {
		assertThat(blog.findPostsByTag("Spring Boot")).extracting(BlogTools.PostSummary::slug).containsExactly("hello");
		assertThat(blog.findPostsByTag("spring-boot")).extracting(BlogTools.PostSummary::slug).containsExactly("hello");
		assertThat(blog.findPostsByTag("java")).extracting(BlogTools.PostSummary::slug).containsExactly("newer", "hello");
		assertThat(blog.findPostsByTag("rust")).isEmpty();
	}

	@Test
	@DisplayName("MCP-7 read_post returns a published post's values and its Markdown body as written")
	void readPost() {
		BlogTools.PostText post = blog.readPost("newer");

		assertThat(post.title()).isEqualTo("Newer");
		assertThat(post.date()).isEqualTo("2026-11-10");
		assertThat(post.updated()).isEqualTo("2026-11-12");
		assertThat(post.tags()).containsExactly("java");
		assertThat(post.picture()).isEqualTo("/images/a.png");
		assertThat(post.markdown()).isEqualTo("Newer body.\n");
		assertThat(blog.readPost("hello").markdown()).isEqualTo("Hi *there*.\n");
	}

	@Test
	@DisplayName("MCP-8 read_post says there is no such post for a draft, a future post or an unknown slug")
	void readHidden() {
		for (String slug : List.of("secret", "later", "nope")) {
			assertThatThrownBy(() -> blog.readPost(slug)).hasMessageContaining("No published post").hasMessageContaining(slug)
				.hasMessageNotContaining("Not yet").hasMessageNotContaining("One day");
		}
	}

	@Test
	@DisplayName("MCP-11 create_draft writes a new post file with the title, today's date, tags, summary and draft: true")
	void createDraft() throws IOException {
		DraftTools.Draft draft = drafts.createDraft("Why \"JTE\" \\ wins", "Some *Markdown*.\n", List.of("java", "a, b"),
				"A short \"summary\".");

		Path file = folder.resolve(draft.file());
		assertThat(file).exists();
		assertThat(Files.readString(file)).contains("draft: true", "date: 2026-11-17");
		Post read = new PostParser(ZoneOffset.UTC).parse(new dev.danvega.beansprout.content.MarkdownFile(draft.file(), Files.readString(file)));
		assertThat(read.title()).isEqualTo("Why \"JTE\" \\ wins");
		assertThat(read.tags()).containsExactly("java", "a, b");
		assertThat(read.lede()).isEqualTo("A short \"summary\".");
		assertThat(read.draft()).isTrue();
		assertThat(read.html()).contains("<em>Markdown</em>");
	}

	@Test
	@DisplayName("MCP-11 create_draft skips tags the blog cannot show, survives a missing tag, and says which it skipped")
	void oddTags() throws IOException {
		List<String> tags = new java.util.ArrayList<>(List.of("java", "日本語", "!!!"));
		tags.add(null);
		DraftTools.Draft draft = drafts.createDraft("Odd tags", "Body.\n", tags, null);

		Post read = new PostParser(ZoneOffset.UTC)
			.parse(new dev.danvega.beansprout.content.MarkdownFile(draft.file(), Files.readString(folder.resolve(draft.file()))));
		assertThat(read.tags()).containsExactly("java");
		assertThat(Files.readString(folder.resolve(draft.file()))).doesNotContain("日本語", "!!!");
		assertThat(draft.note()).contains("日本語", "!!!");
	}

	@Test
	@DisplayName("MCP-13 a very long title gives a file name the disk can hold")
	void longTitle() throws IOException {
		DraftTools.Draft draft = drafts.createDraft("word ".repeat(100), "Body.\n", List.of(), null);

		assertThat(draft.file().length()).isLessThanOrEqualTo(83);
		assertThat(draft.file()).endsWith(".md").doesNotContain("-.md");
		assertThat(folder.resolve(draft.file())).exists();
	}

	@Test
	@DisplayName("MCP-12 create_draft always writes draft: true, even when the Markdown says otherwise")
	void alwaysDraft() throws IOException {
		DraftTools.Draft draft = drafts.createDraft("Sneaky", "---\ndraft: false\n---\nBody.\n", List.of(), null);

		Post read = new PostParser(ZoneOffset.UTC)
			.parse(new dev.danvega.beansprout.content.MarkdownFile(draft.file(), Files.readString(folder.resolve(draft.file()))));
		assertThat(read.draft()).isTrue();
		assertThat(posts.find("sneaky")).isEmpty();
	}

	@Test
	@DisplayName("MCP-13 create_draft names the file from the title's slug, and nothing the caller sends names a path")
	void nameFromTitle() throws IOException {
		DraftTools.Draft draft = drafts.createDraft("../../Escape: the /etc/passwd!", "Body.\n", List.of(), null);

		assertThat(draft.file()).isEqualTo("escape-the-etc-passwd.md");
		assertThat(folder.resolve(draft.file())).exists();
		try (var files = Files.list(folder.getParent())) {
			assertThat(files.filter(path -> path.getFileName().toString().endsWith(".md"))).isEmpty();
		}
		assertThatThrownBy(() -> drafts.createDraft("!!!", "Body.\n", List.of(), null)).hasMessageContaining("title");
	}

	@Test
	@DisplayName("MCP-14 create_draft writes nothing when a post already has that slug, and names the file in the way")
	void noOverwrite() throws IOException {
		String before = Files.readString(folder.resolve("hello.md"));
		long files;
		try (var walk = Files.walk(folder)) {
			files = walk.count();
		}

		assertThatThrownBy(() -> drafts.createDraft("Hello", "New body.\n", List.of(), null)).hasMessageContaining("hello.md");
		assertThatThrownBy(() -> drafts.createDraft("Secret", "New body.\n", List.of(), null)).hasMessageContaining("secret.md");
		assertThatThrownBy(() -> drafts.createDraft("Newer", "New body.\n", List.of(), null)).hasMessageContaining("2026/newer.md");
		assertThatThrownBy(() -> drafts.createDraft("Page", "New body.\n", List.of(), null)).hasMessageContaining("page");
		assertThat(Files.readString(folder.resolve("hello.md"))).isEqualTo(before);
		try (var walk = Files.walk(folder)) {
			assertThat(walk.count()).as("no new file").isEqualTo(files);
		}
	}

}
