package dev.danvega.beansprout.content;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import dev.danvega.beansprout.code.CodeHighlighter;
import org.commonmark.Extension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.AbstractVisitor;
import org.commonmark.node.Code;
import org.commonmark.node.HardLineBreak;
import org.commonmark.node.Node;
import org.commonmark.node.Paragraph;
import org.commonmark.node.SoftLineBreak;
import org.commonmark.node.Text;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.commonmark.renderer.text.TextContentRenderer;
import org.jspecify.annotations.Nullable;

/** CommonMark with GitHub tables. Gives the HTML, the {@code ##} headings, and plain text for summaries. */
final class MarkdownRenderer {

	private static final List<Extension> EXTENSIONS = List.of(TablesExtension.create());

	private final Parser parser = Parser.builder().extensions(EXTENSIONS).build();

	private final CodeHighlighter highlighter;

	MarkdownRenderer(CodeHighlighter highlighter) {
		this.highlighter = highlighter;
	}

	private final TextContentRenderer text = TextContentRenderer.builder().extensions(EXTENSIONS).build();

	record Rendered(String html, List<Heading> headings, String firstParagraph, int words) {
	}

	Rendered render(String markdown) {
		return render(markdown, null);
	}

	/**
	 * Renders a post's body.
	 *
	 * @param images for a post that is a folder, the address its images are served under, ending in
	 *     {@code /}. An image linked by a relative path, like {@code shots/a.png}, gets it in front.
	 *     Null for a post that is a single file, whose links stay as written.
	 */
	Rendered render(String markdown, @Nullable String images) {
		Node document = parser.parse(markdown);

		IdentityHashMap<Node, String> ids = new IdentityHashMap<>();
		List<Heading> headings = new ArrayList<>();
		Map<String, Integer> used = new HashMap<>();
		Set<String> taken = new HashSet<>();
		document.accept(new AbstractVisitor() {
			@Override
			public void visit(org.commonmark.node.Heading heading) {
				if (heading.getLevel() == 2) {
					String label = plain(heading);
					String id = unique(Slugs.slugify(label).isEmpty() ? "section" : Slugs.slugify(label), used, taken);
					ids.put(heading, id);
					headings.add(new Heading(id, label));
				}
				visitChildren(heading);
			}
		});

		HtmlRenderer html = HtmlRenderer.builder()
			.extensions(EXTENSIONS)
			.nodeRendererFactory(context -> new FencedCodeRenderer(context, highlighter))
			.attributeProviderFactory(context -> (node, tagName, attributes) -> {
				String id = ids.get(node);
				if (id != null) {
					attributes.put("id", id);
				}

			})
			.build();

		String body = html.render(document);
		return new Rendered(images == null ? body : inFolder(body, images), headings, firstParagraph(document),
				countWords(text.render(document)));
	}

	/**
	 * Puts a bundle's address in front of every image that points at a file in its folder, in
	 * Markdown images and in raw HTML alike. Each {@code <img>} tag's attributes are read one by one,
	 * so a quoted value may hold {@code >} or {@code src=}. Code is escaped by then, so an
	 * {@code <img>} shown in code is not touched.
	 */
	private static String inFolder(String html, String images) {
		StringBuilder out = new StringBuilder(html.length());
		int done = 0;
		int at = indexOfImg(html, 0);
		while (at >= 0) {
			int p = at + 4;
			while (p < html.length() && html.charAt(p) != '>') {
				char c = html.charAt(p);
				if (Character.isWhitespace(c) || c == '/') {
					p++;
					continue;
				}
				int nameStart = p;
				while (p < html.length() && !Character.isWhitespace(html.charAt(p)) && "=>/".indexOf(html.charAt(p)) < 0) {
					p++;
				}
				String name = html.substring(nameStart, p);
				int q = skipSpace(html, p);
				if (q >= html.length() || html.charAt(q) != '=') {
					p = Math.max(p, nameStart + 1);
					continue;
				}
				int valueStart = skipSpace(html, q + 1);
				int valueEnd;
				int next;
				if (valueStart < html.length() && (html.charAt(valueStart) == '"' || html.charAt(valueStart) == '\'')) {
					int close = html.indexOf(html.charAt(valueStart), valueStart + 1);
					valueEnd = close < 0 ? html.length() : close;
					next = close < 0 ? html.length() : close + 1;
					valueStart++;
				}
				else {
					valueEnd = valueStart;
					while (valueEnd < html.length() && !Character.isWhitespace(html.charAt(valueEnd)) && html.charAt(valueEnd) != '>') {
						valueEnd++;
					}
					next = valueEnd;
				}
				String value = unescape(html.substring(valueStart, valueEnd));
				if (name.equalsIgnoreCase("src") && value != null && Bundles.inFolder(value)) {
					out.append(html, done, nameStart)
						.append("src=\"")
						.append(images)
						.append(Bundles.tidy(value).replace("&", "&amp;").replace("\"", "&quot;"))
						.append('"');
					done = next;
				}
				p = next;
			}
			at = indexOfImg(html, p);
		}
		return out.append(html, done, html.length()).toString();
	}

