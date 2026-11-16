package dev.danvega.beansprout.code;

/** Escapes text for HTML the way CommonMark does: ampersand, angle brackets and double quotes. */
final class Html {

	private Html() {
	}

	static void escape(String text, int from, int to, StringBuilder out) {
		for (int i = from; i < to; i++) {
			char c = text.charAt(i);
			switch (c) {
				case '&' -> out.append("&amp;");
				case '<' -> out.append("&lt;");
				case '>' -> out.append("&gt;");
				case '"' -> out.append("&quot;");
				default -> out.append(c);
			}
		}
	}

}
