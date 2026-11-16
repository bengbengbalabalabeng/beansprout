package dev.danvega.beansprout.content;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

/** Reads every {@code .md} file in a folder and its subfolders. */
public final class DirectoryPostSource implements PostSource {

	private final Path root;

	public DirectoryPostSource(Path root) {
		this.root = root;
	}

	@Override
	public List<MarkdownFile> files() {
		if (!Files.isDirectory(root)) {
			return List.of();
		}
		try (Stream<Path> paths = Files.walk(root)) {
			return paths.filter(Files::isRegularFile)
				.filter(path -> path.getFileName().toString().endsWith(".md"))
				.filter(path -> !MarkdownFile.hidden(path.getFileName().toString()))
				.sorted()
				.map(this::read)
				.toList();
		}
		catch (IOException ex) {
			throw new UncheckedIOException("Could not list the posts in " + root, ex);
		}
	}

	private MarkdownFile read(Path file) {
		String relative = root.relativize(file).toString().replace(file.getFileSystem().getSeparator(), "/");
		try {
			return MarkdownFile.decode(relative, Files.readAllBytes(file));
		}
		catch (IOException ex) {
			return MarkdownFile.unreadable(relative, "it could not be read: " + ex.getMessage());
		}
	}

}
