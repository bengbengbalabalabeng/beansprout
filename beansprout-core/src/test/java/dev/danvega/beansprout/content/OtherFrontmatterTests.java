package dev.danvega.beansprout.content;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Frontmatter that Jekyll and Hugo posts already use. */
class OtherFrontmatterTests {

	private static final ZoneId ZONE = ZoneId.of("America/New_York");

	private final PostParser parser = new PostParser(ZONE);

	private final Clock clock = Clock.fixed(Instant.parse("2026-11-16T12:00:00Z"), ZoneOffset.UTC);

	private final List<MarkdownFile> files = new ArrayList<>();

	private Post parse(String path, String text) {
		return parser.parse(new MarkdownFile(path, text));
	}

	private Post yaml(String frontmatter) {
		return parse("a.md", "---\ntitle: Hi\ndate: 2026-03-14\n" + frontmatter + "\n---\nThe first paragraph.\n");
	}

	private Post toml(String frontmatter) {
		return parse("a.md", "+++\n" + frontmatter + "\n+++\nThe first paragraph.\n");
	}

	private Posts posts() {
		return new Posts(() -> List.copyOf(files), parser, clock, false);
	}

	private void add(String path, String text) {
		files.add(new MarkdownFile(path, text));
	}

	// Other names for keys Beansprout has

	@Test
	@DisplayName("FM-1 description is the summary when there is no summary")
	void descriptionIsTheSummary() {
		Post post = yaml("description: From the description");

		assertThat(post.lede()).isEqualTo("From the description");
		assertThat(post.summary()).isEqualTo("From the description");
	}

	@Test
	@DisplayName("FM-1 a description folded over indented lines is the summary, its lines joined")
	void foldedDescription() {
		Post post = yaml("description: >\n  Folded text\n\n  over two lines\ntags: [java]");

		assertThat(post.lede()).isEqualTo("Folded text over two lines");
		assertThat(post.tags()).containsExactly("java");
		assertThat(yaml("description: |-\n  Kept as lines").lede()).isEqualTo("Kept as lines");
		assertThat(yaml("description: This is long\n  and continues: here").lede()).isEqualTo("This is long and continues: here");
		assertThat(yaml("description:\n  Starts on the next line").lede()).isEqualTo("Starts on the next line");
		assertThat(yaml("description: \"This is a long\n  description\"").lede()).isEqualTo("This is a long description");
		assertThat(yaml("description: 'It''s long\n  and quoted'").lede()).isEqualTo("It's long and quoted");
		assertThat(yaml("description: >").lede()).isEmpty();
	}

	@Test
	@DisplayName("FM-1 a known key indented under description is still a mistake, so a draft is never published by it")
	void knownKeyUnderDescription() {
		add("hidden.md", "---\ntitle: A\ndate: 2026-03-14\ndescription: Hi\n  draft: true\n---\nBody.\n");
		Posts posts = posts();

		assertThat(posts.published()).isEmpty();
		assertThat(posts.problems()).singleElement().asString().contains("hidden.md", "line 5", "draft: true");
	}

	@Test
	@DisplayName("FM-2 summary wins over description")
	void summaryWins() {
		Post post = yaml("summary: From the summary\ndescription: From the description");

		assertThat(post.lede()).isEqualTo("From the summary");
	}

	@Test
	@DisplayName("FM-3 a single image is the picture when there is no picture")
	void imageIsThePicture() {
		assertThat(yaml("image: /images/a.png").image()).isEqualTo("/images/a.png");
		assertThat(yaml("image: \"https://cdn.example.com/a.png\"").image()).isEqualTo("https://cdn.example.com/a.png");
	}

	@Test
	@DisplayName("FM-3 a list under image is not a single value, so it is not the picture")
	void imageListIsNotThePicture() {
		assertThat(yaml("image:\n- /images/a.png").image()).isEmpty();
	}

	@Test
	@DisplayName("FM-3 an image that is not a path or an address skips the post, like a bad picture")
	void badImageSkips() {
		add("bad.md", "---\ntitle: Hi\ndate: 2026-03-14\nimage: javascript:alert(1)\n---\n");

		assertThat(posts().published()).isEmpty();
		assertThat(posts().problems()).singleElement().asString().contains("bad.md", "image");
	}

