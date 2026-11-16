package dev.danvega.beansprout.web;

import dev.danvega.beansprout.feed.Feeds;
import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.Nullable;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/** The Atom feed and the sitemap, under {@code beansprout.base-path}. */
@Controller
@RequestMapping("${beansprout.base-path:/blog}")
public class FeedController {

	private static final MediaType ATOM = MediaType.parseMediaType("application/atom+xml;charset=UTF-8");

	private static final MediaType XML = MediaType.parseMediaType("application/xml;charset=UTF-8");

	private final Feeds feeds;

	private final @Nullable String siteUrl;

	/**
	 * Creates the controller.
	 *
	 * @param siteUrl {@code beansprout.site-url}, or null to use the request's scheme, host and port
	 */
	public FeedController(Feeds feeds, @Nullable String siteUrl) {
		this.feeds = feeds;
		this.siteUrl = siteUrl;
	}

	@GetMapping("/feed.xml")
	public ResponseEntity<String> feed(HttpServletRequest request) {
		return ResponseEntity.ok().contentType(ATOM).body(feeds.atom(site(request)));
	}

	@GetMapping("/sitemap.xml")
	public ResponseEntity<String> sitemap(HttpServletRequest request) {
		return ResponseEntity.ok().contentType(XML).body(feeds.sitemap(site(request)));
	}

	private String site(HttpServletRequest request) {
		return SiteAddress.of(siteUrl, request);
	}

}
