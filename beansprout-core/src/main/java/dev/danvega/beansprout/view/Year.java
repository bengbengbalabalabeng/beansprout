package dev.danvega.beansprout.view;

import java.util.List;

/**
 * One year of the archive.
 *
 * @param count 7 posts
 * @param posts newest first
 */
public record Year(String year, String count, List<ArchiveRow> posts) {
}
