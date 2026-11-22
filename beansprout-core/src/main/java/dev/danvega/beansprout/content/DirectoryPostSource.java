package dev.danvega.beansprout.content;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;

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

	@Override
	public boolean contains(String folder, String path) {
		return find(folder, path) != null;
	}

	@Override
	public @Nullable InputStream open(String folder, String path) throws IOException {
		Path file = find(folder, path);
		return file == null ? null : Files.newInputStream(file);
	}

	/** The file, when it is really in the folder: a link that leads out of it does not count. */
	private @Nullable Path find(String folder, String path) {
		try {
			Path inside = root.toRealPath().resolve(folder).normalize();
			Path file = inside.resolve(path).normalize();
			if (!file.startsWith(inside) || !Files.isRegularFile(file)) {
				return null;
			}
			Path real = file.toRealPath();
			return real.startsWith(inside.toRealPath()) ? real : null;
		}
		catch (IOException | InvalidPathException ex) {
			return null;
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
