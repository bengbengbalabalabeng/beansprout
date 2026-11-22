package dev.danvega.beansprout.content;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;

/**
 * The block between the {@code ---} lines at the top of a post, and the Markdown after it.
 *
 * <p>Only the shapes a blog post needs are read: {@code key: value}, a value in single or double
 * quotes, a list as {@code [a, b]}, a list as one {@code - item} per line, and comments. This is
 * not YAML, on purpose: a small format with clear errors beats a large one with surprises.
 */
final class Frontmatter {

	/** A key at the start of a line: anything up to the first colon. */
	private static final Pattern KEY_VALUE = Pattern.compile("^([^\\s:#][^:]*?)\\s*:(.*)$", Pattern.DOTALL);

	/** The keys Beansprout reads. Any other key is ignored, whatever its shape. */
	static final Set<String> KNOWN_KEYS = Set.of("title", "date", "tags", "summary", "slug", "draft", "picture", "picture-alt", "caption", "updated",
			"canonical", "description", "image", "lastmod", "canonical_url", "aliases", "redirect_from", "author");

	/**
	 * Known keys that are ignored when indented lines follow them, as Jekyll themes nest
	 * {@code image:} and jekyll-seo-tag nests {@code author:}. The post is kept.
	 */
	private static final Set<String> NESTED_IGNORED = Set.of("image", "author");

	/**
	 * Known keys whose indented lines go on with the value, as Jekyll folds a long
	 * {@code description:} over lines. The lines are joined with spaces. An indented known key is
	 * still a mistake.
	 */
	private static final Set<String> FOLDED = Set.of("description");

	/** A YAML block indicator alone, like {@code >} or {@code |-}: the text starts on the next line. */
	private static final Pattern BLOCK = Pattern.compile("^[>|][-+]?$");

	/** The keys whose {@code [a, b]} value is a list. Anywhere else brackets are just text. */
	static final Set<String> LIST_KEYS = Set.of("tags", "aliases", "redirect_from");

	private static final Pattern LIST_ITEM = Pattern.compile("^\\s*-\\s+(.*)$", Pattern.DOTALL);

	private final Map<String, Object> values;

	private final String body;

	private final boolean toml;

	Frontmatter(Map<String, Object> values, String body, boolean toml) {
		this.values = values;
		this.body = body;
		this.toml = toml;
	}

	/** The Markdown after the frontmatter. */
	String body() {
		return body;
	}

	/** True when the frontmatter was Hugo's TOML between {@code +++} lines, so hints use {@code key = value}. */
	boolean toml() {
		return toml;
	}

	/** A single value, or null when the key is missing or empty. A list gives its first item. */
	@Nullable String text(String key) {
		Object value = values.get(key);
		if (value instanceof List<?> list) {
			return list.isEmpty() ? null : (String) list.getFirst();
		}
		String text = (String) value;
		return text == null || text.isEmpty() ? null : text;
	}

	/** A list: {@code [a, b]}, one {@code - item} per line, or a single value split on commas. */
	List<String> list(String key) {
		Object value = values.get(key);
		if (value == null) {
			return List.of();
		}
		if (value instanceof List<?> list) {
			return list.stream().map(String.class::cast).toList();
		}
		return splitList((String) value);
	}

	/** The raw value: a String, a List of Strings, or null when the key is missing. */
	@Nullable Object raw(String key) {
		return values.get(key);
	}

	static Frontmatter parse(String text) {
		String[] lines = text.replace("\r\n", "\n").replace("\r", "\n").split("\n", -1);
		int start = 0;
		if (lines.length > 0 && lines[0].startsWith("﻿")) {
			lines[0] = lines[0].substring(1);
		}
		if (lines.length > 0 && lines[start].strip().equals("+++")) {
			return TomlFrontmatter.parse(lines);
		}
		if (lines.length == 0 || !lines[start].strip().equals("---")) {
			throw new InvalidPostException("it has no frontmatter. Start the file with a --- line, then title: and date:, then another --- line.");
		}
		Map<String, Object> values = new LinkedHashMap<>();
		String openList = null;
		String lastKey = null;
		for (int i = start + 1; i < lines.length; i++) {
			String line = lines[i];
			if (line.strip().equals("---")) {
				String body = String.join("\n", java.util.Arrays.copyOfRange(lines, i + 1, lines.length));
				dropBareBlocks(values);
				return new Frontmatter(values, body, false);
			}
			if (line.isBlank() || line.strip().startsWith("#")) {
				continue;
			}
			Matcher item = LIST_ITEM.matcher(line);
			if (openList != null && item.matches()) {
				listAt(values, openList).add(unquote(stripComment(item.group(1)).strip()));
				continue;
			}
			if (Character.isWhitespace(line.charAt(0))) {
				// An indented line belongs to the key above it. Under an unknown key it is ignored,
				// whatever it says. Under a known key, or with no key above, it is a mistake.
				if (lastKey != null && !KNOWN_KEYS.contains(lastKey)) {
					continue;
				}
				Matcher indented = KEY_VALUE.matcher(line.strip());
				boolean knownKey = indented.matches() && KNOWN_KEYS.contains(indented.group(1));
				if (lastKey != null && FOLDED.contains(lastKey) && !knownKey) {
					fold(values, lastKey, line.strip());
					openList = null;
					continue;
				}
				if (lastKey != null && NESTED_IGNORED.contains(lastKey)) {
					values.remove(lastKey);
					openList = null;
					continue;
				}
				Matcher nested = KEY_VALUE.matcher(line.strip());
				String hint = nested.matches() && KNOWN_KEYS.contains(nested.group(1))
						? ". Start " + nested.group(1) + ": at the beginning of the line." : "";
				throw new InvalidPostException("line " + (i + 1) + " of the frontmatter is indented: " + line.strip() + hint);
			}
			Matcher keyValue = KEY_VALUE.matcher(line);
			if (!keyValue.matches()) {
				throw new InvalidPostException("line " + (i + 1) + " of the frontmatter is not key: value: " + line.strip());
			}
			String key = keyValue.group(1);
			lastKey = key;
			String raw = stripComment(keyValue.group(2)).strip();
			if (raw.isEmpty()) {
				values.put(key, new ArrayList<String>());
				openList = key;
			}
			else {
				values.put(key, raw.startsWith("[") && LIST_KEYS.contains(key) ? inlineList(raw, i + 1) : unquote(raw));
				openList = null;
			}
		}
		throw new InvalidPostException("its frontmatter has no closing --- line.");
	}

