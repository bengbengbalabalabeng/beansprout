package dev.danvega.beansprout.view;

import java.util.regex.Pattern;

/**
 * Every address the blog serves, built from the app's context path and the blog's base path.
 * Nothing here reads a request, so pages can be rendered for a static export.
 */
public final class Links {

	private static final Pattern BASE_PATH = Pattern.compile("(/[A-Za-z0-9._~-]+)+");

	private final String root;

	private final String prefix;

	/**
	 * Creates the links for one blog.
	 *
	 * @param root the app's context path: empty, or like {@code /app}
	 * @param base the blog's base path, like {@code /blog}
	 */
	public Links(String root, String base) {
		if (!BASE_PATH.matcher(base).matches()) {
			throw new IllegalArgumentException("beansprout.base-path must look like /blog, with no trailing slash, not '" + base + "'");
		}
		this.root = root.endsWith("/") ? root.substring(0, root.length() - 1) : root;
		this.prefix = this.root + base;
	}

	public String root() {
		return root;
	}

	public String home() {
		return prefix;
	}

	/** Page n of the index. Page 1 is the index itself. */
	public String page(int n) {
		return n <= 1 ? home() : prefix + "/page/" + n;
	}

	public String post(String slug) {
		return prefix + "/" + slug;
	}

	public String tag(String slug) {
		return prefix + "/tags/" + slug;
	}

	/** Page n of a tag. Page 1 is the tag page itself. */
	public String tagPage(String slug, int n) {
		return n <= 1 ? tag(slug) : tag(slug) + "/page/" + n;
	}

	public String archive() {
		return prefix + "/archive";
	}

	/**
	 * A picture's address for a page: a path starting with {@code /} gets the context path in
	 * front, a full address is used as written, and empty stays empty.
	 */
	public String picture(String address) {
		return inApp(address);
	}

	/**
	 * An address the app set, such as a menu link: a path starting with {@code /} gets the context
	 * path in front, and a full address stays as it is.
	 */
	public String inApp(String address) {
		return address.startsWith("/") ? root + address : address;
	}

	public String feed() {
		return prefix + "/feed.xml";
	}

	public String sitemap() {
		return prefix + "/sitemap.xml";
	}

}
