package dev.danvega.beansprout.code;

import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;

/**
 * One kind of token in a grammar.
 *
 * @param type the Prism token type, and any alias after a space, such as {@code key atrule}
 * @param pattern what the token looks like
 * @param lookbehind when true, the pattern's first group is context before the token, not part of it
 * @param inside a grammar for the token's own text, such as attributes inside a tag, or null
 */
record Rule(String type, Pattern pattern, boolean lookbehind, @Nullable Grammar inside) {
}
