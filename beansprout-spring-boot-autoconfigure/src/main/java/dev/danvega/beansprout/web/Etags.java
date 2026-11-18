package dev.danvega.beansprout.web;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.CacheControl;
import org.springframework.web.context.request.ServletWebRequest;

/**
 * Weak ETags for the blog's responses. Each one is a hash of what the response is built from,
 * plus a value picked when the app starts. A restart may bring a new theme, new templates or new
 * code, so it changes every ETag.
 */
final class Etags {

	/** Readers and crawlers may keep a response, but ask again before they use it. */
	static final CacheControl NO_CACHE = CacheControl.noCache();

	private final byte[] startup = new byte[16];

	Etags() {
		new SecureRandom().nextBytes(startup);
	}

	/** The ETag for a response built from this content, like {@code W/"3f2a..."}. */
	String of(String content) {
		MessageDigest sha;
		try {
			sha = MessageDigest.getInstance("SHA-256");
		}
		catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException("Every Java runtime has SHA-256", ex);
		}
		sha.update(startup);
		byte[] hash = sha.digest(content.getBytes(StandardCharsets.UTF_8));
		return "W/\"" + HexFormat.of().formatHex(hash, 0, 16) + "\"";
	}

	/** True when the request's {@code If-None-Match} holds this ETag, so a 304 will do. */
	static boolean notModified(HttpServletRequest request, String etag) {
		return new ServletWebRequest(request).checkNotModified(etag);
	}

}
