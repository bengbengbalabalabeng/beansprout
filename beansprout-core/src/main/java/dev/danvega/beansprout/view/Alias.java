package dev.danvega.beansprout.view;

/**
 * An old path that answers with a redirect to a post.
 *
 * @param path the path from the app's root, like {@code /2019/03/14/hello.html}, as the post's
 *     frontmatter wrote it, with any {@code %XX} escapes read. It never has the context path in it.
 * @param target the post's address, with the context path, like {@code /blog/hello}
 */
public record Alias(String path, String target) {
}
