package dev.danvega.beansprout.view;

/**
 * One tag and its posts. Template: {@code tag}.
 *
 * @param count 8 posts
 */
public record TagPage(Site site, Meta meta, String name, String count, Listing listing) {
}
