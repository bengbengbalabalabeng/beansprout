package dev.danvega.beansprout.view;

/**
 * One tag and its posts. Template: {@code tag}.
 *
 * @param count 8 posts
 * @param feed the address of the tag's own Atom feed, like {@code /blog/tags/java/feed.xml}
 */
public record TagPage(Site site, Meta meta, String name, String count, Listing listing, String feed) {
}