	@Test
	@DisplayName("FM-4 an image with indented lines under it is ignored and the post is kept")
	void nestedImageIsIgnored() {
		Post post = yaml("image:\n  path: /images/a.png\n  alt: A picture\n  - stray\ntags: [java]");

		assertThat(post.image()).isEmpty();
		assertThat(post.tags()).containsExactly("java");
	}

	@Test
	@DisplayName("FM-4 an image with indented list items under it is ignored and the post is kept")
	void indentedImageListIsIgnored() {
		assertThat(yaml("image:\n  - /images/a.png").image()).isEmpty();
	}

	@Test
	@DisplayName("FM-5 picture wins over image")
	void pictureWins() {
		assertThat(yaml("picture: /images/picture.png\nimage: /images/image.png").image()).isEqualTo("/images/picture.png");
	}

	@Test
	@DisplayName("FM-6 lastmod is the modified date when there is no updated")
	void lastmodIsModified() {
		assertThat(yaml("lastmod: 2026-04-01").modified().toLocalDate()).hasToString("2026-04-01");
	}

	@Test
	@DisplayName("FM-6 updated wins over lastmod")
	void updatedWins() {
		assertThat(yaml("updated: 2026-05-01\nlastmod: 2026-04-01").modified().toLocalDate()).hasToString("2026-05-01");
	}

	@Test
	@DisplayName("FM-7 a lastmod that is not a date skips the post with a warning that names it")
	void badLastmodSkips() {
		add("edited.md", "---\ntitle: Hi\ndate: 2026-03-14\nlastmod: last tuesday\n---\n");
		add("fine.md", "---\ntitle: Fine\ndate: 2026-03-14\n---\n");
		Posts posts = posts();

		assertThat(posts.published()).extracting(Post::slug).containsExactly("fine");
		assertThat(posts.problems()).singleElement().asString().contains("edited.md", "lastmod", "last tuesday");
	}

	@Test
	@DisplayName("FM-7 a lastmod that is not a date skips the post even when updated is there")
	void badLastmodWithUpdatedSkips() {
		add("both.md", "---\ntitle: Hi\ndate: 2026-03-14\nupdated: 2026-05-01\nlastmod: last tuesday\n---\n");
		Posts posts = posts();

		assertThat(posts.published()).isEmpty();
		assertThat(posts.problems()).singleElement().asString().contains("both.md", "lastmod", "last tuesday");
	}

	@Test
	@DisplayName("FM-7 a TOML lastmod that is not a date skips the post with a warning that names it")
	void badTomlLastmodSkips() {
		add("word.md", "+++\ntitle = \"A\"\ndate = 2026-03-14\nlastmod = yesterday\n+++\n");
		add("number.md", "+++\ntitle = \"B\"\ndate = 2026-03-14\nlastmod = 1700000000\n+++\n");
		add("quoted.md", "+++\ntitle = \"C\"\ndate = 2026-03-14\nlastmod = \"yesterday\"\n+++\n");
		Posts posts = posts();

		assertThat(posts.published()).isEmpty();
		assertThat(posts.problems()).hasSize(3);
		assertThat(posts.problems()).anySatisfy(problem -> assertThat(problem).contains("word.md", "lastmod", "yesterday"));
		assertThat(posts.problems()).anySatisfy(problem -> assertThat(problem).contains("number.md", "lastmod", "1700000000"));
	}

	@Test
	@DisplayName("FM-8 canonical_url is the canonical when there is no canonical")
	void canonicalUrlIsTheCanonical() {
		assertThat(yaml("canonical_url: https://dev.to/jane/a").canonical()).isEqualTo("https://dev.to/jane/a");
		assertThat(yaml("canonical: https://one.example/a\ncanonical_url: https://two.example/a").canonical())
			.isEqualTo("https://one.example/a");
	}

	@Test
	@DisplayName("FM-8 a canonical_url that is not an absolute address skips the post, like a bad canonical")
	void badCanonicalUrlSkips() {
		add("moved.md", "---\ntitle: Hi\ndate: 2026-03-14\ncanonical_url: /elsewhere\n---\n");

		assertThat(posts().problems()).singleElement().asString().contains("moved.md", "canonical_url", "/elsewhere");
	}

