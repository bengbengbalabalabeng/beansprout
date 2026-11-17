package dev.danvega.beansprout.web;

import org.jspecify.annotations.Nullable;

import org.springframework.core.io.Resource;

/** Finds a file in a post's folder, for {@link BundleImageController}. */
@FunctionalInterface
public interface BundleFiles {

	/**
	 * A readable file in a bundle's folder, or null when there is none. A file that is really
	 * somewhere else, such as a link out of the folder, is not found.
	 *
	 * @param folder the bundle's folder under the posts location, like {@code 2026/welcome}
	 * @param path the file's path in that folder, already checked so it cannot leave it
	 */
	@Nullable Resource find(String folder, String path);

}
