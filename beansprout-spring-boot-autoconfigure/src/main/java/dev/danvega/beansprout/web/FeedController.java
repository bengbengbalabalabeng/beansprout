package dev.danvega.beansprout.web;

import java.util.Optional;

import dev.danvega.beansprout.feed.Feeds;
import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.Nullable;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

/** The Atom feeds, the blog's and each tag's, and the sitemap, under {@code beansprout.base-path}. */
@Controller
@RequestMapping("${beansprout.base-path:/blog}")
public class FeedController {

	private static final MediaType ATOM = MediaType.parseMediaType("application/atom+xml;charset=UTF-8");

	private static final MediaType XML = MediaType.parseMediaType("application/xml;charset=UTF-8");

	private final Feeds feeds;

	private final @Nullable String siteUrl;

	private final @Nullable AliasRedirects aliases;

	private final Etags etags = new Etags();

	/**
	 * Creates the controller.
	 *
	 * @param siteUrl {@code beansprout.site-url}, or null to use the request's scheme, host and port
	 */
	public FeedController(Feeds feeds, @Nullable String siteUrl) {
		this(feeds, siteUrl, null);
	}

	/**
	 * Creates the controller.
	 *
	 * @param siteUrl {@code beansprout.site-url}, or null to use the request's scheme, host and port
	 * @param aliases old paths under the blog that answer with a redirect, or the app's static file there,
	 *     instead of a 404, or null for none
	 */
	public FeedController(Feeds feeds, @Nullable String siteUrl, @Nullable AliasRedirects aliases) {
		this.feeds = feeds;
		this.siteUrl = siteUrl;
		this.aliases = aliases;
	}

	@GetMapping("/feed.xml")
	public ResponseEntity<String> feed(HttpServletRequest request) {
		return respond(feeds.atom(site(request)), ATOM, request);
	}

	/**
	 * A tag's own feed. When no published post has the tag, a post's alias at this path answers
	 * with a 301, or the app's static file there answers, and otherwise a 404.
	 */
	@GetMapping("/tags/{tag}/feed.xml")
	public ResponseEntity<?> tagFeed(@PathVariable String tag, HttpServletRequest request) {
		Optional<String> body = feeds.tagAtom(tag, site(request));
		if (body.isPresent()) {
			return respond(body.get(), ATOM, request);
		}
		if (aliases != null) {
			Optional<String> moved = aliases.target(request);
			if (moved.isPresent()) {
				return ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY).header(HttpHeaders.LOCATION, moved.get()).build();
			}
			Optional<ResponseEntity<byte[]>> file = aliases.staticFile(request);
			if (file.isPresent()) {
				return file.get();
			}
		}
		return ResponseEntity.notFound().build();
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
