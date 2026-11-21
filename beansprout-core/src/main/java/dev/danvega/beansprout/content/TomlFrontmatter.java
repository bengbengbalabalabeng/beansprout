package dev.danvega.beansprout.content;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;

/**
 * Hugo's TOML frontmatter, between two {@code +++} lines, read into the same keys and values as
 * {@link Frontmatter}'s YAML.
 *
 * <p>Only the shapes a blog post needs are read: a string in double or single quotes, a date or
 * date and time, {@code true} and {@code false}, and an array of strings, which may span lines.
 * Any other value, such as a number, an inline table or a multi-line string, is skipped over and
 * ignored. A table header like {@code [params]} ends what is read, since every line after it
 * belongs to that table.
 */
final class TomlFrontmatter {

	private static final String PART = "(?:[A-Za-z0-9_-]+|\"(?:[^\"\\\\]|\\\\.)*\"|'[^']*')";

	/** A key, bare, quoted or dotted, then {@code =}. */
	private static final Pattern KEY_VALUE = Pattern.compile("^(" + PART + "(?:\\s*\\.\\s*" + PART + ")*)\\s*=(.*)$");

	private static final Pattern DATE = Pattern.compile(
			"^\\d{4}-\\d{2}-\\d{2}(?:[Tt ]\\d{2}:\\d{2}(?::\\d{2}(?:\\.\\d+)?)?(?:[Zz]|[+-]\\d{2}:\\d{2})?)?$");

	/** Keys whose value is a date or a flag. Any other shape is kept as written, so the parser rejects it. */
	private static final Set<String> CHECKED = Set.of("date", "updated", "lastmod", "draft", "unlisted");

	private TomlFrontmatter() {
	}

	/** Reads the lines after the opening {@code +++} line, which is {@code lines[0]}. */
	static Frontmatter parse(String[] lines) {
		int end = -1;
		for (int i = 1; i < lines.length; i++) {
			if (lines[i].strip().equals("+++")) {
				end = i;
				break;
			}
		}
		if (end < 0) {
			throw new InvalidPostException("its frontmatter has no closing +++ line.");
		}
		Map<String, Object> values = new LinkedHashMap<>();
		for (int i = 1; i < end; i++) {
			String line = lines[i].strip();
			if (line.isEmpty() || line.startsWith("#")) {
				continue;
			}
			if (line.startsWith("[")) {
				break;
			}
			var keyValue = KEY_VALUE.matcher(line);
			if (!keyValue.matches()) {
				throw new InvalidPostException("line " + (i + 1) + " of the frontmatter is not key = value: " + line);
			}
			Scan scan = scan(lines, i, end, keyValue.group(2));
			String key = keyValue.group(1);
			Object value = value(scan.text());
			if (value != null) {
				values.put(key, value);
			}
			else if (CHECKED.contains(key)) {
				// The parser checks these, as it does in YAML, so a value it cannot read skips the
				// post with a warning instead of quietly dropping a date or a draft flag.
				values.put(key, scan.text());
			}
			i = scan.lastLine();
		}
		String body = String.join("\n", Arrays.copyOfRange(lines, end + 1, lines.length));
		return new Frontmatter(values, body, true);
	}

	/** A value's source with comments taken out, and the line it ends on. */
	private record Scan(String text, int lastLine) {
	}

	/**
	 * Collects one value, starting with what follows {@code =} on line {@code start}. An array or a
	 * multi-line string goes on to the lines after it, up to the closing {@code +++} line.
	 */
	private static Scan scan(String[] lines, int start, int end, String first) {
		StringBuilder text = new StringBuilder();
		int depth = 0;
		// 0: outside a string, '"' and '\'': one line, 'B' and 'L': three quotes, so many lines
		char string = 0;
		String line = first;
		int at = start;
		while (true) {
			for (int c = 0; c < line.length(); c++) {
				char ch = line.charAt(c);
				if (string == 0) {
					if (ch == '#') {
						break;
					}
					if (line.startsWith("\"\"\"", c) || line.startsWith("'''", c)) {
						string = ch == '"' ? 'B' : 'L';
						text.append(line, c, c + 3);
						c += 2;
						continue;
					}
					if (ch == '"' || ch == '\'') {
						string = ch;
					}
					else if (ch == '[' || ch == '{') {
						depth++;
					}
					else if (ch == ']' || ch == '}') {
						depth--;
					}
					text.append(ch);
				}
				else if ((string == 'B' && line.startsWith("\"\"\"", c)) || (string == 'L' && line.startsWith("'''", c))) {
					text.append(line, c, c + 3);
					c += 2;
					string = 0;
				}
				else {
					text.append(ch);
					if (ch == '\\' && (string == '"' || string == 'B') && c + 1 < line.length()) {
						text.append(line.charAt(++c));
					}
					else if ((string == '"' || string == '\'') && ch == string) {
						string = 0;
					}
				}
			}
			boolean open = depth > 0 || string == 'B' || string == 'L';
			if (string == '"' || string == '\'') {
				throw new InvalidPostException("line " + (at + 1) + " of the frontmatter opens a quote but does not close it.");
			}
			if (!open) {
				if (text.toString().isBlank()) {
					throw new InvalidPostException("line " + (start + 1) + " of the frontmatter has no value after =.");
				}
				return new Scan(text.toString().strip(), at);
			}
			at++;
			if (at >= end) {
				throw new InvalidPostException("line " + (start + 1) + " of the frontmatter opens a value that does not close before +++.");
			}
			text.append('\n');
			line = lines[at];
		}
	}