	// Jekyll file names

	@Test
	@DisplayName("FM-9 a dated file name gives the date when there is no date")
	void dateFromFileName() {
		Post post = parse("2024/2026-03-14-hello.md", "---\ntitle: Hello\n---\nBody.\n");

		assertThat(post.date()).isEqualTo(java.time.LocalDate.parse("2026-03-14").atStartOfDay(ZONE));
	}

	@Test
	@DisplayName("FM-9 a dated bundle folder gives the date when there is no date")
	void dateFromFolderName() {
		Post post = parse("2026-03-14-trip/index.md", "---\ntitle: Trip\n---\nBody.\n");

		assertThat(post.date().toLocalDate()).hasToString("2026-03-14");
	}

	@Test
	@DisplayName("FM-9 a date in the frontmatter wins over the file name")
	void frontmatterDateWins() {
		Post post = parse("2026-03-14-hello.md", "---\ntitle: Hello\ndate: 2026-03-20\n---\n");

		assertThat(post.date().toLocalDate()).hasToString("2026-03-20");
	}

	@Test
	@DisplayName("FM-10 a dated file name leaves the date out of the slug")
	void slugWithoutDate() {
		assertThat(parse("2026-03-14-hello.md", "---\ntitle: Hello\n---\n").slug()).isEqualTo("hello");
		assertThat(parse("2026-03-14-hello.md", "---\ntitle: Hello\ndate: 2026-03-20\n---\n").slug()).isEqualTo("hello");
		assertThat(parse("2026-03-14-trip/index.md", "---\ntitle: Trip\n---\n").slug()).isEqualTo("trip");
	}

	@Test
	@DisplayName("FM-10 a slug line wins over a dated file name")
	void slugLineWins() {
		assertThat(parse("2026-03-14-hello.md", "---\ntitle: Hello\nslug: 2026-03-14-hello\n---\n").slug())
			.isEqualTo("2026-03-14-hello");
	}

	@Test
	@DisplayName("FM-10 a name that is only a date keeps it as the slug")
	void nameThatIsOnlyADate() {
		assertThat(parse("2026-03-14.md", "---\ntitle: Day\ndate: 2026-03-14\n---\n").slug()).isEqualTo("2026-03-14");
	}

	@Test
	@DisplayName("FM-11 a dated name with a day that does not exist skips the post with a warning that names it")
	void dayThatDoesNotExist() {
		add("2026-02-30-hello.md", "---\ntitle: Hello\n---\n");
		add("2026-13-01-other.md", "---\ntitle: Other\n---\n");
		Posts posts = posts();

		assertThat(posts.published()).isEmpty();
		assertThat(posts.problems()).hasSize(2);
		assertThat(posts.problems().get(0)).contains("2026-02-30-hello.md", "starts with 2026-02-30", "not a day");
		assertThat(posts.problems().get(1)).contains("2026-13-01-other.md", "starts with 2026-13-01", "not a day");
	}

	// Hugo's TOML frontmatter

	@Test
	@DisplayName("FM-12 a file that starts with +++ has TOML frontmatter up to the next +++ line")
	void tomlBlock() {
		Post post = parse("a.md", "+++\ntitle = \"Hugo\"\ndate = 2026-03-14\n+++\nThe first paragraph.\n\n+++ is text here.\n");

		assertThat(post.title()).isEqualTo("Hugo");
		assertThat(post.html()).contains("The first paragraph.", "+++ is text here.");
	}

	@Test
	@DisplayName("FM-12 a TOML block with no closing +++ line skips the post with a warning")
	void tomlBlockNotClosed() {
		add("open.md", "+++\ntitle = \"Hugo\"\ndate = 2026-03-14\n");

		assertThat(posts().problems()).singleElement().asString().contains("open.md", "+++");
	}

	@Test
	@DisplayName("FM-13 a TOML string in double or single quotes is the key's value")
	void tomlStrings() {
		Post post = toml("title = \"Say \\\"hi\\\" \\u00e9\" # a comment\ndate = 2026-03-14\ndescription = 'C:\\path # not a comment'");

		assertThat(post.title()).isEqualTo("Say \"hi\" é");
		assertThat(post.lede()).isEqualTo("C:\\path # not a comment");
	}