	/**
	 * An attribute value with its character references decoded, the way a browser reads it, or
	 * null when it has an {@code &} this does not read for sure. Such a value is left as written.
	 */
	private static @Nullable String unescape(String value) {
		if (value.indexOf('&') < 0) {
			return value;
		}
		StringBuilder out = new StringBuilder();
		int i = 0;
		while (i < value.length()) {
			char c = value.charAt(i);
			if (c != '&') {
				out.append(c);
				i++;
				continue;
			}
			int semi = value.indexOf(';', i);
			if (semi < 0) {
				return null;
			}
			String character = character(value.substring(i + 1, semi));
			if (character == null) {
				return null;
			}
			out.append(character);
			i = semi + 1;
		}
		return out.toString();
	}

	/** One character reference without its {@code &} and {@code ;}, or null when it is not one this reads. */
	private static @Nullable String character(String reference) {
		try {
			if (reference.startsWith("#x") || reference.startsWith("#X")) {
				return Character.toString(Integer.parseInt(reference.substring(2), 16));
			}
			if (reference.startsWith("#")) {
				return Character.toString(Integer.parseInt(reference.substring(1)));
			}
		}
		catch (IllegalArgumentException ex) {
			return null;
		}
		return switch (reference) {
			case "amp" -> "&";
			case "quot" -> "\"";
			case "apos" -> "'";
			case "lt" -> "<";
			case "gt" -> ">";
			case "period" -> ".";
			case "sol" -> "/";
			case "percnt" -> "%";
			default -> null;
		};
	}

	/** Where the next {@code <img} tag starts, in any case, or -1. */
	private static int indexOfImg(String html, int from) {
		for (int i = html.indexOf('<', from); i >= 0 && i + 4 <= html.length(); i = html.indexOf('<', i + 1)) {
			if (html.regionMatches(true, i + 1, "img", 0, 3)
					&& (i + 4 == html.length() || Character.isWhitespace(html.charAt(i + 4)) || "/>".indexOf(html.charAt(i + 4)) >= 0)) {
				return i;
			}
		}
		return -1;
	}

	private static int skipSpace(String html, int from) {
		int i = from;
		while (i < html.length() && Character.isWhitespace(html.charAt(i))) {
			i++;
		}
		return i;
	}

	private String firstParagraph(Node document) {
		for (Node node = document.getFirstChild(); node != null; node = node.getNext()) {
			if (node instanceof Paragraph) {
				return plain(node).replaceAll("\\s+", " ").strip();
			}
		}
		return "";
	}

	/** The text a reader sees: literal text and inline code, with line breaks as spaces. */
	private static String plain(Node node) {
		StringBuilder out = new StringBuilder();
		node.accept(new AbstractVisitor() {
			@Override
			public void visit(Text text) {
				out.append(text.getLiteral());
			}

			@Override
			public void visit(Code code) {
				out.append(code.getLiteral());
			}

			@Override
			public void visit(SoftLineBreak softLineBreak) {
				out.append(' ');
			}

			@Override
			public void visit(HardLineBreak hardLineBreak) {
				out.append(' ');
			}
		});
		return out.toString().strip();
	}

	private static String unique(String base, Map<String, Integer> used, Set<String> taken) {
		int seen = used.merge(base, 1, Integer::sum);
		String id = seen == 1 ? base : base + "-" + seen;
		while (!taken.add(id)) {
			seen = used.merge(base, 1, Integer::sum);
			id = base + "-" + seen;
		}
		return id;
	}

	private static int countWords(String text) {
		String trimmed = text.strip();
		return trimmed.isEmpty() ? 0 : trimmed.split("\\s+").length;
	}

}
