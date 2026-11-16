package dev.danvega.beansprout.view;

/**
 * A tag with how many posts use it: in the sidebar and the menu.
 *
 * @param count how many published posts use it, as text
 * @param current true on the tag's own page
 */
public record TagCount(String name, String url, String count, boolean current) {
}
