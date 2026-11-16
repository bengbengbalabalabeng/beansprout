package dev.danvega.beansprout.web;

import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.Nullable;

import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/** The site's address for absolute links: {@code beansprout.site-url}, or the request's own. */
final class SiteAddress {

	private SiteAddress() {
	}

	static String of(@Nullable String configured, HttpServletRequest request) {
		if (configured != null && !configured.isBlank()) {
			return configured;
		}
		return ServletUriComponentsBuilder.fromRequestUri(request).replacePath(null).replaceQuery(null).toUriString();
	}

}
