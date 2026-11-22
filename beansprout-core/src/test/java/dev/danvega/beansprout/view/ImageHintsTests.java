package dev.danvega.beansprout.view;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import javax.imageio.ImageIO;

import dev.danvega.beansprout.code.CodeHighlighter;
import dev.danvega.beansprout.content.DirectoryPostSource;
import dev.danvega.beansprout.content.MarkdownFile;
import dev.danvega.beansprout.content.PostParser;
import dev.danvega.beansprout.content.PostSource;
import dev.danvega.beansprout.content.Posts;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

/** Loading and size hints on a post's pictures, from specs/image-hints.md. */
class ImageHintsTests {

	private final Clock clock = Clock.fixed(Instant.parse("2026-11-17T12:00:00Z"), ZoneOffset.UTC);

	@TempDir
	Path posts;

	private Pages pages(PostSource source) {
		Posts all = new Posts(source, new PostParser(ZoneOffset.UTC, CodeHighlighter.NONE, "", "/blog"), clock, false);
		return new Pages(all, new Links("", "/blog"), new BlogSettings("Notes", "", 10)).forSite("https://example.com");
	}

	private Pages pages() {
		return pages(new DirectoryPostSource(posts));
	}

	private Post page(String slug) {
		return pages().post(slug).orElseThrow().post();
	}

	private String body(String slug) {
		return page(slug).body();
	}

	private void post(String path, String extra, String body) throws IOException {
		write(path, ("---\ntitle: A post\ndate: 2026-11-10\n" + extra + "---\n" + body + "\n").getBytes(StandardCharsets.UTF_8));
	}

	private void write(String path, byte[] bytes) throws IOException {
		Path file = posts.resolve(path);
		Files.createDirectories(file.getParent());
		Files.write(file, bytes);
	}

