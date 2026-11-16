package dev.danvega.beansprout.view;

import java.util.List;

/**
 * A post as a card in a list.
 *
 * @param date as the page shows it: 14 March 2026
 * @param dateTime the same date as 2026-03-14, for the time element
 * @param reading 9 min read
 * @param excerpt the post's summary
 */
public record Card(
		String title,
		String url,
		String date,
		String dateTime,
		String reading,
		String excerpt,
		List<Tag> tags) {
}
