package dev.danvega.beansprout.code;

import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import dev.danvega.beansprout.content.MarkdownFile;
import dev.danvega.beansprout.content.Post;
import dev.danvega.beansprout.content.PostParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HighlighterTests {

	private final PrismHighlighter prism = new PrismHighlighter();

	private String html(String language, String code) {
		return prism.highlight(language, code).orElseThrow();
	}

	/** The text a reader sees: tags removed, entities decoded. */
	private static String text(String html) {
		return html.replaceAll("<[^>]+>", "").replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"")
			.replace("&#39;", "'").replace("&amp;", "&");
	}

	private Post post(String markdown) {
		return new PostParser(ZoneOffset.UTC).parse(new MarkdownFile("a.md", "---\ntitle: A\ndate: 2026-01-01\n---\n" + markdown));
	}

	@Test
	@DisplayName("HL-1 tokens are wrapped in spans with token and a Prism type")
	void prismSpans() {
		String java = html("java", "public class Bean { // sprout\n  String name = \"bean\"; int size = 42; }");

		assertThat(java).contains("<span class=\"token keyword\">public</span>", "<span class=\"token keyword\">class</span>",
				"<span class=\"token class-name\">Bean</span>", "<span class=\"token comment\">// sprout</span>",
				"<span class=\"token string\">&quot;bean&quot;</span>", "<span class=\"token number\">42</span>",
				"<span class=\"token punctuation\">{</span>");
	}

	@Test
	@DisplayName("HL-2 a fenced block keeps its language class on the code element")
	void languageClass() {
		assertThat(post("```java\nint x = 1;\n```\n").html()).contains("<pre><code class=\"language-java\">");
		assertThat(post("```cobol\nMOVE 1 TO X.\n```\n").html()).contains("<pre><code class=\"language-cobol\">");
	}

	@Test
	@DisplayName("HL-3 a fence with no language, or an unknown one, is shown escaped and unchanged")
	void unknownLanguages() {
		assertThat(post("```\n<b>x</b>\n```\n").html()).isEqualTo("<pre><code>&lt;b&gt;x&lt;/b&gt;\n</code></pre>\n");
		assertThat(post("```cobol\nMOVE 1 TO X.\n```\n").html())
			.isEqualTo("<pre><code class=\"language-cobol\">MOVE 1 TO X.\n</code></pre>\n");
		assertThat(prism.highlight("cobol", "x")).isEmpty();
	}

	@Test
	@DisplayName("HL-4 the text of every token is escaped")
	void escaping() {
		String html = html("html", "<script>alert(\"x\" & 'y')</script>");

		assertThat(html).doesNotContain("<script>", "</script>").contains("&lt;", "&gt;", "&amp;");
	}

	@Test
	@DisplayName("HL-5 removing the spans gives back the code exactly as written")
	void codeUnchanged() {
		List<String[]> samples = List.of(
				new String[] { "java", "@Service\nclass A {\n  /* block */ String s = \"a\\\"b\"; char c = '\\n';\n  var t = \"\"\"\n    text\n    \"\"\";\n}\n" },
				new String[] { "kotlin", "fun main() { val name = \"bean ${1 + 2}\" // hi\n println(name) }" },
				new String[] { "xml", "<?xml version=\"1.0\"?>\n<!-- c -->\n<dependency scope=\"test\"><artifactId>a</artifactId></dependency>" },
				new String[] { "html", "<!DOCTYPE html><a href=\"x?a=1&b=2\">link</a>" },
				new String[] { "yaml", "spring:\n  application:\n    name: blog # comment\n  list: [a, 'b']\n" },
				new String[] { "properties", "# c\nserver.port=8080\nspring.name : blog\n" },
				new String[] { "json", "{\"name\": \"bean\", \"size\": -1.5e3, \"ok\": true, \"none\": null}" },
				new String[] { "sql", "SELECT id, name FROM posts WHERE draft = false -- live\nORDER BY date DESC;" },
				new String[] { "bash", "#!/bin/bash\n./mvnw -q verify && echo \"$HOME ${USER}\" # done\n" },
				new String[] { "javascript", "const x = `t ${a}`; // c\nfunction f(a) { return a ?? null; }" },
				new String[] { "typescript", "interface Bean { name: string }\nconst b: Bean = { name: 'x' };" },
				new String[] { "java", "unclosed \"string and /* comment" });
		for (String[] sample : samples) {
			assertThat(text(html(sample[0], sample[1]))).as(sample[0]).isEqualTo(sample[1]);
		}
	}

	@Test
	@DisplayName("HL-6 code is highlighted when the post is read, so the post's HTML already has the spans")
	void highlightedOnRead() {
		assertThat(post("```java\npublic class A {}\n```\n").html())
			.contains("<pre><code class=\"language-java\"><span class=\"token keyword\">public</span>");
	}

	@Test
	@DisplayName("HL-7 the built-in highlighter knows the languages a Spring blog shows")
	void languages() {
		assertThat(html("java", "@Bean public void x() {}")).contains("token annotation", "token keyword", "token function");
		assertThat(html("kotlin", "fun main() { val x = 1 }")).contains("<span class=\"token keyword\">fun</span>",
				"<span class=\"token keyword\">val</span>");
		assertThat(html("xml", "<a href=\"x\">t</a>")).contains("token tag", "token attr-name", "token attr-value");
		assertThat(html("html", "<!-- c --><p>t</p>")).contains("token comment", "token tag");
		assertThat(html("yaml", "server:\n  port: 8080 # c")).contains("<span class=\"token key atrule\">port</span>",
				"token number", "token comment");
		assertThat(html("properties", "server.port=8080")).contains("<span class=\"token attr-name\">server.port</span>",
				"<span class=\"token attr-value\">8080</span>");
		assertThat(html("json", "{\"a\": \"b\", \"n\": 1}")).contains("<span class=\"token property\">&quot;a&quot;</span>",
				"<span class=\"token string\">&quot;b&quot;</span>");
		assertThat(html("sql", "select * from posts")).contains("<span class=\"token keyword\">select</span>",
				"<span class=\"token keyword\">from</span>");
		assertThat(html("bash", "echo $HOME # c")).contains("token function", "token variable", "token comment");
		assertThat(html("javascript", "const s = `x`;")).contains("<span class=\"token keyword\">const</span>",
				"token template-string");
		assertThat(html("typescript", "let n: number = 1;")).contains("<span class=\"token builtin\">number</span>");
	}

	@Test
	@DisplayName("HL-8 common short names and any letter case work")
	void aliases() {
		for (String name : List.of("yml", "sh", "bash", "shell", "zsh", "js", "ts", "kt", "svg", "Java", "JSON")) {
			assertThat(prism.highlight(name, "x")).as(name).isPresent();
		}
	}

	@Test
	@DisplayName("HL-10 the NONE highlighter leaves every fence as it is")
	void none() {
		assertThat(CodeHighlighter.NONE.highlight("java", "class A {}")).isEqualTo(Optional.empty());
		Post post = new PostParser(ZoneOffset.UTC, CodeHighlighter.NONE)
			.parse(new MarkdownFile("a.md", "---\ntitle: A\ndate: 2026-01-01\n---\n```java\nclass A {}\n```\n"));
		assertThat(post.html()).isEqualTo("<pre><code class=\"language-java\">class A {}\n</code></pre>\n");
	}


	@Test
	@DisplayName("HL-18 a very long string literal is highlighted without failing, and its text stays the same")
	void longLiterals() {
		for (String[] sample : List.of(new String[] { "java", "String s = \"" + "a".repeat(50_000) + "\";" },
				new String[] { "json", "{\"k\":\"" + "a".repeat(50_000) + "\"}" },
				new String[] { "javascript", "const s = `" + "a".repeat(50_000) + "`;" },
				new String[] { "yaml", "k: \"" + "a".repeat(50_000) + "\"" },
				new String[] { "sql", "select '" + "a".repeat(50_000) + "'" },
				new String[] { "bash", "echo \"" + "a".repeat(50_000) + "\"" },
				new String[] { "java", "String s = \"" + "\\n".repeat(30_000) + "\";" },
				new String[] { "xml", "<a" + " b=c".repeat(20_000) + ">" })) {
			assertThat(text(html(sample[0], sample[1]))).as(sample[0]).isEqualTo(sample[1]);
		}
	}

	@Test
	@DisplayName("HL-18 a fence the highlighter fails on, or one over 100,000 characters, is plain code and the post still loads")
	void failuresFallBack() {
		CodeHighlighter broken = (language, code) -> {
			throw new StackOverflowError("simulated");
		};
		Post post = new PostParser(ZoneOffset.UTC, broken)
			.parse(new MarkdownFile("a.md", "---\ntitle: A\ndate: 2026-01-01\n---\nBefore.\n\n```java\nclass A {}\n```\n"));
		assertThat(post.html()).contains("<p>Before.</p>", "<pre><code class=\"language-java\">class A {}\n</code></pre>");

		String huge = "int x = 1;\n".repeat(10_000);
		assertThat(post("```java\n" + huge + "```\n").html()).doesNotContain("token");
	}

	@Test
	@DisplayName("HL-4 an ampersand inside a token is escaped")
	void ampersandInToken() {
		assertThat(html("html", "<a href=\"x?a=1&b=2\">t</a>"))
			.contains("<span class=\"token attr-value\">&quot;x?a=1&amp;b=2&quot;</span>");
		assertThat(html("java", "String s = \"a&b\";")).contains("<span class=\"token string\">&quot;a&amp;b&quot;</span>");
	}

	@Test
	@DisplayName("HL-18 a YAML line with a long run of spaces is highlighted quickly")
	void yamlSpaces() {
		long start = System.nanoTime();
		html("yaml", "a" + " ".repeat(80_000) + "x");
		assertThat((System.nanoTime() - start) / 1_000_000).isLessThan(1_000);
	}


	@Test
	@DisplayName("HL-18 long string literals still get their string token, not just the plain fallback")
	void longLiteralsAreHighlighted() {
		assertThat(html("java", "String s = \"" + "a".repeat(50_000) + "\";")).contains("<span class=\"token string\">&quot;aaa");
		assertThat(html("json", "{\"k\":\"" + "a".repeat(50_000) + "\"}")).contains("<span class=\"token string\">&quot;aaa");
		assertThat(html("javascript", "x = `" + "a".repeat(50_000) + "`")).contains("token template-string");
	}

	@Test
	@DisplayName("HL-18 a highlighter that throws, or returns null, leaves that fence plain and the post loads")
	void misbehavingHighlighters() {
		String markdown = "---\ntitle: A\ndate: 2026-01-01\n---\nBefore.\n\n```java\nclass A {}\n```\n";
		CodeHighlighter throwing = (language, code) -> {
			throw new IllegalStateException("broken");
		};
		@SuppressWarnings("NullAway")
		CodeHighlighter nulling = (language, code) -> null;
		CodeHighlighter linkage = (language, code) -> {
			throw new NoClassDefFoundError("missing");
		};
		CodeHighlighter asserting = (language, code) -> {
			throw new AssertionError("broken");
		};
		for (CodeHighlighter highlighter : List.of(throwing, nulling, linkage, asserting)) {
			assertThat(new PostParser(ZoneOffset.UTC, highlighter).parse(new MarkdownFile("a.md", markdown)).html())
				.contains("<p>Before.</p>", "<pre><code class=\"language-java\">class A {}\n</code></pre>");
		}
	}

	@Test
	@DisplayName("HL-18 code of exactly 100,000 characters is highlighted, and one more character makes it plain")
	void sizeBoundary() {
		String body = "int x = 1;\n".repeat(9_090);
		String exact = body + "x".repeat(100_000 - body.length());
		assertThat(exact).hasSize(100_000);

		assertThat(post("```java\n" + exact + "\n```\n").html()).contains("token keyword");
		assertThat(post("```java\nx" + exact + "\n```\n").html()).doesNotContain("token");
	}

	@Test
	@DisplayName("HL-19 a fence that would take seconds stops within the time budget and stays plain")
	void timeBudget() {
		for (String[] sample : List.of(new String[] { "xml", "<a ".repeat(33_000) },
				new String[] { "javascript", "'\\\"".repeat(30_000) },
				new String[] { "java", "\"" + "\\\"".repeat(45_000) })) {
			long start = System.nanoTime();
			String html = html(sample[0], sample[1]);
			long millis = (System.nanoTime() - start) / 1_000_000;
			assertThat(millis).as(sample[0]).isLessThan(1_500);
			assertThat(html).as(sample[0]).doesNotContain("<span");
			assertThat(text(html)).as(sample[0]).isEqualTo(sample[1]);
		}
	}


	@Test
	@DisplayName("HL-20 a post full of slow fences stops highlighting after one second and shows the rest plain")
	void postBudget() {
		String slow = "```xml\n" + "<a ".repeat(33_000) + "\n```\n\n";
		long start = System.nanoTime();
		Post post = post("```java\npublic class First {}\n```\n\n" + slow.repeat(12) + "```java\nclass A {}\n```\n");
		long millis = (System.nanoTime() - start) / 1_000_000;

		assertThat(millis).isLessThan(2_500);
		assertThat(post.html()).contains("<code class=\"language-java\"><span class=\"token keyword\">public</span>")
			.contains("<code class=\"language-java\">class A {}");
	}


	@Test
	@DisplayName("HL-18 a very long properties key is still highlighted")
	void longPropertyKey() {
		assertThat(html("properties", "a".repeat(50_000) + "=1"))
			.startsWith("<span class=\"token attr-name\">aaa").contains("<span class=\"token attr-value\">1</span>");
	}

}