	private void image(String path, String format, int width, int height) throws IOException {
		Path file = posts.resolve(path);
		Files.createDirectories(file.getParent());
		assertThat(ImageIO.write(new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB), format, file.toFile()))
			.as(format)
			.isTrue();
	}

	/** The {@code <img>} tag whose alt text is given. */
	private static String img(String html, String alt) {
		int at = html.indexOf("alt=\"" + alt + "\"");
		assertThat(at).as("an image with alt " + alt + " in " + html).isNotNegative();
		int start = html.lastIndexOf("<img", at);
		return html.substring(start, html.indexOf('>', at) + 1);
	}

	/** Every {@code <img>} tag in the body. */
	private static List<String> imgs(String html) {
		return java.util.regex.Pattern.compile("(?i)<img\\b[^>]*>").matcher(html).results().map(m -> m.group()).toList();
	}

	private static byte[] riff(String chunk, byte[] payload) {
		ByteBuffer out = ByteBuffer.allocate(20 + payload.length).order(ByteOrder.LITTLE_ENDIAN);
		out.put("RIFF".getBytes(StandardCharsets.US_ASCII)).putInt(12 + payload.length);
		out.put("WEBP".getBytes(StandardCharsets.US_ASCII)).put(chunk.getBytes(StandardCharsets.US_ASCII));
		out.putInt(payload.length).put(payload);
		return out.array();
	}

	/** A lossy WebP header: a frame tag, the start code, then 14-bit width and height. */
	private static byte[] vp8(int width, int height) {
		ByteBuffer out = ByteBuffer.allocate(10).order(ByteOrder.LITTLE_ENDIAN);
		out.put(new byte[] { 0x10, 0x02, 0x00, (byte) 0x9d, 0x01, 0x2a });
		out.putShort((short) width).putShort((short) height);
		return riff("VP8 ", out.array());
	}

	/** A lossless WebP header: the 0x2f signature, then width - 1 and height - 1 in 14 bits each. */
	private static byte[] vp8l(int width, int height) {
		ByteBuffer out = ByteBuffer.allocate(5).order(ByteOrder.LITTLE_ENDIAN);
		out.put((byte) 0x2f).putInt((width - 1) | ((height - 1) << 14));
		return riff("VP8L", out.array());
	}

	/** An extended WebP header: flags, then the canvas width - 1 and height - 1 in 24 bits each. */
	private static byte[] vp8x(int width, int height) {
		byte[] out = new byte[10];
		for (int i = 0; i < 3; i++) {
			out[4 + i] = (byte) ((width - 1) >> (8 * i));
			out[7 + i] = (byte) ((height - 1) >> (8 * i));
		}
		return riff("VP8X", out);
	}

	/** A GIF whose logical screen is bigger than its one frame, as an optimized animation has. */
	private static byte[] gifScreen(int width, int height) {
		ByteBuffer out = ByteBuffer.allocate(13).order(ByteOrder.LITTLE_ENDIAN);
		out.put("GIF89a".getBytes(StandardCharsets.US_ASCII)).putShort((short) width).putShort((short) height);
		out.put(new byte[] { 0, 0, 0 });
		return out.array();
	}

	@Test
	@DisplayName("IMG-1 every <img> rendered from a post's Markdown gets loading=lazy")
	void lazy() throws IOException {
		post("pictured/index.md", "picture: cover.png\n",
				"![A](a.png)\n\n![B](/images/b.png)\n\n[![C](https://example.com/c.png)](/about)\n\n| Shot |\n| --- |\n| ![D](d.png) |");
		image("pictured/cover.png", "png", 8, 8);
		post("solo.md", "", "Intro.\n\n![E](/images/e.png)\n\nMore.\n\n![F](/images/f.png)");

		String pictured = body("pictured");
		assertThat(imgs(pictured)).hasSize(4).allSatisfy(tag -> assertThat(tag).contains("loading=\"lazy\""));
		assertThat(img(body("solo"), "F")).contains("loading=\"lazy\"");
	}

	@Test
	@DisplayName("IMG-2 every <img> rendered from a post's Markdown gets decoding=async")
	void async() throws IOException {
		post("solo.md", "", "![A](/images/a.png)\n\nText ![B](/images/b.png) inline.");
		post("pictured/index.md", "picture: cover.png\n", "![C](c.png)");
		image("pictured/cover.png", "png", 8, 8);

		assertThat(imgs(body("solo"))).hasSize(2).allSatisfy(tag -> assertThat(tag).contains("decoding=\"async\""));
		assertThat(img(body("pictured"), "C")).contains("decoding=\"async\"");
	}

	@Test
	@DisplayName("IMG-3 a post with no picture loads the first <img> in its body at once")
	void firstImageEager() throws IOException {
		post("plain.md", "", "Intro.\n\n![A](/images/a.png)\n\n![B](/images/b.png)");
		post("pictured.md", "picture: /images/cover.png\n", "![A](/images/a.png)\n\n![B](/images/b.png)");
		post("raw-first.md", "", "<img src=\"/images/raw.png\" alt=\"Raw\">\n\n![A](/images/a.png)");
		post("inline-raw-first.md", "", "Text <img src=\"/images/raw.png\" alt=\"Raw\"> and ![A](/images/a.png)");
		post("commented.md", "", "<!-- <img src=\"/images/old.png\" alt=\"Old\"> -->\n\n"
				+ "Text <!-- <img src=\"/images/old.png\"> --> here.\n\n![A](/images/a.png)\n\n![B](/images/b.png)");
		post("empty-comment.md", "", "<!--> <img src=\"/images/raw.png\" alt=\"Raw\">\n\n![A](/images/a.png)");
		post("dash-comment.md", "", "<!---> <img src=\"/images/raw.png\" alt=\"Raw\">\n\n![A](/images/a.png)");

		String plain = body("plain");
		assertThat(img(plain, "A")).doesNotContain("loading").contains("decoding=\"async\"");
		assertThat(img(plain, "B")).contains("loading=\"lazy\"");
		assertThat(img(body("pictured"), "A")).contains("loading=\"lazy\"");
		assertThat(img(body("raw-first"), "A")).contains("loading=\"lazy\"");
		assertThat(img(body("inline-raw-first"), "A")).contains("loading=\"lazy\"");
		String commented = body("commented");
		assertThat(img(commented, "A")).doesNotContain("loading");
		assertThat(img(commented, "B")).contains("loading=\"lazy\"");
		assertThat(img(body("empty-comment"), "A")).as("after <!-->").contains("loading=\"lazy\"");
		assertThat(img(body("dash-comment"), "A")).as("after <!--->").contains("loading=\"lazy\"");
	}

	@Test
	@DisplayName("IMG-4 an <img> written as raw HTML in a post is left as it is")
	void rawHtml() throws IOException {
		post("shots/index.md", "", "<img src=\"a.png\" alt=\"Block\">\n\nText <img src=\"a.png\" alt=\"Inline\"> text.\n\n"
				+ "<IMG SRC=\"/images/b.png\" ALT=\"Upper\">");
		image("shots/a.png", "png", 30, 20);

		String html = body("shots");
		for (String alt : List.of("Block", "Inline")) {
			assertThat(img(html, alt)).as(alt)
				.doesNotContain("loading", "decoding", "width", "height")
				.contains("src=\"/blog/shots/a.png\"");
		}
		assertThat(html).contains("<IMG SRC=\"/images/b.png\" ALT=\"Upper\">");
	}

	@Test
	@DisplayName("IMG-5 a PNG, JPEG or GIF in the post's folder gets the file's width and height")
	void pngJpegGif() throws IOException {
		post("shots/index.md", "", "![P](p.png)\n\n![J](sub/j.jpg)\n\n![G](./g.gif)\n\n![S](my%20pic.png)\n\n"
				+ "![Q](q.png?v=2)\n\n![W](sub/../wide.gif)\n\n![M](misnamed.jpg)\n\n![Sun](🌅%20shot.png)");
		image("shots/p.png", "png", 640, 480);
		image("shots/sub/j.jpg", "jpeg", 300, 200);
		image("shots/g.gif", "gif", 48, 32);
		image("shots/my pic.png", "png", 12, 7);
		image("shots/q.png", "png", 5, 9);
		write("shots/wide.gif", gifScreen(400, 100));
		image("shots/misnamed.jpg", "png", 21, 13);
		image("shots/🌅 shot.png", "png", 33, 11);

		String html = body("shots");
		assertThat(img(html, "P")).contains("width=\"640\"", "height=\"480\"");
		assertThat(img(html, "J")).contains("width=\"300\"", "height=\"200\"");
		assertThat(img(html, "G")).contains("width=\"48\"", "height=\"32\"");
		assertThat(img(html, "S")).contains("width=\"12\"", "height=\"7\"");
		assertThat(img(html, "Q")).contains("width=\"5\"", "height=\"9\"");
		assertThat(img(html, "W")).contains("width=\"400\"", "height=\"100\"");
		assertThat(img(html, "M")).contains("width=\"21\"", "height=\"13\"");
		assertThat(img(html, "Sun")).contains("width=\"33\"", "height=\"11\"");
	}

	@Test
	@DisplayName("IMG-6 a WebP in the post's folder gets the width and height from the file's header")
	void webp() throws IOException {
		post("shots/index.md", "", "![Lossy](lossy.webp)\n\n![Lossless](lossless.webp)\n\n![Extended](extended.webp)");
		write("shots/lossy.webp", vp8(640, 360));
		write("shots/lossless.webp", vp8l(300, 150));
		write("shots/extended.webp", vp8x(1920, 1080));

		String html = body("shots");
		assertThat(img(html, "Lossy")).contains("width=\"640\"", "height=\"360\"");
		assertThat(img(html, "Lossless")).contains("width=\"300\"", "height=\"150\"");
		assertThat(img(html, "Extended")).contains("width=\"1920\"", "height=\"1080\"");
	}

	@Test
	@DisplayName("IMG-7 an image whose size cannot be read gets no width or height")
	void unknownSize() throws IOException {
		post("shots/index.md", "", "![Gone](gone.png)\n\n![Broken](broken.png)\n\n![Cut](cut.png)\n\n![Svg](chart.svg)\n\n"
				+ "![Avif](photo.avif)\n\n![Static](/images/a.png)\n\n![Web](https://example.com/a.png)\n\n"
				+ "![Out](../outside.png)\n\n![Linked](linked.png)\n\n![Empty](empty.webp)\n\n![Dir](folder.png)");
		write("shots/broken.png", "not a picture".getBytes(StandardCharsets.UTF_8));
		image("whole.png", "png", 64, 64);
		write("shots/cut.png", java.util.Arrays.copyOf(Files.readAllBytes(posts.resolve("whole.png")), 12));
		write("shots/chart.svg", "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"10\" height=\"10\"/>".getBytes(StandardCharsets.UTF_8));
		write("shots/photo.avif", new byte[] { 0, 0, 0, 0x1c, 'f', 't', 'y', 'p', 'a', 'v', 'i', 'f' });
		image("outside.png", "png", 50, 50);
		Files.createSymbolicLink(posts.resolve("shots/linked.png"), posts.resolve("outside.png"));
		write("shots/empty.webp", new byte[0]);
		Files.createDirectories(posts.resolve("shots/folder.png"));
		post("solo.md", "", "![Solo](next.png)");
		image("next.png", "png", 40, 40);

		for (String alt : List.of("Gone", "Broken", "Cut", "Svg", "Avif", "Static", "Web", "Out", "Linked", "Empty", "Dir")) {
			assertThat(img(body("shots"), alt)).as(alt).doesNotContain("width", "height");
		}
		assertThat(img(body("solo"), "Solo")).doesNotContain("width", "height");
	}

	@Test
	@DisplayName("IMG-7 a source that cannot open files gives no width or height")
	void sourceWithoutOpen() throws IOException {
		image("shots/p.png", "png", 640, 480);
		DirectoryPostSource disk = new DirectoryPostSource(posts);
		PostSource listing = new PostSource() {
			@Override
			public List<MarkdownFile> files() {
				return List.of(new MarkdownFile("shots/index.md",
						"---\ntitle: A post\ndate: 2026-11-10\npicture: p.png\n---\n![P](p.png)\n"));
			}

			@Override
			public boolean contains(String folder, String path) {
				return disk.contains(folder, path);
			}
		};

		Post post = pages(listing).post("shots").orElseThrow().post();
		assertThat(img(post.body(), "P")).doesNotContain("width", "height");
		assertThat(post.pictureWidth()).isEmpty();
		assertThat(post.pictureHeight()).isEmpty();
	}

	@Test
	@DisplayName("IMG-8 a picture in the post's folder passes its size to the theme")
	void pictureSize() throws IOException {
		post("bundle/index.md", "picture: cover.png\n", "Body.");
		image("bundle/cover.png", "png", 640, 480);
		post("jekyll/index.md", "image: ./art/cover.webp\n", "Body.");
		write("jekyll/art/cover.webp", vp8x(1200, 630));
		post("broken/index.md", "picture: cover.png\n", "Body.");
		write("broken/cover.png", "not a picture".getBytes(StandardCharsets.UTF_8));
		post("static/index.md", "picture: /images/cover.png\n", "Body.");
		post("none/index.md", "", "Body.");

		assertThat(page("bundle").pictureWidth()).isEqualTo("640");
		assertThat(page("bundle").pictureHeight()).isEqualTo("480");
		assertThat(page("jekyll").pictureWidth()).isEqualTo("1200");
		assertThat(page("jekyll").pictureHeight()).isEqualTo("630");
		for (String slug : List.of("broken", "static", "none")) {
			assertThat(page(slug).pictureWidth()).as(slug).isEmpty();
			assertThat(page(slug).pictureHeight()).as(slug).isEmpty();
		}
		assertThat(page("broken").picture()).isEqualTo("/blog/broken/cover.png");
	}

}
