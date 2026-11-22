package dev.danvega.beansprout.web;

import java.io.IOException;
import java.util.Optional;
import java.util.function.Function;

import dev.danvega.beansprout.view.Aliases;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.UrlPathHelper;

/**
 * Where a request for a post's alias is sent. An alias at a path where the app has a static file
 * sends nowhere: the app's file keeps its path. {@link AliasHandlerMapping} asks for paths outside
 * the blog, and the blog's controllers ask for the paths under it they cannot answer.
 */
public final class AliasRedirects {

	private final Aliases aliases;

	private final Function<String, Optional<Resource>> staticFiles;

	/**
	 * Creates the redirects.
	 *
	 * @param staticFiles the app's static file at a path in the app, or empty when it has none there
	 */
	public AliasRedirects(Aliases aliases, Function<String, Optional<Resource>> staticFiles) {
		this.aliases = aliases;
		this.staticFiles = staticFiles;
	}

	/**
	 * The post's address when the request's path is a live alias and no static file of the app's
	 * is there, or empty. The query string is not kept: an alias is an old link, and the post's
	 * address is the new one.
	 */
	public Optional<String> target(HttpServletRequest request) {
		String path = path(request);
		Optional<String> target = aliases.target(path);
		return target.isPresent() && staticFiles.apply(path).isPresent() ? Optional.empty() : target;
	}

	/**
	 * The app's static file, when the request's path is a live alias and the app has a file there.
	 * Under the blog's path the blog's controllers match first, so they answer with the file
	 * themselves. Spring Boot's static files would answer anywhere else.
	 */
	public Optional<ResponseEntity<byte[]>> staticFile(HttpServletRequest request) {
		String path = path(request);
		if (aliases.target(path).isEmpty()) {
			return Optional.empty();
		}
		return staticFiles.apply(path).map(AliasRedirects::serve);
	}

	private static ResponseEntity<byte[]> serve(Resource file) {
		MediaType type = MediaTypeFactory.getMediaType(file).orElse(MediaType.APPLICATION_OCTET_STREAM);
		// Bytes, as for a post's images: every Spring MVC setup can write a byte array.
		try {
			return ResponseEntity.ok().contentType(type).lastModified(file.lastModified()).body(file.getContentAsByteArray());
		}
		catch (IOException ex) {
			// Gone since it was found, or not a file after all, such as a folder in a jar.
			return ResponseEntity.notFound().build();
		}
	}

	private static String path(HttpServletRequest request) {
		return UrlPathHelper.defaultInstance.getPathWithinApplication(request);
	}

}
