package dev.danvega.beansprout.content;

import java.util.List;

/**
 * Where posts come from. {@link DirectoryPostSource} reads a folder on disk. The Spring Boot
 * auto-configuration adds one that reads any Spring resource location, such as the classpath.
 */
@FunctionalInterface
public interface PostSource {

	/** Every Markdown file, read fresh. A location that does not exist has no files. */
	List<MarkdownFile> files();

}
