package dev.danvega.beansprout.sample;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import dev.danvega.beansprout.export.ExportException;
import dev.danvega.beansprout.view.Pages;
import dev.danvega.beansprout.web.ThemeRenderer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.THROWABLE;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * The static export, run for real in the sample app: the files it writes, the ones it leaves out,
 * and what it refuses. One export serves most tests. The others run their own.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(OutputCaptureExtension.class)
class StaticExportTests {

	private static final String SITE = "https://example.com";

	private static final String POSTS = "classpath:test-posts-export/";

	private static final Pattern LOC = Pattern.compile("<loc>([^<]*)</loc>");

	private Path out;

	private ConfigurableApplicationContext exported;

	private Set<String> files;

	@BeforeAll
	void exportOnce() throws IOException {
		out = Files.createTempDirectory("beansprout-export").resolve("site");
		exported = export(out);
		files = files(out);
	}

	@AfterAll
	void close() {
		exported.close();
	}

	static ConfigurableApplicationContext export(Path out, String... extra) {
		List<String> args = new ArrayList<>(List.of("--beansprout.export.enabled=true", "--beansprout.export.output=" + out,
				"--beansprout.site-url=" + SITE, "--beansprout.posts.location=" + POSTS));
		// A second --name=value would join the first with a comma, so an extra one replaces it.
		for (String arg : extra) {
			String name = arg.substring(0, arg.indexOf('=') + 1);
			args.removeIf(existing -> existing.startsWith(name));
			args.add(arg);
		}
		return new SpringApplicationBuilder(SampleApplication.class).run(args.toArray(String[]::new));
	}

	/** The export's own failure, wherever Spring Boot wrapped it. */
	static Throwable exportFailure(Throwable thrown) {
		for (Throwable ex = thrown; ex != null; ex = ex.getCause()) {
			if (ex instanceof ExportException) {
				return ex;
			}
		}
		return thrown;
	}

	private static ConfigurableApplicationContext live(String... extra) {
		List<String> args = new ArrayList<>(List.of("--server.port=0", "--beansprout.site-url=" + SITE,
				"--beansprout.posts.location=" + POSTS));
		args.addAll(List.of(extra));
		return new SpringApplicationBuilder(SampleApplication.class).run(args.toArray(String[]::new));
	}

	/** Every file under a folder, as paths with / between their parts. */
	static Set<String> files(Path folder) throws IOException {
		Set<String> found = new TreeSet<>();
		if (!Files.exists(folder)) {
			return found;
		}
		try (Stream<Path> walk = Files.walk(folder)) {
			walk.filter(Files::isRegularFile)
				.forEach(file -> found.add(folder.relativize(file).toString().replace(FileSystems.getDefault().getSeparator(), "/")));
		}
		return found;
	}

	private String read(String file) throws IOException {
		return Files.readString(out.resolve(file), StandardCharsets.UTF_8);
	}

	@Test
	@DisplayName("EXPORT-1 with beansprout.export.enabled=true the app writes the blog to the output folder")
	void writesTheBlog() {
		assertThat(files).contains("blog.html", "blog/published.html", "blog/plain.html");
	}

	@Test
	@DisplayName("EXPORT-2 when the export finishes the app stops")
	void appStops() {
		assertThat(exported.isActive()).isFalse();
	}

	@Test
	@DisplayName("EXPORT-15 the export writes a .beansprout-export file at the root")
	void marker() {
		assertThat(files).contains(".beansprout-export");
	}

	@Test
	@DisplayName("EXPORT-16 every address in Pages.urls() has a file")
	void everyUrl() throws IOException {
		Matcher loc = LOC.matcher(read("blog/sitemap.xml"));
		List<String> urls = new ArrayList<>();
		while (loc.find()) {
			urls.add(loc.group(1).substring(SITE.length()));
		}
		assertThat(urls).contains("/blog", "/blog/archive", "/blog/published", "/blog/plain", "/blog/tags/export");
		assertThat(urls).allSatisfy(url -> assertThat(files).contains(url.substring(1) + ".html"));
	}

