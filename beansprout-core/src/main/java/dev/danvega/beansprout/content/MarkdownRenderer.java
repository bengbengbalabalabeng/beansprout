package dev.danvega.beansprout.content;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

/** CommonMark with GitHub tables. Gives the HTML, the {@code ##} headings, and plain text for summaries. */
final class MarkdownRenderer {

	private static final List<Extension> EXTENSIONS = List.of(TablesExtension.create());

	private final Parser parser = Parser.builder().extensions(EXTENSIONS).build();

	private final TextContentRenderer text = TextContentRenderer.builder().extensions(EXTENSIONS).build();

	record Rendered(String html, List<Heading> headings, String firstParagraph, int words) {
	}

	Rendered render(String markdown) {
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
			.attributeProviderFactory(context -> (node, tagName, attributes) -> {
				String id = ids.get(node);
				if (id != null) {
					attributes.put("id", id);
				}
			})
			.build();

		return new Rendered(html.render(document), headings, firstParagraph(document), countWords(text.render(document)));
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
