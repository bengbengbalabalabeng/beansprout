package dev.danvega.beansprout.content;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * Where posts come from. {@link DirectoryPostSource} reads a folder on disk. The Spring Boot
 * auto-configuration adds one that reads any Spring resource location, such as the classpath.
 */
@FunctionalInterface
public interface PostSource {

	/** Every Markdown file, read fresh. A location that does not exist has no files. */
	List<MarkdownFile> files();

	/**
	 * True when a file is really in a post's folder, such as its picture. A link that leads out of
	 * the folder does not count. A source that cannot tell says no, so a post that needs the file
	 * is skipped.
	 *
	 * @param folder the post's folder under the posts location, like {@code 2026/welcome}
	 * @param path the file's path in that folder, already checked by {@link Bundles#relative(String)}
	 *     so it cannot leave it
	 */
	default boolean contains(String folder, String path) {
		return false;
	}

	/**
	 * Opens a file in a post's folder, such as a picture, so its size can be read from its header.
	 * The same file as {@link #contains(String, String)}: a link that leads out of the folder is not
	 * followed. The caller closes the stream. A source that cannot open files gives null, so its
	 * pictures get no size.
	 *
	 * @param folder the post's folder under the posts location, like {@code 2026/welcome}
	 * @param path the file's path in that folder, already checked by {@link Bundles#relative(String)}
	 * @return the file's bytes, or null when it is not there
	 * @throws IOException when the file is there but cannot be opened
	 */
	default @Nullable InputStream open(String folder, String path) throws IOException {
		return null;
	}

}
