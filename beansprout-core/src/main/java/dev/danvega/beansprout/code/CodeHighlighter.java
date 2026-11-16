package dev.danvega.beansprout.code;

import java.util.Optional;

/**
 * Turns the code in one fence into highlighted HTML. Beansprout's built-in one is
 * {@link PrismHighlighter}. An app can define its own as a bean.
 */
@FunctionalInterface
public interface CodeHighlighter {

	/** Highlights nothing, so every fence is shown as plain, escaped code. */
	CodeHighlighter NONE = (language, code) -> Optional.empty();

	/**
	 * Highlights code.
	 *
	 * @param language the fence's language, as written after the backticks, such as {@code java}
	 * @param code the code, unescaped
	 * @return HTML whose text is the code exactly, with every character escaped, or empty for a
	 *     language this highlighter does not know
	 */
	Optional<String> highlight(String language, String code);

}
