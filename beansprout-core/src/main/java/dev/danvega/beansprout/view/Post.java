package dev.danvega.beansprout.view;

import java.util.List;

/**
 * One post on its own page.
 *
 * @param crumb the tag the breadcrumb leads back to: the post's first tag, or the index
 * @param date as the page shows it: 14 March 2026
 * @param dateTime the same date as 2026-03-14, for the time element
 * @param reading 9 min read
 * @param body the post as HTML. The only value a theme shows without escaping.
 * @param headings the post's {@code ##} headings, for "On this page"
 * @param newer the next newer post, or {@link PostLink#NONE}
 * @param older the next older post, or {@link PostLink#NONE}
 */
public record Post(
		String title,
		Tag crumb,
		String date,
		String dateTime,
		String reading,
		List<Tag> tags,
		String body,
		List<Heading> headings,
		PostLink newer,
		PostLink older) {
}
