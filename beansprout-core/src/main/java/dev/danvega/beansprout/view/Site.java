package dev.danvega.beansprout.view;

import java.util.List;

/**
 * What every page shows around its content: the blog's name, the menu and the tags.
 *
 * @param name the blog's name, from {@code beansprout.title}
 * @param description a line under the name, from {@code beansprout.description}, or empty
 * @param year this year, for the footer
 * @param home the index's address
 * @param archive the archive's address
 * @param feed the Atom feed's address
 * @param root the app's context path, empty at the root, for a theme's own files
 * @param photo the writer's picture, or empty
 * @param section the menu item this page belongs to: {@code latest}, {@code archive}, or empty
 * @param tags every tag with its count, most used first
 * @param links links the app adds to the main menu, after the theme's own items, or none. A path
 *     already has the context path in front.
 */
public record Site(
		String name,
		String description,
		String year,
		String home,
		String archive,
		String feed,
		String root,
		String photo,
		String section,
		List<TagCount> tags,
		List<NavLink> links) {
}
