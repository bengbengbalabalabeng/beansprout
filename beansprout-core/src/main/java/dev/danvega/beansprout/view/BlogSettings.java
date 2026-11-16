package dev.danvega.beansprout.view;

/**
 * What the app chose for its blog.
 *
 * @param title the blog's name
 * @param description a line under the name, or empty
 * @param pageSize posts per page, at least 1
 */
public record BlogSettings(String title, String description, int pageSize) {

	public BlogSettings {
		if (pageSize < 1) {
			throw new IllegalArgumentException("beansprout.page-size must be 1 or more, not " + pageSize);
		}
	}

}