	@Test
	@DisplayName("EXPORT-17 the page at /a/b is written to a/b.html")
	void htmlFileNames() throws IOException {
		assertThat(files).contains("blog/archive.html", "blog/tags/export.html")
			.doesNotContain("blog/archive", "blog/plain", "blog/index.html");
		assertThat(read("blog/plain.html")).containsIgnoringCase("<!doctype html>").contains("A plain post");
	}

	@Test
	@DisplayName("EXPORT-18 the feed is written to blog/feed.xml")
	void feed() throws IOException {
		assertThat(read("blog/feed.xml")).contains("<feed").contains(SITE + "/blog/published");
	}

	@Test
	@DisplayName("EXPORT-19 the sitemap is written to blog/sitemap.xml")
	void sitemap() throws IOException {
		assertThat(read("blog/sitemap.xml")).contains("<urlset").contains("<loc>" + SITE + "/blog/plain</loc>");
	}

	@Test
	@DisplayName("EXPORT-20 the files a page loads are written: jar CSS, theme CSS, static pictures, bundle pictures")
	void pageFiles() {
		assertThat(files).contains("beansprout/code.css", "beansprout/longhand/longhand.css", "images/beansprout.png",
				"blog/published/cover.png", "export-test/style.css");
	}

	@Test
	@DisplayName("EXPORT-21 the files a stylesheet points to with url(...) or @import are written")
	void stylesheetFiles() {
		assertThat(files).contains("export-test/fonts/test.woff2", "export-test/bg.svg", "export-test/more.css",
				"export-test/dot.svg");
		assertThat(files).noneMatch(file -> file.contains("far.png") || file.contains("base64"));
	}

	@Test
	@DisplayName("EXPORT-22 a link that does not answer 200 fails the export and names it and its page")
	void deadLinkFails() throws IOException {
		Path folder = Files.createTempDirectory("beansprout-export").resolve("site");
		assertThatThrownBy(() -> export(folder, "--beansprout.posts.location=classpath:test-posts-broken/"))
			.extracting(StaticExportTests::exportFailure, THROWABLE)
			.hasMessageContaining("/blog/dead-links")
			.hasMessageContaining("404")
			.satisfies(ex -> assertThat(ex.getMessage()).containsAnyOf("/blog/no-such-post", "/images/no-such-picture.png"));
		assertThat(files(folder)).isEmpty();
	}

	@Test
	@DisplayName("EXPORT-23 a link to an app page outside the blog is left out with a warning")
	void appPageLeftOut(CapturedOutput output) throws IOException {
		assertThat(files).doesNotContain("export-test/about.html", "export-test/about.html.html");
		Path folder = Files.createTempDirectory("beansprout-export").resolve("site");
		export(folder).close();
		assertThat(output.getOut()).contains("/export-test/about.html").contains("/blog/plain");
	}

	@Test
	@DisplayName("EXPORT-24 404.html at the root holds the theme's not-found page")
	void notFoundPage() throws IOException {
		assertThat(read("404.html")).containsIgnoringCase("<!doctype html>").contains("Page not found");
	}

	@Test
	@DisplayName("EXPORT-25 404.html is rendered with an empty requested address")
	void notFoundHasNoAddress() throws IOException {
		try (ConfigurableApplicationContext app = live()) {
			String expected = app.getBean(ThemeRenderer.class)
				.render("not-found", app.getBean(Pages.class).forSite(SITE).notFound(""));
			assertThat(read("404.html")).isEqualTo(expected);
		}
	}

	@Test
	@DisplayName("EXPORT-27 index.html at the root sends the reader to the blog")
	void rootRedirect() throws IOException {
		assertThat(read("index.html")).contains("<meta http-equiv=\"refresh\" content=\"0; url=/blog\">")
			.contains("<a href=\"/blog\">");
	}

	@Test
	@DisplayName("EXPORT-28 an empty .nojekyll file is written at the root")
	void noJekyll() throws IOException {
		assertThat(Files.size(out.resolve(".nojekyll"))).isZero();
	}

