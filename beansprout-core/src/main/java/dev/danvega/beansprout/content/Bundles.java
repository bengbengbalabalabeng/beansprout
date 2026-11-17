package dev.danvega.beansprout.content;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;

/**
 * Posts that are folders. A folder under the posts location that holds {@code index.md} is one
 * post, a bundle, and the images next to it are served at the post's address.
 */
public final class Bundles {

	/** The file that makes a folder a bundle. */
	public static final String INDEX = "index.md";

	/** The images a bundle may serve, by extension, with their content types. */
	private static final Map<String, String> IMAGE_TYPES = Map.of("png", "image/png", "jpg", "image/jpeg", "jpeg",
			"image/jpeg", "gif", "image/gif", "webp", "image/webp", "avif", "image/avif");

	private static final String HEX = "0123456789ABCDEF";

	private static final Pattern SCHEME = Pattern.compile("^[A-Za-z][A-Za-z0-9+.-]*:");

	private Bundles() {
	}

	/**
	 * The bundle folder a file is the {@code index.md} of, like {@code 2026/welcome}, or null when
	 * the file is not one. An {@code index.md} at the top of the posts location is a plain post.
	 */
	public static @Nullable String folder(String path) {
		String suffix = "/" + INDEX;
		return path.endsWith(suffix) && path.length() > suffix.length()
				? path.substring(0, path.length() - suffix.length()) : null;
	}

