package dev.danvega.beansprout.view;

import dev.danvega.beansprout.content.Pictures;

/**
 * What the app chose for its blog.
 *
 * @param title the blog's name
 * @param description a line under the name, or empty
 * @param pageSize posts per page, at least 1
 * @param author the writer's name for post pages, or empty
 * @param photo the writer's picture for every page, or empty. A path starting with {@code /} or
 *     a full http or https address.
 */
public record BlogSettings(String title, String description, int pageSize, String author, String photo) {

	public BlogSettings {
		if (pageSize < 1) {
			throw new IllegalArgumentException("beansprout.page-size must be 1 or more, not " + pageSize);
		}
		if (!photo.isEmpty() && !Pictures.valid(photo)) {
			throw new IllegalArgumentException(
					"beansprout.photo must be a path starting with / or an http or https address, not '" + photo + "'");
		}
	}

	/** Settings with no author and no photo. */
	public BlogSettings(String title, String description, int pageSize) {
		this(title, description, pageSize, "", "");
	}

}
