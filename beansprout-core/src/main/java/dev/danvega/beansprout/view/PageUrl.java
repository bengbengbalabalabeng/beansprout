package dev.danvega.beansprout.view;

/**
 * One address the blog serves, for the sitemap and for a static export.
 *
 * @param path the address, starting with the context path
 * @param lastModified the post's date as 2026-03-14, or empty for a list page
 */
public record PageUrl(String path, String lastModified) {
}
