package dev.danvega.beansprout.sample;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Writes what a {@link SitemapCrawl} found as Markdown: a full report and a short summary for
 * the CI job summary. The files are evidence for a reader. The test decides pass or fail.
 */
final class CrawlReport {

	static final String REPORT = "crawl-report.md";

	static final String SUMMARY = "crawl-summary.md";

	private CrawlReport() {
	}

	static void write(SitemapCrawl.Result result, Path folder, String posts) throws IOException {
		Files.createDirectories(folder);
		Files.writeString(folder.resolve(REPORT), report(result, posts));
		Files.writeString(folder.resolve(SUMMARY), summary(result));
	}

	private static String verdict(SitemapCrawl.Result result) {
		return result.passed() ? "PASS" : "FAIL";
	}

	private static String summary(SitemapCrawl.Result result) {
		StringBuilder md = new StringBuilder();
		md.append("### Sitemap crawl: ").append(verdict(result)).append("\n\n");
		md.append("| Pages in the sitemap | Not 200 | Internal targets checked | Failures | Linked but not in the sitemap |\n");
		md.append("| --- | --- | --- | --- | --- |\n");
		long not200 = result.pages().stream().filter(page -> page.status() != 200).count();
		md.append("| ").append(result.pages().size()).append(" | ").append(not200).append(" | ")
			.append(result.targets().size()).append(" | ").append(result.failures().size()).append(" | ")
			.append(result.notInSitemap().size()).append(" |\n\n");
		List<SitemapCrawl.Failure> failures = result.failures();
		for (SitemapCrawl.Failure failure : failures.subList(0, Math.min(10, failures.size()))) {
			md.append("- `").append(failure.link()).append("` on `").append(failure.page()).append("`: ")
				.append(status(failure.status())).append('\n');
		}
		if (failures.size() > 10) {
			md.append("- and ").append(failures.size() - 10).append(" more\n");
		}
		md.append("\nThe full report is in the `crawl-evidence` artifact.\n");
		return md.toString();
	}

	private static String report(SitemapCrawl.Result result, String posts) {
		StringBuilder md = new StringBuilder();
		md.append("# Sitemap crawl: ").append(verdict(result)).append("\n\n");
		md.append("- Run: ").append(Instant.now().truncatedTo(ChronoUnit.SECONDS)).append('\n');
		md.append("- App: ").append(result.base()).append(" (the sample app on a random port)\n");
		md.append("- Posts: ").append(posts).append('\n');
		md.append("- Sitemap: `").append(result.sitemapPath()).append("` answered ").append(result.sitemapStatus())
			.append(" with ").append(result.pages().size()).append(" URLs\n");
		md.append("- Internal targets checked: ").append(result.targets().size())
			.append(". Links to other hosts, counted but not fetched: ").append(result.external()).append("\n\n");
		md.append("Each sitemap URL must answer 200. Each `href` and `src` on those pages that points into the app ")
			.append("must answer 2xx, after redirects.\n\n");

		md.append("## Pages\n\n| URL | Status | Content type | Links checked |\n| --- | --- | --- | --- |\n");
		for (SitemapCrawl.Page page : result.pages()) {
			md.append("| `").append(page.path()).append("` | ").append(page.status()).append(" | ").append(page.contentType())
				.append(" | ").append(page.links()).append(" |\n");
		}

		md.append("\n## Failures\n\n");
		List<SitemapCrawl.Failure> failures = result.failures();
		if (failures.isEmpty()) {
			md.append("None.\n");
		}
		else {
			md.append("| Page | Link | Status |\n| --- | --- | --- |\n");
			for (SitemapCrawl.Failure failure : failures) {
				md.append("| `").append(failure.page()).append("` | `").append(failure.link()).append("` | ")
					.append(status(failure.status())).append(" |\n");
			}
		}

		md.append("\n## Every internal target\n\n| Target | Status | Linked from | In the sitemap |\n| --- | --- | --- | --- |\n");
		Set<String> sitemap = result.sitemapPaths();
		for (Map.Entry<String, Integer> target : result.targets().entrySet()) {
			int pages = result.linkedFrom().getOrDefault(target.getKey(), Set.of()).size();
			md.append("| `").append(target.getKey()).append("` | ").append(status(target.getValue())).append(" | ")
				.append(pages).append(pages == 1 ? " page" : " pages").append(" | ")
				.append(sitemap.contains(target.getKey()) ? "yes" : "no").append(" |\n");
		}

		md.append("\n## Gaps a static export would hit\n\n");
		md.append("The sitemap is built from `Pages.urls()`, the one list of blog URLs. An export that writes only ")
			.append("those URLs would leave these out:\n\n");
		Set<String> missing = result.notInSitemap();
		if (missing.isEmpty()) {
			md.append("- Nothing linked from the pages is missing from the sitemap.\n");
		}
		for (String target : missing) {
			md.append("- `").append(target).append("`: ").append(kind(target, result.sitemapPath())).append('\n');
		}
		md.append("- `").append(result.sitemapPath()).append("`: the sitemap itself is not in `urls()`.\n");
		md.append("- `").append(result.notFoundPath()).append("` answered ").append(result.notFoundStatus())
			.append(". The not-found page is not in `urls()`. A static host needs it as a file, like `404.html`.\n");

		md.append("\n## When static export lands\n\n");
		md.append("Static export is danvega/beansprout#1 and needs a spec first. When it lands, this crawl gets one more ")
			.append("assertion: for every URL in the Pages table and every target above, the exported file exists and its ")
			.append("bytes match the live response. The gaps above are the files most likely to be missing.\n");
		return md.toString();
	}

	private static String kind(String target, String sitemapPath) {
		String blog = sitemapPath.substring(0, sitemapPath.lastIndexOf('/') + 1);
		String path = target.contains("?") ? target.substring(0, target.indexOf('?')) : target;
		if (path.endsWith(".css") || path.endsWith(".js")) {
			return "a stylesheet or script from a jar";
		}
		if (path.endsWith(".xml")) {
			return "a feed";
		}
		if (path.matches(".*\\.(png|jpe?g|gif|svg|webp|avif|ico)")) {
			return target.startsWith(blog) ? "a picture in a post bundle" : "a picture from the app's own static files";
		}
		return "a page the sitemap does not list";
	}

	private static String status(int status) {
		return status == SitemapCrawl.INVALID ? "not a valid address" : Integer.toString(status);
	}

}
