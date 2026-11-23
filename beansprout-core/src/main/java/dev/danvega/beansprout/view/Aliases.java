package dev.danvega.beansprout.view;

import java.io.ByteArrayOutputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import dev.danvega.beansprout.content.Post;
import dev.danvega.beansprout.content.TagSummary;
import org.jspecify.annotations.Nullable;

/**
 * The old paths of live posts, each pointing to its post's address. They come from {@code aliases}
 * and {@code redirect_from}, and from a dated file name whose slug left the date out.
 *
 * <p>An alias's {@code %XX} escapes are read as UTF-8, the way a request's path is read, so
 * {@code /caf%C3%A9.html} and {@code /café.html} are one alias. An alias that is not a plain path from
 * the app's root, or that is a page the blog serves, robots.txt included, is ignored. When two posts name the same alias, the post whose file comes first by path gets it.
 * Each of these is logged once as a warning. A draft or a post dated later has no aliases yet,
 * and gets them when it goes live, with no restart.
 */
public final class Aliases {

	private static final Logger LOG = System.getLogger(Aliases.class.getName());

	private static final Result NONE = new Result(List.of(), List.of());

	private final Pages pages;

	private final Set<String> warned = ConcurrentHashMap.newKeySet();

	/** The aliases of the posts behind these pages. */
	public Aliases(Pages pages) {
		this.pages = pages;
	}

	/** Every live alias, in the order of the posts' file paths. */
	public List<Alias> all() {
		return resolve().aliases();
	}

	/**
	 * The post's address for a path in the app, without the context path, or empty when no live
	 * post has it as an alias. A trailing slash does not matter.
	 */
	public Optional<String> target(String path) {
		String key = key(path);
		for (Alias alias : all()) {
			if (key(alias.path()).equals(key)) {
				return Optional.of(alias.target());
			}
		}
		return Optional.empty();
	}

	/** Why aliases were ignored, one line each, as they are logged. */
	public List<String> problems() {
		return resolve().problems();
	}

	private Result resolve() {
		List<Post> posts = new ArrayList<>(pages.posts().published());
		if (posts.stream().allMatch(post -> post.aliases().isEmpty())) {
			return NONE;
		}
		posts.sort(Comparator.comparing(Post::path));
		Links links = pages.links();
		Set<String> served = served(links);
		Map<String, Post> owners = new HashMap<>();
		List<Alias> aliases = new ArrayList<>();
		List<String> problems = new ArrayList<>();
		for (Post post : posts) {
			for (String written : post.aliases()) {
				String alias = decode(written);
				if (alias == null || !plain(alias)) {
					problems.add("Beansprout ignored the alias '" + written + "' of " + post.path()
							+ ": an alias is a path that starts with / and names one page, like /2019/03/14/hello.html.");
					continue;
				}
				String key = key(alias);
				if (served.contains(key)) {
					problems.add("Beansprout ignored the alias '" + alias + "' of " + post.path()
							+ ": the blog serves a page of its own at " + alias + ".");
					continue;
				}
				Post owner = owners.putIfAbsent(key, post);
				if (owner == null) {
					aliases.add(new Alias(alias, links.post(post.slug())));
				}
				else if (!owner.path().equals(post.path())) {
					problems.add("Beansprout gave the alias '" + alias + "' to " + owner.path() + ", the first by path. "
							+ post.path() + " names it too.");
				}
			}
		}
		for (String problem : problems) {
			if (warned.add(problem)) {
				LOG.log(Level.WARNING, problem);
			}
		}
		return new Result(List.copyOf(aliases), List.copyOf(problems));
	}

	/** The blog's own pages, as paths in the app without the context path. */
	private Set<String> served(Links links) {
		Set<String> served = new HashSet<>();
		for (PageUrl url : pages.urls()) {
			served.add(key(inApp(url.path(), links)));
		}
		// Page 1 has no page/1 address of its own, but the blog answers it with a redirect to page 1.
		served.add(key(inApp(links.home() + "/page/1", links)));
		for (TagSummary tag : pages.posts().tags()) {
			served.add(key(inApp(links.tag(tag.slug()) + "/page/1", links)));
			served.add(key(inApp(links.tagFeed(tag.slug()), links)));
		}
		served.add(key(inApp(links.feed(), links)));
		served.add(key(inApp(links.sitemap(), links)));
		// Beansprout's robots.txt, at the site's root.
		served.add("/robots.txt");
		return served;
	}

	private static String inApp(String address, Links links) {
		return address.substring(links.root().length());
	}

	/**
	 * A path from the root with at least one segment, and nothing a request path could not match
	 * or a file name could not hold: no {@code ?} or {@code #}, no backslash or control character,
	 * and no segment that is empty, {@code .} or {@code ..}. One trailing slash is fine.
	 */
	private static boolean plain(String alias) {
		if (alias.length() < 2 || !alias.startsWith("/")) {
			return false;
		}
		if (alias.chars().anyMatch(ch -> ch == '?' || ch == '#' || ch == '\\' || ch < 0x20 || ch == 0x7f)) {
			return false;
		}
		String path = alias.endsWith("/") ? alias.substring(1, alias.length() - 1) : alias.substring(1);
		for (String segment : path.split("/", -1)) {
			if (segment.isEmpty() || segment.equals(".") || segment.equals("..")) {
				return false;
			}
		}
		return true;
	}

	/**
	 * An alias with its {@code %XX} escapes read as UTF-8, or null when one cannot be read, or when
	 * it spells a {@code /}, which a request path cannot hold.
	 */
	private static @Nullable String decode(String alias) {
		if (alias.indexOf('%') < 0) {
			return alias;
		}
		StringBuilder out = new StringBuilder();
		int i = 0;
		while (i < alias.length()) {
			if (alias.charAt(i) != '%') {
				out.append(alias.charAt(i++));
				continue;
			}
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			while (i < alias.length() && alias.charAt(i) == '%') {
				if (i + 2 >= alias.length()) {
					return null;
				}
				int high = Character.digit(alias.charAt(i + 1), 16);
				int low = Character.digit(alias.charAt(i + 2), 16);
				if (high < 0 || low < 0 || !ascii(alias.charAt(i + 1)) || !ascii(alias.charAt(i + 2))) {
					return null;
				}
				bytes.write(high * 16 + low);
				i += 3;
			}
			try {
				String text = StandardCharsets.UTF_8.newDecoder()
					.onMalformedInput(CodingErrorAction.REPORT)
					.onUnmappableCharacter(CodingErrorAction.REPORT)
					.decode(ByteBuffer.wrap(bytes.toByteArray()))
					.toString();
				if (text.indexOf('/') >= 0) {
					return null;
				}
				out.append(text);
			}
			catch (CharacterCodingException ex) {
				return null;
			}
		}
		return out.toString();
	}

	private static boolean ascii(char ch) {
		return ch < 0x80;
	}

	/** A path without one trailing slash, so {@code /a/} and {@code /a} match. */
	private static String key(String path) {
		return path.length() > 1 && path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
	}

	private record Result(List<Alias> aliases, List<String> problems) {
	}

}
