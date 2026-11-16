package dev.danvega.beansprout.view;

/**
 * One post as a row in the archive.
 *
 * @param date the day and month, under its year: 23 March
 * @param dateTime 2026-03-23, for the time element
 * @param reading 6 min
 */
public record ArchiveRow(String title, String url, String date, String dateTime, String reading) {
}
