package dev.danvega.beansprout.web;

import dev.danvega.beansprout.feed.Feeds;
import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.Nullable;

import org.springframework.http.HttpStatus;
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

	private final Etags etags = new Etags();

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
		return respond(feeds.atom(site(request)), ATOM, request);
	}

	@GetMapping("/sitemap.xml")
	public ResponseEntity<String> sitemap(HttpServletRequest request) {
		return respond(feeds.sitemap(site(request)), XML, request);
	}

	/** The body with its ETag, or a 304 when the request already has it. */
	private ResponseEntity<String> respond(String body, MediaType type, HttpServletRequest request) {
		String etag = etags.of(body);
		if (Etags.notModified(request, etag)) {
			return ResponseEntity.status(HttpStatus.NOT_MODIFIED).eTag(etag).cacheControl(Etags.NO_CACHE).build();
		}
		return ResponseEntity.ok().eTag(etag).cacheControl(Etags.NO_CACHE).contentType(type).body(body);
	}

	private String site(HttpServletRequest request) {
		return SiteAddress.of(siteUrl, request);
	}

}
