package dev.danvega.beansprout.export;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import dev.danvega.beansprout.view.Links;
import dev.danvega.beansprout.view.PageUrl;
import dev.danvega.beansprout.view.Pages;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

/**
 * Writes the blog as files for a static host. Every page in {@link Pages#urls()}, the feed and the
 * sitemap are fetched from the running app, and so is every file they point to: stylesheets,
 * pictures, fonts. Nothing is written until everything was fetched, so a failed fetch leaves the
 * folder as it was. A failed write can leave it half written.
 */
public final class StaticExport {

	/** The file that marks a folder an earlier export wrote, so this one may empty it. */
	public static final String MARKER = ".beansprout-export";

	private static final Log LOG = LogFactory.getLog(StaticExport.class);

	/** A real tag: a name, then attributes. Text and escaped code hold no {@code <}, so they never match. */
	private static final Pattern TAG = Pattern.compile("<[a-zA-Z][a-zA-Z0-9-]*+(?:\\s++[^\\s\"'>/=]++"
			+ "(?:\\s*+=\\s*+(?:\"[^\"]*+\"|'[^']*+'|[^\\s\"'>]++))?+)*+\\s*+/?>");

	/**
	 * One attribute of a tag: its name, then its value in double quotes, single quotes, or none.
	 * Read one after another, each quoted value is used up whole, so text inside it is never an
	 * attribute.
	 */
	private static final Pattern ATTRIBUTE = Pattern.compile("\\s++([^\\s\"'>/=]++)"
			+ "(?:\\s*+=\\s*+(?:\"([^\"]*+)\"|'([^']*+)'|([^\\s\"'>]++)))?+");

	private static final Pattern COMMENT = Pattern.compile("(?s)<!--.*?-->");

	private static final Pattern IPV6_HOST = Pattern.compile("(?i)^[a-z][a-z0-9+.-]*://\\[.*");

	private static final Pattern CSS_URL = Pattern.compile("url\\(\\s*(?:\"([^\"]*)\"|'([^']*)'|([^)'\"\\s]*))\\s*\\)",
			Pattern.CASE_INSENSITIVE);

	private static final Pattern CSS_IMPORT = Pattern.compile("@import\\s+(?:\"([^\"]*)\"|'([^']*)')",
			Pattern.CASE_INSENSITIVE);

	private static final Set<String> NOT_FETCHED = Set.of("mailto", "tel", "javascript", "data");

	private static final Duration TIMEOUT = Duration.ofSeconds(30);

	private final Pages pages;

	private final URI site;

	private final Supplier<String> notFound;

	private final Path output;

	private final List<String> neverFetched;

	/**
	 * Creates the export.
	 *
	 * @param pages the blog's pages, for the list of addresses and the links
	 * @param siteUrl where the site is published, like {@code https://name.github.io}, with no path
	 * @param notFound renders the theme's not-found page with no requested address
	 * @param output the folder to write to
	 * @param neverFetched addresses in the app that are not part of a static site, like the MCP
	 *     server's: a link to one, or to an address under one, is left out without a request
	 */
	public StaticExport(Pages pages, String siteUrl, Supplier<String> notFound, Path output,
			List<String> neverFetched) {
		this.pages = pages;
		this.site = URI.create(siteUrl.strip());
		this.notFound = notFound;
		this.output = output.toAbsolutePath().normalize();
		this.neverFetched = List.copyOf(neverFetched);
	}

	/** The folder the export writes to. */
	public Path output() {
		return output;
	}

	/**
	 * Fetches the blog from the running app and writes it to the output folder.
	 *
	 * @param app the running app's address, like {@code http://127.0.0.1:52345}
	 * @return how many files were written
	 * @throws ExportException when a page or a file does not answer 200, or the folder cannot be written
	 */
	public int run(URI app) {
		checkFolder();
		Map<String, byte[]> files = new LinkedHashMap<>();
		files.put(MARKER, "Written by Beansprout's static export. The next export empties this folder.\n"
			.getBytes(StandardCharsets.UTF_8));
		try (HttpClient http = HttpClient.newBuilder()
			.followRedirects(HttpClient.Redirect.NEVER)
			.connectTimeout(TIMEOUT)
			.build()) {
			new Crawl(http, app, files).run();
		}
		add(files, "404.html", notFound.get().getBytes(StandardCharsets.UTF_8), "the not-found page");
		add(files, "index.html", redirect().getBytes(StandardCharsets.UTF_8), "the page at the site's root");
		add(files, ".nojekyll", new byte[0], "the .nojekyll file");
		write(files);
		return files.size();
	}

