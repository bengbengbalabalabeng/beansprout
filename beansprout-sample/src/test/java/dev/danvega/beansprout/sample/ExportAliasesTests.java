package dev.danvega.beansprout.sample;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Aliases in the static export: a page at each old address that sends the reader on. */
class ExportAliasesTests {

	private static Path export(String... extra) throws IOException {
		Path out = Files.createTempDirectory("beansprout-export").resolve("site");
		String[] args = new String[extra.length + 1];
		args[0] = "--beansprout.posts.location=classpath:test-posts-aliases/";
		System.arraycopy(extra, 0, args, 1, extra.length);
		StaticExportTests.export(out, args).close();
		return out;
	}

	private static String read(Path file) throws IOException {
		return Files.readString(file, StandardCharsets.UTF_8);
	}

	@Test
	@DisplayName("ALIAS-13 the export writes a page at each live alias with a meta refresh and a canonical link to the post")
	void aliasPages() throws IOException {
		Path out = export();

		String page = read(out.resolve("old/hello.html"));
		assertThat(page).contains("<meta http-equiv=\"refresh\" content=\"0; url=/blog/hello\">",
				"<link rel=\"canonical\" href=\"https://example.com/blog/hello\">", "<a href=\"/blog/hello\">");
		assertThat(read(out.resolve("blog/2024-05-01-dated.html"))).contains("url=/blog/dated");
		assertThat(read(out.resolve("blog/hi.html"))).contains("url=/blog/hello");
		assertThat(out.resolve("draft-old.html")).as("a draft's alias").doesNotExist();
		assertThat(out.resolve("export-test/about.html")).as("the app's static file answers there").doesNotExist();
		assertThat(read(out.resolve("blog/hello.html"))).as("the post itself is still written").contains("Hello again");
	}

	@Test
	@DisplayName("ALIAS-13 under a context path an alias page sends the reader to the post under it")
	void contextPath() throws IOException {
		Path out = export("--server.servlet.context-path=/my-blog");

		assertThat(read(out.resolve("old/hello.html"))).contains("url=/my-blog/blog/hello",
				"href=\"https://example.com/my-blog/blog/hello\"");
	}

	@Test
	@DisplayName("ALIAS-14 an alias whose file the export already writes is left out, and the export still finishes")
	void clashingFiles() throws IOException {
		Path out = export();

		assertThat(read(out.resolve("index.html"))).as("the root page").doesNotContain("url=/blog/clash");
		assertThat(read(out.resolve("404.html"))).as("the not-found page").doesNotContain("url=/blog/clash");
		assertThat(read(out.resolve("blog.html"))).as("the blog's home page").doesNotContain("url=/blog/clash");
		assertThat(read(out.resolve("blog/clash.html"))).as("the post's own page").contains("Clashing aliases");
		assertThat(read(out.resolve("robots.txt"))).startsWith("User-agent: *");
		assertThat(read(out.resolve("pair.html"))).as("/pair and /pair.html share one file").contains("url=/blog/clash");
	}

	@Test
	@DisplayName("ALIAS-14 an alias with %XX escapes is written at the file name they spell")
	void percentEscapes() throws IOException {
		Path out = export();

		assertThat(read(out.resolve("café.html"))).contains("url=/blog/clash");
	}

	@Test
	@DisplayName("ALIAS-14 an alias page is written at the file its path names")
	void fileNames() throws IOException {
		Path out = export();

		assertThat(out.resolve("2019/03/14/hello.html")).as("/a/b.html").isRegularFile();
		assertThat(out.resolve("old/hello.html")).as("/a/b").isRegularFile();
		assertThat(out.resolve("posts/hello/index.html")).as("/a/b/").isRegularFile();
		assertThat(out.resolve("2019/03/14/hello.html.html")).doesNotExist();
	}

}
