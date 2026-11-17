package dev.danvega.beansprout.content;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import dev.danvega.beansprout.code.CodeHighlighter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

/** A post as a folder: {@code index.md} and the images next to it. */
class PostBundlesTests {

	private final Clock clock = Clock.fixed(Instant.parse("2026-11-17T12:00:00Z"), ZoneOffset.UTC);

	@TempDir
	Path posts;

	private Posts posts(String root) {
		return new Posts(new DirectoryPostSource(posts), new PostParser(ZoneOffset.UTC, CodeHighlighter.NONE, root, "/blog"),
				clock, false);
	}

	private Posts posts() {
		return posts("");
	}

	private void write(String path, String text) throws IOException {
		Path file = posts.resolve(path);
		Files.createDirectories(file.getParent());
		Files.writeString(file, text);
	}

	private void post(String path, String extra, String body) throws IOException {
		write(path, "---\ntitle: A post\ndate: 2026-11-10\n" + extra + "---\n" + body + "\n");
	}

	@Test
	@DisplayName("BUNDLE-1 a folder's index.md is read as one post")
	void indexIsAPost() throws IOException {
		post("welcome/index.md", "", "Hello.");

		assertThat(posts().published()).extracting(Post::path).containsExactly("welcome/index.md");
	}

	@Test
	@DisplayName("BUNDLE-2 a bundle with no slug is named after its folder")
	void slugFromFolder() throws IOException {
		post("2026/welcome-to-wrap/index.md", "", "Hello.");
		post("release.md/index.md", "", "A folder whose name ends in .md.");

		assertThat(posts().published()).extracting(Post::slug).containsExactlyInAnyOrder("welcome-to-wrap", "release-md");
	}

	@Test
	@DisplayName("BUNDLE-3 a bundle's slug frontmatter wins over its folder's name")
	void slugFromFrontmatter() throws IOException {
		post("welcome/index.md", "slug: hello-there\n", "Hello.");

		assertThat(posts().published()).extracting(Post::slug).containsExactly("hello-there");
	}

	@Test
	@DisplayName("BUNDLE-4 other Markdown files inside a bundle are not posts")
	void otherMarkdownInBundleIsNotAPost() throws IOException {
		post("welcome/index.md", "", "Hello.");
		post("welcome/notes.md", "", "Notes.");
		post("welcome/old/draft.md", "", "Old.");
		post("welcome/inner/index.md", "", "Inner.");

		assertThat(posts().published()).extracting(Post::path).containsExactly("welcome/index.md");
		assertThat(posts().problems()).isEmpty();
	}

	@Test
	@DisplayName("BUNDLE-5 a Markdown file outside any bundle stays a post named after its file")
	void plainPostsStillWork() throws IOException {
		post("hello.md", "", "![A](shots/a.png)");
		post("2026/notes.md", "", "Notes.");
		post("welcome/index.md", "", "Hello.");

		assertThat(posts().published()).extracting(Post::slug).containsExactlyInAnyOrder("hello", "notes", "welcome");
		assertThat(posts().find("hello")).get().extracting(Post::html).asString().contains("src=\"shots/a.png\"");
	}

