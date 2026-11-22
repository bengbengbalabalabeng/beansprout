package dev.danvega.beansprout.view;

import java.util.List;

/**
 * One post on its own page.
 *
 * @param url the post's own address on the blog, like {@code /blog/hello}, even when its
 *     canonical link points to another site
 * @param lede the summary written in frontmatter, shown under the title, or empty
 * @param crumb the tag the breadcrumb leads back to: the post's first tag, or the index
 * @param date as the page shows it: 14 March 2026
 * @param dateTime the same date as 2026-03-14, for the time element
 * @param reading 9 min read
 * @param updated the day the post last changed, like 2 April 2026, when that is a later day than
 *     its date, or empty
 * @param updatedDateTime the same day as 2026-04-02, or empty
 * @param author the post's own {@code author}, or {@code beansprout.author} when it has none, or empty
 * @param picture the lead picture's address, or empty
 * @param pictureAlt the picture's alt text, or empty
 * @param pictureWidth the picture's width in pixels, like {@code 1200}, when it is in the post's
 *     folder and its size could be read, or empty
 * @param pictureHeight the picture's height in pixels, or empty, like {@code pictureWidth}
 * @param caption a line under the picture, or empty. Always empty when there is no picture.
 * @param body the post as HTML. The only value a theme shows without escaping.
 * @param headings the post's {@code ##} headings, for "On this page"
 * @param newer the next newer post, or {@link PostLink#NONE}
 * @param older the next older post, or {@link PostLink#NONE}
 */
public record Post(
		String title,
		String url,
		String lede,
		Tag crumb,
		String author,
		String date,
		String dateTime,
		String reading,
		String updated,
		String updatedDateTime,
		List<Tag> tags,
		String picture,
		String pictureAlt,
		String pictureWidth,
		String pictureHeight,
		String caption,
		String body,
		List<Heading> headings,
		PostLink newer,
		PostLink older) {
}
