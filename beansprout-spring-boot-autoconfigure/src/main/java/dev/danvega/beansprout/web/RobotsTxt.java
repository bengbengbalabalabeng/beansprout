package dev.danvega.beansprout.web;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import dev.danvega.beansprout.view.Links;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.Nullable;

import org.springframework.http.HttpHeaders;
import org.springframework.web.HttpRequestHandler;

/**
 * The robots.txt at the site's root: every crawler may read every path, the sitemap is named, and
 * the AI training crawlers can be kept out of the blog.
 *
 * <p>It is a plain handler, not a {@code @Controller}, so an app that maps {@code /robots.txt}
 * itself still starts, and its own mapping answers.
 */
public final class RobotsTxt implements HttpRequestHandler {

	/** The crawlers that read pages to train AI models. Their search crawlers are not on it. */
	static final List<String> TRAINING_CRAWLERS = List.of("GPTBot", "ClaudeBot", "Google-Extended", "Applebot-Extended",
			"CCBot");

	private final Links links;

	private final String basePath;

	private final @Nullable String siteUrl;

	private final boolean blockTraining;

	private final Etags etags = new Etags();

	/**
	 * Creates the handler.
	 *
	 * @param basePath where the blog lives, like {@code /blog}
	 * @param siteUrl {@code beansprout.site-url}, or null to use the request's scheme, host and port
	 * @param blockTraining true to keep the AI training crawlers out of the blog
	 */
	public RobotsTxt(Links links, String basePath, @Nullable String siteUrl, boolean blockTraining) {
		this.links = links;
		this.basePath = basePath;
		this.siteUrl = siteUrl;
		this.blockTraining = blockTraining;
	}

	@Override
	public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws IOException {
		if (!request.getMethod().equals("GET") && !request.getMethod().equals("HEAD")) {
			response.setHeader(HttpHeaders.ALLOW, "GET, HEAD");
			response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
			return;
		}
		String body = body(SiteAddress.of(siteUrl, request));
		String etag = etags.of(body);
		response.setHeader(HttpHeaders.ETAG, etag);
		response.setHeader(HttpHeaders.CACHE_CONTROL, Etags.NO_CACHE.getHeaderValue());
		if (Etags.notModified(request, etag)) {
			response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
			return;
		}
		byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
		response.setContentType("text/plain;charset=UTF-8");
		response.setContentLength(bytes.length);
		response.getOutputStream().write(bytes);
	}

	private String body(String site) {
		StringBuilder out = new StringBuilder("User-agent: *\nAllow: /\n");
		if (blockTraining) {
			out.append('\n');
			for (String crawler : TRAINING_CRAWLERS) {
				out.append("User-agent: ").append(crawler).append('\n');
			}
			out.append("Disallow: ").append(basePath).append('\n');
		}
		String root = site.endsWith("/") ? site.substring(0, site.length() - 1) : site;
		return out.append("\nSitemap: ").append(root).append(links.sitemap()).append('\n').toString();
	}

}
