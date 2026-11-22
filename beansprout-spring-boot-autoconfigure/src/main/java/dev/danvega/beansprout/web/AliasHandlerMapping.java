package dev.danvega.beansprout.web;

import java.util.Optional;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.Nullable;

import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.servlet.handler.AbstractHandlerMapping;

/**
 * Answers a GET for a post's alias with a 301 to the post. It is ordered after the app's own
 * mappings and before its static files, like robots.txt, so the app's mapping or static file at
 * the same path answers instead. The aliases are read on every request, so a post that goes live
 * brings its aliases with it.
 *
 * <p>An alias under the blog's own path reaches the blog's controllers first. They ask the same
 * {@link AliasRedirects} before they answer 404.
 */
public final class AliasHandlerMapping extends AbstractHandlerMapping {

	private final AliasRedirects redirects;

	public AliasHandlerMapping(AliasRedirects redirects) {
		this.redirects = redirects;
		setOrder(Ordered.LOWEST_PRECEDENCE - 2);
	}

	@Override
	protected @Nullable Object getHandlerInternal(HttpServletRequest request) {
		if (!request.getMethod().equals("GET") && !request.getMethod().equals("HEAD")) {
			return null;
		}
		Optional<String> target = redirects.target(request);
		if (target.isEmpty()) {
			return null;
		}
		String location = target.get();
		return (HttpRequestHandler) (ignored, response) -> {
			response.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
			response.setHeader(HttpHeaders.LOCATION, location);
		};
	}

}