	@Test
	@DisplayName("EXPORT-29 files are written at their address without the context path")
	void contextPath() throws IOException {
		Path folder = Files.createTempDirectory("beansprout-export").resolve("site");
		export(folder, "--server.servlet.context-path=/notes").close();
		Set<String> written = files(folder);
		assertThat(written).contains("blog.html", "blog/published.html", "blog/published/cover.png",
				"beansprout/longhand/longhand.css", "images/beansprout.png", "404.html")
			.noneMatch(file -> file.startsWith("notes/"));
		String post = Files.readString(folder.resolve("blog/published.html"));
		assertThat(post).contains("href=\"/notes/beansprout/longhand/longhand.css\"");
		assertThat(Files.readString(folder.resolve("index.html"))).contains("url=/notes/blog");
	}

	@Test
	@DisplayName("EXPORT-30 under a context path the links in a post's body get it, live and exported")
	void bodyLinksUnderContextPath() throws IOException {
		Path folder = Files.createTempDirectory("beansprout-export").resolve("site");
		export(folder, "--server.servlet.context-path=/notes").close();
		String post = Files.readString(folder.resolve("blog/published.html"));
		assertThat(post).contains("href=\"/notes/blog/plain\"")
			.contains("src=\"/notes/images/beansprout.png\"")
			.contains("href=\"/notes/export-test/style.css\"");
	}

	@Test
	@DisplayName("EXPORT-32 a draft gets no file for its page or its bundle")
	void noDrafts() throws IOException {
		assertThat(files).noneMatch(file -> file.contains("draft-bundle") || file.contains("secret.png"));
		assertThat(read("blog/sitemap.xml")).doesNotContain("draft-bundle");
	}

	@Test
	@DisplayName("EXPORT-33 a post dated after the export gets no file for its page or its bundle")
	void noFuturePosts() throws IOException {
		assertThat(files).noneMatch(file -> file.contains("future-bundle") || file.contains("later.png"));
		assertThat(read("blog/feed.xml")).contains("<id>" + SITE + "/blog/plain</id>")
			.doesNotContain("<id>" + SITE + "/blog/future-bundle</id>");
	}

	@Test
	@DisplayName("EXPORT-34 the MCP server's address gets no file, with the MCP starter on the classpath")
	void noMcp() {
		assertThat(ClassUtils.isPresent("org.springframework.ai.mcp.annotation.McpTool", null)).isTrue();
		assertThat(files).contains("blog.html").noneMatch(file -> file.contains("mcp"));
	}

	@Test
	@DisplayName("EXPORT-13 a folder with files and no .beansprout-export file is left as it is")
	void foreignFolder() throws IOException {
		Path folder = Files.createTempDirectory("beansprout-export");
		Files.writeString(folder.resolve("notes.txt"), "mine");
		assertThatThrownBy(() -> export(folder))
			.extracting(StaticExportTests::exportFailure, THROWABLE)
			.hasMessageContaining(folder.toString())
			.hasMessageContaining(".beansprout-export");
		assertThat(files(folder)).containsExactly("notes.txt");
		assertThat(Files.readString(folder.resolve("notes.txt"))).isEqualTo("mine");
	}

	@Test
	@DisplayName("EXPORT-14 a folder from an earlier export is emptied first, so a removed post leaves no file")
	void earlierExport() throws IOException {
		Path folder = Files.createTempDirectory("beansprout-export");
		Files.writeString(folder.resolve(".beansprout-export"), "");
		Files.createDirectories(folder.resolve("blog"));
		Files.writeString(folder.resolve("blog/removed-post.html"), "old");
		export(folder).close();
		assertThat(files(folder)).doesNotContain("blog/removed-post.html").contains("blog/plain.html");
	}

	@Test
	@DisplayName("EXPORT-35 a file the export cannot write fails it with the file and the cause")
	void cannotWrite() throws IOException {
		assumeTrue(FileSystems.getDefault().supportedFileAttributeViews().contains("posix"));
		Path folder = Files.createTempDirectory("beansprout-export");
		Files.setPosixFilePermissions(folder, PosixFilePermissions.fromString("r-xr-xr-x"));
		try {
			assertThatThrownBy(() -> export(folder))
			.extracting(StaticExportTests::exportFailure, THROWABLE)
				.hasMessageContaining("Could not write")
				.hasMessageContaining(folder.toString());
		}
		finally {
			Files.setPosixFilePermissions(folder, PosixFilePermissions.fromString("rwxr-xr-x"));
		}
	}

