package dev.danvega.beansprout.content;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/** Turns titles, file names and tags into the part of an address that names them. */
public final class Slugs {

	private static final Pattern MARKS = Pattern.compile("\\p{M}+");

	private static final Pattern NOT_ALLOWED = Pattern.compile("[^a-z0-9]+");

	private static final Pattern EDGE_DASHES = Pattern.compile("^-+|-+$");

	private Slugs() {
	}

	/**
	 * Lowercase letters and digits joined by single dashes, with accents removed. Text with no
	 * Latin letters or digits gives an empty slug.
	 */
	public static String slugify(String text) {
		String plain = MARKS.matcher(Normalizer.normalize(text, Normalizer.Form.NFD)).replaceAll("");
		String dashed = NOT_ALLOWED.matcher(plain.toLowerCase(Locale.ROOT)).replaceAll("-");
		return EDGE_DASHES.matcher(dashed).replaceAll("");
	}

}
