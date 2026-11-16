package dev.danvega.beansprout.content;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import dev.danvega.beansprout.code.CodeHighlighter;
import org.commonmark.node.FencedCodeBlock;
import org.commonmark.node.Node;
import org.commonmark.renderer.NodeRenderer;
import org.commonmark.renderer.html.HtmlNodeRendererContext;
import org.commonmark.renderer.html.HtmlWriter;

/**
 * Renders a code fence the way CommonMark does, with {@code class="language-x"} on the code
 * element, and puts the highlighter's HTML inside when it knows the language.
 */
final class FencedCodeRenderer implements NodeRenderer {

	/** Longer code is shown plain, so one huge block cannot slow down reading the posts. */
	static final int MAX_HIGHLIGHT = 100_000;

	/** How long one post may spend highlighting. Fences after that are shown plain. */
	static final long POST_BUDGET_NANOS = java.util.concurrent.TimeUnit.SECONDS.toNanos(1);

	private final HtmlNodeRendererContext context;

	private final CodeHighlighter highlighter;

	/** One renderer is made for each post, so the budget starts when the post starts rendering. */
	private final long postDeadline;

	FencedCodeRenderer(HtmlNodeRendererContext context, CodeHighlighter highlighter) {
		this.context = context;
		this.highlighter = highlighter;
		this.postDeadline = System.nanoTime() + POST_BUDGET_NANOS;
	}

	@Override
	public Set<Class<? extends Node>> getNodeTypes() {
		return Set.of(FencedCodeBlock.class);
	}

	@Override
	public void render(Node node) {
		FencedCodeBlock block = (FencedCodeBlock) node;
		String literal = block.getLiteral();
		String info = block.getInfo();
		String language = info == null ? "" : info.strip().split("\\s+", 2)[0];

		Map<String, String> attributes = new LinkedHashMap<>();
		if (!language.isEmpty()) {
			attributes.put("class", "language-" + language);
		}
		Optional<String> highlighted = highlight(language, literal);

		HtmlWriter html = context.getWriter();
		html.line();
		html.tag("pre", context.extendAttributes(block, "pre", Map.of()));
		html.tag("code", context.extendAttributes(block, "code", attributes));
		if (highlighted.isPresent()) {
			html.raw(highlighted.get());
		}
		else {
			html.text(literal);
		}
		html.tag("/code");
		html.tag("/pre");
		html.line();
	}

	/** The highlighter's HTML, or empty to show the fence plain. A failing highlighter never fails the post. */
	private Optional<String> highlight(String language, String literal) {
		int codeLength = literal.endsWith("\n") ? literal.length() - 1 : literal.length();
		if (language.isEmpty() || codeLength > MAX_HIGHLIGHT || System.nanoTime() > postDeadline) {
			return Optional.empty();
		}
		try {
			Optional<String> result = highlighter.highlight(language, literal);
			// An app's highlighter may break the contract and return null. Treat it as "not known".
			return result == null ? Optional.empty() : result;
		}
		catch (RuntimeException | StackOverflowError | LinkageError | AssertionError ex) {
			// An app's highlighter failed on this fence: show it plain. OutOfMemoryError and other
			// VM errors are left alone, since the JVM itself is in trouble then.
			return Optional.empty();
		}
	}

}