	@Test
	@DisplayName("EXPORT-5 an export runs while another program holds the app's port")
	void busyPort() throws IOException {
		try (ServerSocket busy = new ServerSocket(0)) {
			Path folder = Files.createTempDirectory("beansprout-export").resolve("site");
			export(folder, "--server.port=" + busy.getLocalPort()).close();
			assertThat(files(folder)).contains("blog.html");
		}
	}

	@Test
	@DisplayName("EXPORT-6 an export listens only on 127.0.0.1")
	void loopback() {
		assertThat(exported.getEnvironment().getProperty("server.address")).isEqualTo("127.0.0.1");
	}

	@Test
	@DisplayName("EXPORT-9 a finished export logs the folder and the number of files")
	void logsSummary(CapturedOutput output) throws IOException {
		Path folder = Files.createTempDirectory("beansprout-export").resolve("site");
		export(folder).close();
		int count = files(folder).size();
		assertThat(output.getOut()).contains("Exported " + count + " files to " + folder);
	}

	@Test
	@DisplayName("EXPORT-4 without beansprout.export.enabled the app writes nothing and keeps running")
	void offWritesNothing() throws IOException {
		Path folder = Files.createTempDirectory("beansprout-export").resolve("site");
		try (ConfigurableApplicationContext app = live("--beansprout.export.output=" + folder)) {
			assertThat(app.isActive()).isTrue();
			assertThat(folder).doesNotExist();
		}
	}

	@Test
	@DisplayName("EXPORT-23 a link to a draft, a post dated later or a redirect is left out with a warning, not a failure")
	void linksAheadLeftOut(CapturedOutput output) throws IOException {
		assertThat(files).contains("blog/links-ahead.html")
			.noneMatch(file -> file.startsWith("blog/page/") || file.contains("future-bundle") || file.contains("draft-bundle"));
		Path folder = Files.createTempDirectory("beansprout-export").resolve("site");
		export(folder).close();
		assertThat(output.getOut()).contains("left out /blog/future-bundle")
			.contains("left out /blog/draft-bundle")
			.contains("left out /blog/page/1");
	}

	@Test
	@DisplayName("EXPORT-20 a raw HTML src with no quotes, or with a space, is written too")
	void rawHtmlPictures() throws IOException {
		assertThat(files).contains("export-test/unquoted.svg", "export-test/my pic.svg");
		assertThat(Files.readString(out.resolve("export-test/my pic.svg"))).contains("width=\"4\"");
	}

	@Test
	@DisplayName("EXPORT-22 an address shown as code or plain text in a post is not followed")
	void codeIsNotFollowed() {
		assertThat(files).contains("blog/code-and-tools.html")
			.noneMatch(file -> file.contains("nope") || file.contains("nowhere") || file.contains("not-this")
					|| file.contains("commented-out"));
	}

	@Test
	@DisplayName("EXPORT-20 a picture with [ and ] in its name is written")
	void bracketsInAName() throws IOException {
		assertThat(files).contains("export-test/a[1].svg");
		assertThat(Files.readString(out.resolve("export-test/a[1].svg"))).contains("width=\"5\"");
	}

	@Test
	@DisplayName("EXPORT-34 a link to the MCP server is left out with a warning and never fetched")
	void mcpLinkLeftOut(CapturedOutput output) throws IOException {
		assertThat(files).contains("blog/code-and-tools.html").noneMatch(file -> file.contains("mcp"));
		Path folder = Files.createTempDirectory("beansprout-export").resolve("site");
		export(folder).close();
		assertThat(output.getOut()).contains("left out /mcp");
	}

	@Test
	@DisplayName("EXPORT-20 src= or href= inside another attribute's quoted value is not followed")
	void quotedValuesAreNotFollowed() {
		assertThat(files).contains("blog/code-and-tools.html", "export-test/bg.svg").noneMatch(file -> file.contains("nope"));
	}
}