	/** The page at the site's root: it sends the reader on to the blog. */
	private String redirect() {
		Links links = pages.links();
		String home = attribute(links.home());
		String title = text(pages.settings().title());
		return "<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n<meta charset=\"utf-8\">\n<title>" + title + "</title>\n"
				+ "<meta http-equiv=\"refresh\" content=\"0; url=" + home + "\">\n" + "<link rel=\"canonical\" href=\""
				+ attribute(origin() + links.home()) + "\">\n</head>\n<body>\n<p><a href=\"" + home + "\">" + title
				+ "</a></p>\n</body>\n</html>\n";
	}

	private String origin() {
		String text = site.toString();
		return text.endsWith("/") ? text.substring(0, text.length() - 1) : text;
	}

	/** Stops when the folder is a file, or holds files that no earlier export wrote. */
	private void checkFolder() {
		if (!Files.exists(output)) {
			return;
		}
		if (!Files.isDirectory(output)) {
			throw new ExportException("The export folder " + output
					+ " is a file. Set beansprout.export.output to a folder, or remove the file.");
		}
		boolean empty;
		try (Stream<Path> children = Files.list(output)) {
			empty = children.findAny().isEmpty();
		}
		catch (IOException ex) {
			throw new ExportException("Could not read the export folder " + output + " (" + describe(ex) + ")", ex);
		}
		if (!empty && !Files.exists(output.resolve(MARKER))) {
			throw new ExportException("The export folder " + output + " already has files, and no " + MARKER
					+ " file says an earlier export wrote them. Empty the folder, or set beansprout.export.output to another one.");
		}
	}

	private void write(Map<String, byte[]> files) {
		checkFolder();
		try {
			if (Files.exists(output)) {
				clear(output);
			}
			Files.createDirectories(output);
		}
		catch (IOException ex) {
			throw new ExportException("Could not write " + output + " (" + describe(ex) + ")", ex);
		}
		for (Map.Entry<String, byte[]> entry : files.entrySet()) {
			Path file = output.resolve(entry.getKey()).normalize();
			if (!file.startsWith(output) || file.equals(output)) {
				throw new ExportException("Could not write " + entry.getKey() + ": it would land outside " + output);
			}
			try {
				Path parent = file.getParent();
				if (parent != null) {
					Files.createDirectories(parent);
				}
				Files.write(file, entry.getValue());
			}
			catch (IOException ex) {
				throw new ExportException("Could not write " + file + " (" + describe(ex) + ")", ex);
			}
		}
	}

