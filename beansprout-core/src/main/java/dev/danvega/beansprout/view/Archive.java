package dev.danvega.beansprout.view;

import java.util.List;

/**
 * Every post, by year.
 *
 * @param total 22 posts
 * @param since the month of the first post: March 2024, or empty with no posts
 * @param years newest year first
 */
public record Archive(String total, String since, List<Year> years) {
}
