package dev.danvega.beansprout.view;

import java.util.List;

/**
 * What a page tells search engines and social sites, for its {@code <head>}. Addresses are
 * absolute. Empty means "leave the tag out".
 *
 * @param title the page's own title, without the blog's name
 * @param description one or two sentences for search results and link previews
 * @param canonical the page's own absolute address, or empty on the not-found page
 * @param type {@code article} on a post, {@code website} elsewhere
 * @param siteName the blog's name
 * @param image a picture for link previews: the post's picture, or the writer's photo, or empty
 * @param imageAlt the picture's alt text, or empty
 * @param twitterCard {@code summary_large_image} when a post has a picture, else {@code summary}
 * @param publishedTime a post's date, like 2026-03-14T09:30:00Z, or empty on other pages
 * @param modifiedTime a post's last change, or empty on other pages
 * @param tags a post's tags, or none
 * @param jsonLd a post's BlogPosting structured data as JSON, safe to put in a script element, or
 *     empty. The only value a theme writes without escaping, besides a post's body.
 */
public record Meta(
		String title,
		String description,
		String canonical,
		String type,
		String siteName,
		String image,
		String imageAlt,
		String twitterCard,
		String publishedTime,
		String modifiedTime,
		List<String> tags,
		String jsonLd) {
}