	/** Deletes everything in a folder, but not the folder. A link is deleted, not followed. */
	private static void clear(Path folder) throws IOException {
		Files.walkFileTree(folder, new SimpleFileVisitor<>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
				Files.delete(file);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult postVisitDirectory(Path dir, @Nullable IOException ex) throws IOException {
				if (ex != null) {
					throw ex;
				}
				if (!dir.equals(folder)) {
					Files.delete(dir);
				}
				return FileVisitResult.CONTINUE;
			}
		});
	}

	private static String describe(IOException ex) {
		String message = ex.getMessage();
		return message == null ? ex.getClass().getSimpleName() : ex.getClass().getSimpleName() + ": " + message;
	}

	private static void add(Map<String, byte[]> files, String name, byte[] bytes, String what) {
		if (files.putIfAbsent(name, bytes) != null) {
			throw new ExportException("Two things would be written to " + name + ", the second is " + what
					+ ". Rename one of them.");
		}
	}

	/**
	 * The file an address is written to: its path without the context path, decoded, with
	 * {@code .html} after a page.
	 */
	String fileFor(String address, boolean page) {
		String root = pages.links().root();
		String path = address;
		if (!root.isEmpty()) {
			if (!path.startsWith(root + "/")) {
				throw new ExportException(address + " is outside the app's context path " + root + ", so it has no file.");
			}
			path = path.substring(root.length());
		}
		if (!path.startsWith("/") || path.length() == 1) {
			throw new ExportException(address + " cannot be written as a file.");
		}
		List<String> parts = new ArrayList<>();
		for (String segment : path.substring(1).split("/", -1)) {
			String part = decode(segment, address);
			if (part.isEmpty() || part.equals(".") || part.equals("..") || part.indexOf('/') >= 0
					|| part.indexOf('\\') >= 0 || part.indexOf('\0') >= 0) {
				throw new ExportException(address + " cannot be written as a file.");
			}
			parts.add(part);
		}
		String name = String.join("/", parts);
		return page ? name + ".html" : name;
	}

	/** A path segment with its %XX escapes read as UTF-8, the way a static host reads it. */
	private static String decode(String segment, String address) {
		if (segment.indexOf('%') < 0) {
			return segment;
		}
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		int plain = 0;
		int i = segment.indexOf('%');
		while (i >= 0) {
			if (i + 2 >= segment.length() || Character.digit(segment.charAt(i + 1), 16) < 0
					|| Character.digit(segment.charAt(i + 2), 16) < 0) {
				throw new ExportException(address + " cannot be written as a file.");
			}
			bytes.writeBytes(segment.substring(plain, i).getBytes(StandardCharsets.UTF_8));
			bytes.write(Character.digit(segment.charAt(i + 1), 16) * 16 + Character.digit(segment.charAt(i + 2), 16));
			plain = i + 3;
			i = segment.indexOf('%', plain);
		}
		bytes.writeBytes(segment.substring(plain).getBytes(StandardCharsets.UTF_8));
		return bytes.toString(StandardCharsets.UTF_8);
	}

	private static String text(String value) {
		return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}

	private static String attribute(String value) {
		return text(value).replace("\"", "&quot;");
	}

	/** An attribute value the way a browser reads it, for the references themes and Markdown write. */
	private static String unescape(String value) {
		return value.replace("&quot;", "\"")
			.replace("&#39;", "'")
			.replace("&#x27;", "'")
			.replace("&lt;", "<")
			.replace("&gt;", ">")
			.replace("&amp;", "&");
	}

	/** One answer from the app. */
	private static final class Response {

		private final int status;

		private final String type;

		private final byte[] body;

		Response(int status, String type, byte[] body) {
			this.status = status;
			this.type = type.toLowerCase(Locale.ROOT);
			this.body = body;
		}

		boolean page() {
			return type.startsWith("text/html") || type.startsWith("application/xhtml+xml");
		}

		boolean stylesheet() {
			return type.startsWith("text/css");
		}

		String text() {
			return new String(body, StandardCharsets.UTF_8);
		}

	}

	/** A link still to follow, and where it was found. */
	private record Link(String address, String from) {
	}

	/** One pass over the running app. */
	private final class Crawl {

		private final HttpClient http;

		private final URI app;

		private final Map<String, byte[]> files;

		private final Set<String> pageAddresses = new LinkedHashSet<>();

		private final Set<String> seen = new HashSet<>();

		private final Deque<Link> queue = new ArrayDeque<>();

		Crawl(HttpClient http, URI app, Map<String, byte[]> files) {
			this.http = http;
			this.app = app;
			this.files = files;
		}

		void run() {
			for (PageUrl url : pages.urls()) {
				pageAddresses.add(url.path());
			}
			for (String address : pageAddresses) {
				seen.add(address);
				Response page = fetch(address);
				if (page.status != 200) {
					throw new ExportException(address + " answered " + page.status
							+ ". Every address in the sitemap must answer 200 for the export to write it.");
				}
				add(files, fileFor(address, true), page.body, address);
				followTags(page.text(), address);
			}
			Links links = pages.links();
			queue.add(new Link(links.feed(), "the export"));
			queue.add(new Link(links.sitemap(), "the export"));
			while (!queue.isEmpty()) {
				Link link = queue.removeFirst();
				if (!seen.add(link.address())) {
					continue;
				}
				if (neverFetched(link.address())) {
					LOG.warn("Beansprout's export left out " + link.address() + ", linked from " + link.from()
							+ ": it is the MCP server, and a static site has none.");
					continue;
				}
				Response response = fetch(link.address());
				if (response.status >= 300 && response.status < 400) {
					LOG.warn("Beansprout's export left out " + link.address() + ", linked from " + link.from()
							+ ": it answers " + response.status + ", a redirect, and a static host has none.");
					continue;
				}
				if (response.page()) {
					LOG.warn("Beansprout's export left out " + link.address() + ", linked from " + link.from()
							+ (response.status == 200 ? ": it is a page of the app, not of the blog."
									: ": it answers " + response.status + ". It may be a draft or a post dated later."));
					continue;
				}
				if (response.status != 200) {
					throw new ExportException(link.address() + " answered " + response.status + ". It is linked from "
							+ link.from() + ". Fix the link, or add the file it points to.");
				}
				add(files, fileFor(link.address(), false), response.body, link.address());
				if (response.stylesheet()) {
					String css = response.text();
					follow(CSS_URL, css, link.address());
					follow(CSS_IMPORT, css, link.address());
				}
			}
			// Crawlers read robots.txt at the root. With a context path the app serves none. A post
			// that links to it has had it fetched already.
			if (seen.add("/robots.txt")) {
				Response robots = fetch("/robots.txt");
				if (robots.status == 200) {
					add(files, "robots.txt", robots.body, "/robots.txt");
				}
			}
		}

		private boolean neverFetched(String address) {
			for (String skipped : neverFetched) {
				if (address.equals(skipped) || address.startsWith(skipped + "/")) {
					return true;
				}
			}
			return false;
		}

		/** Queues the {@code href} and {@code src} of every tag in a page. Comments are left out. */
		private void followTags(String html, String from) {
			Matcher tag = TAG.matcher(COMMENT.matcher(html).replaceAll(""));
			while (tag.find()) {
				Matcher attribute = ATTRIBUTE.matcher(tag.group());
				while (attribute.find()) {
					String name = attribute.group(1).toLowerCase(Locale.ROOT);
					if (!name.equals("href") && !name.equals("src")) {
						continue;
					}
					String value = attribute.group(2) != null ? attribute.group(2)
							: attribute.group(3) != null ? attribute.group(3) : attribute.group(4);
					if (value != null) {
						queue(value, from);
					}
				}
			}
		}

		private void queue(String raw, String from) {
			String address = inApp(unescape(raw).strip(), from);
			if (address != null && !seen.contains(address)) {
				queue.add(new Link(address, from));
			}
		}

		/** Queues every address in the app that a pattern finds in a stylesheet. */
		private void follow(Pattern pattern, String text, String from) {
			Matcher matcher = pattern.matcher(text);
			while (matcher.find()) {
				@Nullable String raw = null;
				for (int group = 1; group <= matcher.groupCount() && raw == null; group++) {
					raw = matcher.group(group);
				}
				if (raw != null) {
					queue(raw, from);
				}
			}
		}

		/** The path of a link that points into the app or at the published site, or null for any other. */
		private @Nullable String inApp(String raw, String from) {
			if (raw.isEmpty() || raw.startsWith("#")) {
				return null;
			}
			URI target;
			try {
				target = URI.create(app + from).resolve(browserSafe(raw));
			}
			catch (IllegalArgumentException ex) {
				throw new ExportException("'" + raw + "' in " + from + " is not an address the export can read. "
						+ "Fix the link.", ex);
			}
			String scheme = target.getScheme() == null ? "" : target.getScheme().toLowerCase(Locale.ROOT);
			if (NOT_FETCHED.contains(scheme) || !(scheme.equals("http") || scheme.equals("https"))) {
				return null;
			}
			if (!sameHost(target, app) && !sameHost(target, site)) {
				return null;
			}
			String path = target.getRawPath();
			return path == null || path.isEmpty() ? "/" : path;
		}

		private Response fetch(String address) {
			HttpRequest request = HttpRequest.newBuilder(URI.create(app + address)).timeout(TIMEOUT).GET().build();
			try {
				HttpResponse<byte[]> response = http.send(request, HttpResponse.BodyHandlers.ofByteArray());
				return new Response(response.statusCode(), response.headers().firstValue("Content-Type").orElse(""),
						response.body());
			}
			catch (IOException ex) {
				throw new ExportException("Could not fetch " + address + " from the app (" + describe(ex) + ")", ex);
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
				throw new ExportException("The export stopped while it fetched " + address, ex);
			}
		}

	}

	/**
	 * An address with the characters a URI cannot hold, like a space, written as %XX in UTF-8, the
	 * way a browser sends them. A % stays as it is.
	 */
	static String browserSafe(String raw) {
		// [ and ] are legal only around an IPv6 host, like http://[::1]/.
		String illegal = IPV6_HOST.matcher(raw).matches() ? "\"<>\\^`{|}" : "\"<>\\^`{|}[]";
		StringBuilder out = new StringBuilder(raw.length());
		raw.codePoints().forEach(c -> {
			if (c > 0x20 && c < 0x7F && illegal.indexOf(c) < 0) {
				out.appendCodePoint(c);
			}
			else {
				for (byte b : new String(Character.toChars(c)).getBytes(StandardCharsets.UTF_8)) {
					out.append('%').append(Character.forDigit((b >> 4) & 0xF, 16)).append(Character.forDigit(b & 0xF, 16));
				}
			}
		});
		return out.toString();
	}

	private static boolean sameHost(URI target, URI base) {
		String host = target.getHost();
		return host != null && base.getHost() != null && host.equalsIgnoreCase(base.getHost())
				&& port(target) == port(base);
	}

	private static int port(URI uri) {
		if (uri.getPort() != -1) {
			return uri.getPort();
		}
		return "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80;
	}

}