	/** A String or a List of Strings, or null for a value Beansprout does not read. */
	private static @Nullable Object value(String text) {
		if (text.startsWith("\"\"\"") || text.startsWith("'''")) {
			return null;
		}
		if (text.startsWith("\"") || text.startsWith("'")) {
			int[] at = { 0 };
			String value = string(text, at);
			return value != null && text.substring(at[0]).isBlank() ? value : null;
		}
		if (text.startsWith("[")) {
			return array(text);
		}
		if (text.equals("true") || text.equals("false")) {
			return text;
		}
		if (DATE.matcher(text).matches()) {
			String date = text.length() > 10 ? text.substring(0, 10) + "T" + text.substring(11) : text;
			return date.endsWith("z") ? date.substring(0, date.length() - 1) + "Z" : date;
		}
		return null;
	}

	/** An array whose items are all strings, or null for any other array. */
	private static @Nullable List<String> array(String text) {
		List<String> items = new ArrayList<>();
		int[] at = { 1 };
		boolean needComma = false;
		while (true) {
			skipSpace(text, at);
			if (at[0] >= text.length()) {
				return null;
			}
			char ch = text.charAt(at[0]);
			if (ch == ']') {
				at[0]++;
				return text.substring(at[0]).isBlank() ? items : null;
			}
			if (ch == ',' && needComma) {
				at[0]++;
				needComma = false;
				continue;
			}
			if (needComma || (ch != '"' && ch != '\'') || text.startsWith("\"\"\"", at[0]) || text.startsWith("'''", at[0])) {
				return null;
			}
			String item = string(text, at);
			if (item == null) {
				return null;
			}
			items.add(item);
			needComma = true;
		}
	}

	/** An ASCII hex digit. TOML allows no other digits in an escape. */
	private static boolean hexDigit(int c) {
		return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
	}

	private static void skipSpace(String text, int[] at) {
		while (at[0] < text.length() && Character.isWhitespace(text.charAt(at[0]))) {
			at[0]++;
		}
	}

	/** One quoted string starting at {@code at}, which is moved past it, or null when it is not one. */
	private static @Nullable String string(String text, int[] at) {
		char quote = text.charAt(at[0]);
		StringBuilder out = new StringBuilder();
		for (int i = at[0] + 1; i < text.length(); i++) {
			char ch = text.charAt(i);
			if (ch == quote) {
				at[0] = i + 1;
				return out.toString();
			}
			if (ch == '\n') {
				return null;
			}
			if (ch != '\\' || quote == '\'') {
				out.append(ch);
				continue;
			}
			if (++i >= text.length()) {
				return null;
			}
			char next = text.charAt(i);
			switch (next) {
				case 'n' -> out.append('\n');
				case 't' -> out.append('\t');
				case 'r' -> out.append('\r');
				case 'b' -> out.append('\b');
				case 'f' -> out.append('\f');
				case 'e' -> out.append('\u001b');
				case '"', '\\' -> out.append(next);
				case 'u', 'U' -> {
					int length = next == 'u' ? 4 : 8;
					if (i + length >= text.length()) {
						return null;
					}
					String hex = text.substring(i + 1, i + 1 + length);
					if (!hex.chars().allMatch(TomlFrontmatter::hexDigit)) {
						return null;
					}
					long code = Long.parseLong(hex, 16);
					if (code > Character.MAX_CODE_POINT || (code >= 0xD800 && code <= 0xDFFF)) {
						return null;
					}
					out.appendCodePoint((int) code);
					i += length;
				}
				default -> {
					return null;
				}
			}
		}
		return null;
	}

}
