package dev.danvega.beansprout.content;

/**
 * A tag and how many published posts use it.
 *
 * @param slug the tag's address part, which is also what makes two spellings one tag
 * @param name the tag as the newest post using it spells it
 * @param count how many published posts use it
 */
public record TagSummary(String slug, String name, int count) {
}
