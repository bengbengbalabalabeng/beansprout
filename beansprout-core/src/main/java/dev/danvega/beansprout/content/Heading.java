package dev.danvega.beansprout.content;

/**
 * A {@code ##} heading in a post's body.
 *
 * @param id the heading's id in the HTML, made from its text
 * @param text the heading as plain text
 */
public record Heading(String id, String text) {
}