	/** True when a file sits inside one of the bundle folders and is not that folder's own {@code index.md}. */
	static boolean insideAnother(String path, Collection<String> folders) {
		for (String folder : folders) {
			if (path.startsWith(folder + "/") && !path.equals(folder + "/" + INDEX)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * A file's path inside a bundle's folder, as written in frontmatter or decoded from a request,
	 * or null when it is not one. It must be relative, with no {@code \\} or control character, and
	 * no segment that is empty or hidden, which also rules out {@code .} and {@code ..}, so it can
	 * never leave the folder. A leading {@code ./} is dropped. Any other character is part of the
	 * name, so {@code 100%.png}, {@code a#b.png} and {@code a:b.png} are files too.
	 */
	public static @Nullable String relative(String path) {
		String tidy = path;
		while (tidy.startsWith("./")) {
			tidy = tidy.substring(2);
		}
		if (tidy.isEmpty() || tidy.startsWith("/") || tidy.chars().anyMatch(ch -> ch == '\\' || ch < 0x20 || ch == 0x7f)) {
			return null;
		}
		for (String segment : tidy.split("/", -1)) {
			if (segment.isEmpty() || segment.startsWith(".")) {
				return null;
			}
		}
		return tidy;
	}

	/**
	 * True when an image's {@code src}, as it appears in HTML, points at a file in the post's own
	 * folder: a relative URL whose path, with {@code .} and {@code ..} resolved the way a browser
	 * resolves them and then decoded, is a {@link #relative(String)} path. A query or fragment after
	 * the path is allowed. A {@code /} path, a full address, {@code //host} and a path that climbs
	 * out of the folder are not.
	 */
	static boolean inFolder(String written) {
		String src = trim(written);
		if (src.startsWith("/") || src.startsWith("#") || src.startsWith("?") || SCHEME.matcher(src).find()) {
			return false;
		}
		List<String> segments = segments(src.substring(0, pathEnd(src)));
		if (segments == null) {
			return false;
		}
		List<String> decoded = new ArrayList<>();
		segments.forEach(segment -> decoded.add(decode(segment)));
		return relative(String.join("/", decoded)) != null;
	}

	/**
	 * An image's {@code src} from a bundle's body, ready to go after the bundle's address: trimmed
	 * the way a browser trims it, with {@code .} and {@code ..} resolved, and a {@code %} that
	 * starts no escape written as {@code %25} and a {@code ;} as {@code %3B}, so the server reads
	 * the name as written. Only call it for a {@code src} that {@link #inFolder(String)} accepts.
	 */
	static String tidy(String written) {
		String src = trim(written);
		int end = pathEnd(src);
		List<String> segments = segments(src.substring(0, end));
		String path = segments == null ? src.substring(0, end) : String.join("/", segments);
		StringBuilder out = new StringBuilder();
		for (int i = 0; i < path.length(); i++) {
			char c = path.charAt(i);
			out.append(c == '%' && !escape(path, i) ? "%25" : c == ';' ? "%3B" : String.valueOf(c));
		}
		return out.append(src.substring(end)).toString();
	}

	/** Where the path ends: at the first {@code ?} or {@code #}, or the end. */
	private static int pathEnd(String src) {
		int end = src.length();
		for (char stop : new char[] { '?', '#' }) {
			int at = src.indexOf(stop);
			if (at >= 0 && at < end) {
				end = at;
			}
		}
		return end;
	}

	/**
	 * A relative URL path's segments with {@code .} and {@code ..} resolved, written or
	 * percent-encoded, or null when a {@code ..} climbs above the start.
	 */
	private static @Nullable List<String> segments(String path) {
		List<String> out = new ArrayList<>();
		String[] parts = path.split("/", -1);
		for (int i = 0; i < parts.length; i++) {
			String decoded = decode(parts[i]);
			boolean last = i == parts.length - 1;
			if (decoded.equals(".")) {
				if (last) {
					out.add("");
				}
			}
			else if (decoded.equals("..")) {
				if (out.isEmpty()) {
					return null;
				}
				out.remove(out.size() - 1);
				if (last) {
					out.add("");
				}
			}
			else {
				out.add(parts[i]);
			}
		}
		return out;
	}

	/** Without the spaces and control characters a browser drops from both ends of a URL. */
	private static String trim(String text) {
		int start = 0;
		int end = text.length();
		while (start < end && text.charAt(start) <= ' ') {
			start++;
		}
		while (end > start && text.charAt(end - 1) <= ' ') {
			end--;
		}
		return text.substring(start, end);
	}

	/** True when a {@code %} at this index starts an escape: two ASCII hex digits. */
	private static boolean escape(String text, int at) {
		return at + 2 < text.length() && hex(text.charAt(at + 1)) >= 0 && hex(text.charAt(at + 2)) >= 0;
	}

	private static int hex(char c) {
		return c < 0x80 ? Character.digit(c, 16) : -1;
	}

	/** A path as it goes in a URL: each segment percent-encoded as UTF-8, the {@code /} kept. */
	static String encode(String path) {
		StringBuilder out = new StringBuilder();
		for (byte b : path.getBytes(StandardCharsets.UTF_8)) {
			char c = (char) (b & 0xff);
			if (c == '/' || (c < 0x80 && (Character.isLetterOrDigit(c) || "-._~!$&'()*+,=:@".indexOf(c) >= 0))) {
				out.append(c);
			}
			else {
				out.append('%').append(HEX.charAt((b >> 4) & 0xf)).append(HEX.charAt(b & 0xf));
			}
		}
		return out.toString();
	}

	/** Percent-decodes UTF-8. A {@code %} that starts no escape stays a {@code %}. */
	private static String decode(String text) {
		if (text.indexOf('%') < 0) {
			return text;
		}
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			if (c == '%' && escape(text, i)) {
				bytes.write(hex(text.charAt(i + 1)) * 16 + hex(text.charAt(i + 2)));
				i += 2;
			}
			else {
				byte[] encoded = String.valueOf(c).getBytes(StandardCharsets.UTF_8);
				bytes.write(encoded, 0, encoded.length);
			}
		}
		return bytes.toString(StandardCharsets.UTF_8);
	}

	/** The content type of an image a bundle may serve, or null when the file is not one. */
	public static @Nullable String imageType(String path) {
		int dot = path.lastIndexOf('.');
		return dot < 0 ? null : IMAGE_TYPES.get(path.substring(dot + 1).toLowerCase(Locale.ROOT));
	}

}
