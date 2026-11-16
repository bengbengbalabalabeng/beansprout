package dev.danvega.beansprout.view;

/**
 * A post as one line: on the not-found page.
 *
 * @param date 14 March 2026
 * @param dateTime 2026-03-14
 * @param reading 9 min read
 */
public record MiniPost(String title, String url, String date, String dateTime, String reading) {
}