	@Test
	@DisplayName("FM-14 a TOML date, or date and time, is the key's value")
	void tomlDates() {
		assertThat(toml("title = \"A\"\ndate = 2026-03-14").date()).isEqualTo(java.time.LocalDate.parse("2026-03-14").atStartOfDay(ZONE));
		assertThat(toml("title = \"A\"\ndate = 2026-03-14T09:30:00Z").date().toInstant()).hasToString("2026-03-14T09:30:00Z");
		assertThat(toml("title = \"A\"\ndate = 2026-03-14 09:30:00-05:00").date().toInstant()).hasToString("2026-03-14T14:30:00Z");
		assertThat(toml("title = \"A\"\ndate = 2026-03-14T09:30:00").date().toLocalDateTime()).hasToString("2026-03-14T09:30");
		assertThat(toml("title = \"A\"\ndate = 2026-03-14\nlastmod = 2026-04-01T08:00:00Z").modified().toInstant())
			.hasToString("2026-04-01T08:00:00Z");
	}

	@Test
	@DisplayName("FM-15 true and false are the values of draft and unlisted")
	void tomlBooleans() {
		assertThat(toml("title = \"A\"\ndate = 2026-03-14\ndraft = true").draft()).isTrue();
		assertThat(toml("title = \"A\"\ndate = 2026-03-14\ndraft = false").draft()).isFalse();
		assertThat(Frontmatter.parse("+++\nunlisted = true\n+++\n").raw("unlisted")).isEqualTo("true");
		assertThat(Frontmatter.parse("+++\nunlisted = false\n+++\n").raw("unlisted")).isEqualTo("false");
	}

	@Test
	@DisplayName("FM-15 a TOML draft that is not true or false skips the post, as in YAML")
	void tomlDraftNotABoolean() {
		add("yes.md", "+++\ntitle = \"A\"\ndate = 2026-03-14\ndraft = yes\n+++\n");
		add("one.md", "+++\ntitle = \"B\"\ndate = 2026-03-14\ndraft = 1\n+++\n");
		add("list.md", "+++\ntitle = \"C\"\ndate = 2026-03-14\ndraft = [true]\n+++\n");
		Posts posts = posts();

		assertThat(posts.published()).isEmpty();
		assertThat(posts.problems()).hasSize(3).allSatisfy(problem -> assertThat(problem).contains("draft", "not true or false"));
	}

	@Test
	@DisplayName("FM-16 a TOML array of strings is a list")
	void tomlArrays() {
		assertThat(toml("title = \"A\"\ndate = 2026-03-14\ntags = [\"java\", 'spring boot', \"a, b\",]").tags())
			.containsExactly("java", "spring boot", "a, b");
		assertThat(toml("title = \"A\"\ndate = 2026-03-14\ntags = [\n  \"java\", # first\n  \"spring\",\n]").tags())
			.containsExactly("java", "spring");
		assertThat(toml("title = \"A\"\ndate = 2026-03-14\ntags = []").tags()).isEmpty();
	}

	@Test
	@DisplayName("FM-17 every line from a table header to the end of the frontmatter is ignored")
	void tomlTables() {
		Post post = toml("title = \"Top\"\ndate = 2026-03-14\n[params]\ntitle = \"Params\"\ndraft = true\nnot toml at all\n"
				+ "[[resources]]\nsrc = \"a.png\"");

		assertThat(post.title()).isEqualTo("Top");
		assertThat(post.draft()).isFalse();
	}

	@Test
	@DisplayName("FM-18 a TOML post with no title skips with a warning that names it")
	void tomlNoTitle() {
		add("untitled.md", "+++\ndate = 2026-03-14\n+++\n");

		assertThat(posts().problems()).singleElement().asString().contains("untitled.md", "title = ");
	}

