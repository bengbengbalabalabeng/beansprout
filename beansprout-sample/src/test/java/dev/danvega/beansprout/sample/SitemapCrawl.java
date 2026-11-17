package dev.danvega.beansprout.sample;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Crawls a running blog the way a reader or a static export would: from the sitemap. Every URL
 * in the sitemap must answer 200. Every {@code href} and {@code src} on those pages that points
 * into the app must resolve. Links to other hosts are counted, not fetched.
 */
final class SitemapCrawl {

	private static final Pattern LOC = Pattern.compile("<loc>([^<]*)</loc>");

	private static final Pattern LINK = Pattern.compile("\\s(?:href|src)\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)')");

	private static final Set<String> NOT_FETCHED = Set.of("mailto", "tel", "javascript", "data");

	/** A status for a link that is not a valid address at all. */
	static final int INVALID = 0;

	/** One page from the sitemap. */
	record Page(String path, int status, String contentType, int links) {
	}

	/** A link on a page whose target did not answer with a 2xx status. */
	record Failure(String page, String link, int status) {
	}

	/** What one crawl found. {@code targets} maps every internal link target to its status. */
	record Result(String base, String sitemapPath, int sitemapStatus, List<Page> pages, Map<String, Integer> targets,
			Map<String, Set<String>> linkedFrom, List<Failure> invalid, int external, int notFoundStatus,
			String notFoundPath) {

		List<Failure> failures() {
			List<Failure> failures = new ArrayList<>(invalid);
			targets.forEach((target, status) -> {
				if (status < 200 || status > 299) {
					for (String page : linkedFrom.getOrDefault(target, Set.of())) {
						failures.add(new Failure(page, target, status));
					}
				}
			});
			return failures;
		}

		Set<String> sitemapPaths() {
			Set<String> paths = new LinkedHashSet<>();
			pages.forEach(page -> paths.add(page.path()));
			return paths;
		}

		/** Targets a reader can reach that the sitemap, and so {@code Pages.urls()}, does not name. */
		Set<String> notInSitemap() {
			Set<String> missing = new LinkedHashSet<>(targets.keySet());
			missing.removeAll(sitemapPaths());
			return missing;
		}

		boolean passed() {
			return sitemapStatus == 200 && !pages.isEmpty()
					&& pages.stream().allMatch(page -> page.status() == 200) && failures().isEmpty();
		}

	}

	private final HttpClient http = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();

	private final URI base;

	SitemapCrawl(String base) {
		this.base = URI.create(base);
	}

	Result crawl(String sitemapPath) throws IOException, InterruptedException {
		HttpResponse<String> sitemap = get(sitemapPath);
		List<Page> pages = new ArrayList<>();
		Map<String, Integer> targets = new LinkedHashMap<>();
		Map<String, Set<String>> linkedFrom = new LinkedHashMap<>();
		List<Failure> invalid = new ArrayList<>();
		int external = 0;
		for (String loc : locs(sitemap.body())) {
			String path = pathOf(URI.create(loc));
			HttpResponse<String> page = get(path);
			Set<String> links = new LinkedHashSet<>();
			Matcher matcher = LINK.matcher(page.body());
			while (matcher.find()) {
				String raw = unescape(matcher.group(1) != null ? matcher.group(1) : matcher.group(2));
				if (raw.isEmpty() || raw.startsWith("#")) {
					continue;
				}
				URI target;
				try {
					target = URI.create(base + path).resolve(raw);
				}
				catch (IllegalArgumentException ex) {
					invalid.add(new Failure(path, raw, INVALID));
					continue;
				}
				String scheme = target.getScheme() == null ? "" : target.getScheme();
				if (NOT_FETCHED.contains(scheme)) {
					continue;
				}
				if (!sameApp(target)) {
					external++;
					continue;
				}
				links.add(pathOf(target));
			}
			for (String link : links) {
				linkedFrom.computeIfAbsent(link, key -> new LinkedHashSet<>()).add(path);
				if (!targets.containsKey(link)) {
					targets.put(link, get(link).statusCode());
				}
			}
			pages.add(new Page(path, page.statusCode(), page.headers().firstValue("Content-Type").orElse(""), links.size()));
		}
		// The blog's own not-found page: an address under the blog that no post has.
		String notFoundPath = sitemapPath.substring(0, sitemapPath.lastIndexOf('/')) + "/crawl-check-no-such-page";
		int notFound = get(notFoundPath).statusCode();
		return new Result(base.toString(), sitemapPath, sitemap.statusCode(), pages, targets, linkedFrom, invalid, external,
				notFound, notFoundPath);
	}

	private boolean sameApp(URI target) {
		return ("http".equals(target.getScheme()) || "https".equals(target.getScheme()))
				&& base.getHost().equalsIgnoreCase(target.getHost()) && port(base) == port(target);
	}

	private static int port(URI uri) {
		if (uri.getPort() != -1) {
			return uri.getPort();
		}
		return "https".equals(uri.getScheme()) ? 443 : 80;
	}

	/** The path and query of an address, without its fragment. */
	private static String pathOf(URI uri) {
		String path = uri.getRawPath() == null || uri.getRawPath().isEmpty() ? "/" : uri.getRawPath();
		return uri.getRawQuery() == null ? path : path + "?" + uri.getRawQuery();
	}

	private HttpResponse<String> get(String path) throws IOException, InterruptedException {
		return http.send(HttpRequest.newBuilder(base.resolve(path)).build(), HttpResponse.BodyHandlers.ofString());
	}

	private static List<String> locs(String sitemap) {
		List<String> locs = new ArrayList<>();
		Matcher matcher = LOC.matcher(sitemap);
		while (matcher.find()) {
			locs.add(unescape(matcher.group(1).strip()));
		}
		return locs;
	}

	private static String unescape(String text) {
		return text.replace("&lt;", "<")
			.replace("&gt;", ">")
			.replace("&quot;", "\"")
			.replace("&#39;", "'")
			.replace("&apos;", "'")
			.replace("&amp;", "&");
	}

}
