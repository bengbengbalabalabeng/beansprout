package dev.danvega.beansprout.content;

import java.util.Locale;

/** The picture addresses Beansprout accepts: a path in the app, or a full web address. */
public final class Pictures {

	private Pictures() {
	}

	/**
	 * True for a path in the app, or a full {@code http://} or {@code https://} address with a host.
	 * A path starts with one {@code /}. A second {@code /} or a {@code \} after it would point a
	 * browser at another site, so both are refused, like a relative path or a {@code javascript:}
	 * address.
	 */
	public static boolean valid(String address) {
		if (address.startsWith("/")) {
			return address.length() == 1 || (address.charAt(1) != '/' && address.charAt(1) != '\\');
		}
		String lower = address.toLowerCase(Locale.ROOT);
		for (String scheme : new String[] { "http://", "https://" }) {
			if (lower.startsWith(scheme)) {
				return address.length() > scheme.length() && "/\\?#".indexOf(address.charAt(scheme.length())) < 0;
			}
		}
		return false;
	}

}
