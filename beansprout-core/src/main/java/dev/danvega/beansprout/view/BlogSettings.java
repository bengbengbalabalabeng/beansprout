package dev.danvega.beansprout.view;

import java.util.List;

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
 * @param links links the app adds to every theme's main menu, in order, or none
 * @param relatedPosts how many related posts a post page shows, 0 for none
 */
public record BlogSettings(String title, String description, int pageSize, String author, String photo,
		List<NavLink> links, int relatedPosts) {

	/** How many related posts a post page shows when the app does not say. */
	public static final int RELATED_POSTS = 3;

	public BlogSettings {
		if (pageSize < 1) {
			throw new IllegalArgumentException("beansprout.page-size must be 1 or more, not " + pageSize);
		}
		if (!photo.isEmpty() && !Pictures.valid(photo)) {
			throw new IllegalArgumentException(
					"beansprout.photo must be a path starting with / or an http or https address, not '" + photo + "'");
		}
		if (relatedPosts < 0) {
			throw new IllegalArgumentException("beansprout.related-posts must be 0 or more, not " + relatedPosts);
		}
		links = List.copyOf(links);
	}

	/** Settings with {@value #RELATED_POSTS} related posts. */
	public BlogSettings(String title, String description, int pageSize, String author, String photo,
			List<NavLink> links) {
		this(title, description, pageSize, author, photo, links, RELATED_POSTS);
	}

	/** Settings with no links in the menu. */
	public BlogSettings(String title, String description, int pageSize, String author, String photo) {
		this(title, description, pageSize, author, photo, List.of(), RELATED_POSTS);
	}

	/** Settings with no author and no photo. */
	public BlogSettings(String title, String description, int pageSize) {
		this(title, description, pageSize, "", "", List.of(), RELATED_POSTS);
	}

}