	@Test
	@DisplayName("FM-18 a TOML post with no date it can find skips with a warning that names it")
	void tomlNoDate() {
		add("undated.md", "+++\ntitle = \"A\"\n+++\n");
		add("2026-02-30-bad.md", "+++\ntitle = \"B\"\n+++\n");
		Posts posts = posts();

		assertThat(posts.published()).isEmpty();
		assertThat(posts.problems()).anySatisfy(problem -> assertThat(problem).contains("undated.md", "date = "));
		assertThat(posts.problems()).anySatisfy(problem -> assertThat(problem).contains("2026-02-30-bad.md", "date = "));
	}

	@Test
	@DisplayName("FM-18 a TOML title that is not a string is not a title")
	void tomlTitleNotAString() {
		add("number.md", "+++\ntitle = 42\ndate = 2026-03-14\n+++\n");

		assertThat(posts().problems()).singleElement().asString().contains("number.md", "no title", "title = ");
	}

	@Test
	@DisplayName("FM-19 a TOML line whose key is not read is ignored, whatever its value")
	void tomlUnknownKeys() {
		Post post = toml("title = \"A\"\ndate = 2026-03-14\nweight = 10\ntoc = true\nmenu = { main = { weight = 1 } }\n"
				+ "series = [\"one\", [\"nested\"]]\nkeywords = [\n  \"a\",\n  \"b\"\n]\nnotes = \"\"\"\nline one\n= not a key\n\"\"\"\n"
				+ "raw = '''\n[not a table]\n'''\nparams.color = \"red\"\n\"quoted key\" = 1\ntags = [\"java\"]");

		assertThat(post.title()).isEqualTo("A");
		assertThat(post.tags()).containsExactly("java");
	}

	@Test
	@DisplayName("FM-19 a known key with a value shape that is not read is ignored")
	void tomlKnownKeyOtherShape() {
		Post post = toml("title = \"A\"\ndate = 2026-03-14\ndescription = \"\"\"\nLong\n\"\"\"\nsummary = 3\ntags = { a = 1 }");

		assertThat(post.lede()).isEmpty();
		assertThat(post.tags()).isEmpty();
	}

	@Test
	@DisplayName("FM-20 a TOML line that is not key = value, a comment or a table header skips the post with a warning that names the line")
	void tomlBadLine() {
		add("broken.md", "+++\ntitle = \"A\"\ndate = 2026-03-14\n# a comment\n\nthis is not toml\n+++\n");
		add("fine.md", "+++\ntitle = \"Fine\"\ndate = 2026-03-14\n+++\n");
		Posts posts = posts();

		assertThat(posts.published()).extracting(Post::slug).containsExactly("fine");
		assertThat(posts.problems()).singleElement().asString().contains("broken.md", "line 6", "this is not toml");
	}

	@Test
	@DisplayName("FM-20 a TOML escape that is not a character skips only that post with a warning")
	void tomlBadEscapes() {
		add("big.md", "+++\ntitle = \"T \\UFFFFFFFF\"\ndate = 2026-03-14\n+++\n");
		add("high.md", "+++\ntitle = \"T \\U80000000\"\ndate = 2026-03-14\n+++\n");
		add("surrogate.md", "+++\ntitle = \"T \\uD83D\"\ndate = 2026-03-14\n+++\n");
		add("fullwidth.md", "+++\ntitle = \"A\\u００４１\"\ndate = 2026-03-14\n+++\n");
		add("ok.md", "+++\ntitle = \"Emoji \\U0001F600 tab\\t slash\\\\\"\ndate = 2026-03-14\n+++\n");
		Posts posts = posts();

		assertThat(posts.published()).extracting(Post::title).containsExactly("Emoji \uD83D\uDE00 tab\t slash\\");
		assertThat(posts.problems()).hasSize(4);
	}

	@Test
	@DisplayName("FM-20 a TOML array or string that never closes skips the post with a warning")
	void tomlNeverCloses() {
		add("array.md", "+++\ntitle = \"A\"\ndate = 2026-03-14\ntags = [\"java\",\n+++\n");
		add("string.md", "+++\ntitle = \"A\"\ndate = 2026-03-14\nnotes = \"\"\"\nnever closed\n+++\n");
		Posts posts = posts();

		assertThat(posts.published()).isEmpty();
		assertThat(posts.problems()).hasSize(2).allSatisfy(problem -> assertThat(problem).contains("line 4"));
	}

}
