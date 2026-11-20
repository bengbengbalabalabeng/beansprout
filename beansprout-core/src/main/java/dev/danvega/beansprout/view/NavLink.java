package dev.danvega.beansprout.view;

import java.util.Locale;

/**
 * A link the app adds to every theme's main menu, after the theme's own items. It is how a blog
 * inside a bigger site links back to the rest of it.
 *
 * @param label the text the reader sees
 * @param href a path starting with {@code /}, inside the app, or a full http or https address
 */
public record NavLink(String label, String href) {

	public NavLink {
		if (label == null || label.isBlank()) {
			throw new IllegalArgumentException("Each of beansprout.links needs a label, like 'Shop'");
		}
		if (href == null || !valid(href)) {
			throw new IllegalArgumentException("beansprout.links '" + label
					+ "' needs an href that is a path starting with /, like /shop, or an http or https address, not '"
					+ href + "'");
		}
	}

	private static boolean valid(String href) {
		if (href.startsWith("//")) {
			return false;
		}
		if (href.startsWith("/")) {
			return true;
		}
		String lower = href.toLowerCase(Locale.ROOT);
		return lower.startsWith("http://") || lower.startsWith("https://");
	}

}
