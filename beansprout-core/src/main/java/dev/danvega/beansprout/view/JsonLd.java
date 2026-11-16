package dev.danvega.beansprout.view;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Writes the small JSON objects a post's structured data needs. Strings are escaped for JSON, and
 * {@code <}, {@code >}, {@code &} and line separators become {@code \\u} escapes, so the result
 * can sit inside a {@code <script>} element with no way to end it early.
 */
final class JsonLd {

	private JsonLd() {
	}

	/** An object from ordered entries. A value is a String or another map from this method. */
	static String object(Map<String, Object> entries) {
		StringBuilder out = new StringBuilder("{");
		for (Map.Entry<String, Object> entry : entries.entrySet()) {
			if (out.length() > 1) {
				out.append(',');
			}
			out.append(string(entry.getKey())).append(':');
			Object value = entry.getValue();
			if (value instanceof Map<?, ?> nested) {
				Map<String, Object> copy = new LinkedHashMap<>();
				nested.forEach((key, inner) -> copy.put(String.valueOf(key), inner));
				out.append(object(copy));
			}
			else {
				out.append(string(String.valueOf(value)));
			}
		}
		return out.append('}').toString();
	}

	static String string(String value) {
		StringBuilder out = new StringBuilder("\"");
		for (int i = 0; i < value.length(); i++) {
			char c = value.charAt(i);
			switch (c) {
				case '"' -> out.append("\\\"");
				case '\\' -> out.append("\\\\");
				case '\n' -> out.append("\\n");
				case '\r' -> out.append("\\r");
				case '\t' -> out.append("\\t");
				default -> {
					if (c < 0x20 || c == '<' || c == '>' || c == '&' || c == '\u2028' || c == '\u2029') {
						out.append(String.format("\\u%04x", (int) c));
					}
					else {
						out.append(c);
					}
				}
			}
		}
		return out.append('"').toString();
	}

}