	/**
	 * Adds one indented line to a folded value. A quoted value that closes on this line loses its
	 * quotes, as on one line. A list of items under the key is left as it is.
	 */
	private static void fold(Map<String, Object> values, String key, String more) {
		Object current = values.get(key);
		if (current instanceof String text) {
			String joined = BLOCK.matcher(text).matches() ? more : text + " " + more;
			values.put(key, unquote(joined));
		}
		else if (current instanceof List<?> list && list.isEmpty()) {
			values.put(key, more);
		}
	}

	/** A folded key left with only its block indicator, like {@code >}, has no text. */
	private static void dropBareBlocks(Map<String, Object> values) {
		for (String key : FOLDED) {
			if (values.get(key) instanceof String text && BLOCK.matcher(text).matches()) {
				values.remove(key);
			}
		}
	}

	@SuppressWarnings("unchecked")
	private static List<String> listAt(Map<String, Object> values, String key) {
		return (List<String>) java.util.Objects.requireNonNull(values.get(key));
	}

	private static List<String> inlineList(String raw, int lineNumber) {
		if (!raw.endsWith("]")) {
			throw new InvalidPostException("line " + lineNumber + " of the frontmatter opens a [ list but does not close it.");
		}
		return splitList(raw.substring(1, raw.length() - 1));
	}

	/** Splits on commas outside quotes, then unquotes each item and drops empty ones. */
	private static List<String> splitList(String text) {
		List<String> items = new ArrayList<>();
		StringBuilder current = new StringBuilder();
		char quote = 0;
		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			if (quote != 0) {
				if (c == '\\' && quote == '"' && i + 1 < text.length()) {
					current.append(c).append(text.charAt(++i));
					continue;
				}
				if (c == '\'' && quote == '\'' && i + 1 < text.length() && text.charAt(i + 1) == '\'') {
					current.append(c).append(text.charAt(++i));
					continue;
				}
				if (c == quote) {
					quote = 0;
				}
			}
			else if ((c == '"' || c == '\'') && current.toString().isBlank()) {
				quote = c;
			}
			else if (c == ',') {
				addItem(items, current);
				continue;
			}
			current.append(c);
		}
		addItem(items, current);
		return items;
	}

	private static void addItem(List<String> items, StringBuilder current) {
		String item = unquote(current.toString().strip());
		if (!item.isEmpty()) {
			items.add(item);
		}
		current.setLength(0);
	}

	/**
	 * Drops a {@code # comment} after a value, unless the # is inside quotes. A quote only opens at
	 * the start of a value or of a list item, so an apostrophe in plain text is just a letter.
	 */
	private static String stripComment(String raw) {
		char quote = 0;
		char previous = 0;
		for (int i = 0; i < raw.length(); i++) {
			char c = raw.charAt(i);
			if (!Character.isWhitespace(c) && quote == 0) {
				char before = previous;
				previous = c;
				if ((c == '"' || c == '\'') && before != 0 && before != '[' && before != ',') {
					continue;
				}
			}
			if (quote != 0) {
				if (c == '\\' && quote == '"') {
					i++;
				}
				else if (c == '\'' && quote == '\'' && i + 1 < raw.length() && raw.charAt(i + 1) == '\'') {
					i++;
				}
				else if (c == quote) {
					quote = 0;
				}
			}
			else if (c == '"' || c == '\'') {
				quote = c;
			}
			else if (c == '#' && (i == 0 || Character.isWhitespace(raw.charAt(i - 1)))) {
				return raw.substring(0, i);
			}
		}
		return raw;
	}

	private static String unquote(String value) {
		if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
			StringBuilder out = new StringBuilder();
			String inner = value.substring(1, value.length() - 1);
			for (int i = 0; i < inner.length(); i++) {
				char c = inner.charAt(i);
				if (c == '\\' && i + 1 < inner.length()) {
					char next = inner.charAt(++i);
					out.append(next == 'n' ? '\n' : next);
				}
				else {
					out.append(c);
				}
			}
			return out.toString();
		}
		if (value.length() >= 2 && value.startsWith("'") && value.endsWith("'")) {
			return value.substring(1, value.length() - 1).replace("''", "'");
		}
		return value;
	}

}