	@Test
	@DisplayName("BUNDLE-15 a relative image in a bundle's body is linked from the app's root, with the context path")
	void relativeBodyImages() throws IOException {
		post("welcome/index.md", "", "![A](shots/a.png)\n\n![B](./b.jpg)\n\n![C](my%20pic.png)\n\n![D](d.png?v=2)\n\n"
				+ "<img src=\"shots/raw.png\" alt=\"Raw\">\n\n`<img src=\"code.png\">`\n\n![E](100%.png)\n\n"
				+ "<img src = 'spaced.png'>\n\n<img src=bare.png>\n\n<img alt=\"a>b src='no.png'\" src=\"real.png\">\n\n"
				+ "<img src=\" /root.png\">\n\n![F](shots/../up.png)\n\n![G](shots/./g.png)\n\n![H](a;b.png)\n\n"
				+ "![I](../outside.png)\n\n<img src=\" spaced-in.png \">\n\n<IMG SRC=\"upper.png\">\n\n"
				+ "<img src=\"data:image/png;base64,AAAA\">\n\n<img src=\"&#46;&#46;/secret.png\">\n\n![J](a&b.png)\n\n"
				+ "<img src=\"&#x2e;/k.png\">");

		assertThat(posts().published().getFirst().html()).contains("src=\"/blog/welcome/shots/a.png\"")
			.contains("src=\"/blog/welcome/b.jpg\"")
			.contains("src=\"/blog/welcome/my%20pic.png\"")
			.contains("src=\"/blog/welcome/d.png?v=2\"")
			.contains("src=\"/blog/welcome/shots/raw.png\"")
			.contains("&lt;img src=&quot;code.png&quot;&gt;")
			.contains("src=\"/blog/welcome/100%25.png\"")
			.contains("src=\"/blog/welcome/spaced.png\"")
			.contains("src=\"/blog/welcome/bare.png\"")
			.contains("alt=\"a>b src='no.png'\" src=\"/blog/welcome/real.png\"")
			.contains("src=\" /root.png\"")
			.contains("src=\"/blog/welcome/up.png\"")
			.contains("src=\"/blog/welcome/shots/g.png\"")
			.contains("src=\"/blog/welcome/a%3Bb.png\"")
			.contains("src=\"../outside.png\"")
			.contains("src=\"/blog/welcome/spaced-in.png\"")
			.contains("src=\"/blog/welcome/upper.png\"")
			.contains("src=\"data:image/png;base64,AAAA\"")
			.contains("src=\"&#46;&#46;/secret.png\"")
			.contains("src=\"/blog/welcome/a&amp;b.png\"")
			.contains("src=\"/blog/welcome/k.png\"");
		assertThat(posts("/app").published().getFirst().html()).contains("src=\"/app/blog/welcome/shots/a.png\"");
	}

	@Test
	@DisplayName("BUNDLE-16 an image in a bundle's body with a / path or a full address is left as written")
	void absoluteBodyImages() throws IOException {
		post("welcome/index.md", "", "![A](/images/a.png)\n\n![B](https://example.com/b.png)");

		assertThat(posts().published().getFirst().html()).contains("src=\"/images/a.png\"")
			.contains("src=\"https://example.com/b.png\"");
	}

	@Test
	@DisplayName("BUNDLE-17 a bundle's relative picture is the file in its folder")
	void relativePicture() throws IOException {
		post("welcome/index.md", "picture: cover.png\npicture-alt: The cover\n", "Hello.");
		write("welcome/cover.png", "png");
		post("named/index.md", "slug: renamed\npicture: my cover.png\n", "Hello.");
		write("named/my cover.png", "png");
		post("semi/index.md", "picture: a;b.png\n", "Hello.");
		write("semi/a;b.png", "png");

		assertThat(posts().published()).extracting(Post::image)
			.containsExactlyInAnyOrder("/blog/welcome/cover.png", "/blog/renamed/my%20cover.png", "/blog/semi/a%3Bb.png");
	}

	@Test
	@DisplayName("BUNDLE-18 a bundle whose relative picture is not in its folder is skipped with a warning that names the file")
	void missingPicture() throws IOException {
		post("welcome/index.md", "picture: cover.png\n", "Hello.");
		write("cover.png", "a picture next to the folder, not in it");
		write("other/cover.png", "a picture in another folder");
		post("up/index.md", "picture: ../cover.png\n", "Up.");
		post("linked/index.md", "picture: link.png\n", "Linked.");
		Files.createSymbolicLink(posts.resolve("linked/link.png"), posts.resolve("other/cover.png"));

		Posts posts = posts();
		assertThat(posts.published()).isEmpty();
		assertThat(posts.problems()).hasSize(3)
			.anySatisfy(problem -> assertThat(problem).contains("welcome/index.md").contains("cover.png"))
			.anySatisfy(problem -> assertThat(problem).contains("up/index.md").contains("../cover.png"))
			.anySatisfy(problem -> assertThat(problem).contains("linked/index.md").contains("link.png"));
	}

}
