package dev.danvega.beansprout.view;

import java.util.List;

/**
 * What a reader sees at an address with no post, tag or page. Template: {@code not-found}.
 *
 * @param path the address nothing lives at
 * @param postCount 22 posts
 * @param recent the newest posts
 */
public record NotFoundPage(Site site, String path, String postCount, List<MiniPost> recent) {
}
