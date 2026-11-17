package dev.danvega.beansprout.autoconfigure;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import dev.danvega.beansprout.content.MarkdownFile;
import dev.danvega.beansprout.content.PostSource;
import dev.danvega.beansprout.web.BundleFiles;
import org.jspecify.annotations.Nullable;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.util.StringUtils;

/** Reads every {@code .md} file under a Spring resource location, such as {@code classpath:posts/}. */
final class ResourcePostSource implements PostSource, BundleFiles {

	private final ResourcePatternResolver resolver;

	private final String location;

	ResourcePostSource(ResourcePatternResolver resolver, String location) {
		this.resolver = resolver;
		this.location = location.endsWith("/") ? location : location + "/";
	}

	@Override
	public List<MarkdownFile> files() {
		try {
			Resource root = resolver.getResource(location);
			if (!root.exists()) {
				return List.of();
			}
			List<MarkdownFile> files = new ArrayList<>();
			for (Resource resource : resolver.getResources(location + "**/*.md")) {
				if (resource.isReadable() && !MarkdownFile.hidden(String.valueOf(resource.getFilename()))) {
					files.add(read(root, resource));
				}
			}
			return files;
		}
		catch (FileNotFoundException ex) {
			return List.of();
		}
		catch (IOException ex) {
			throw new UncheckedIOException("Could not read the posts at " + location, ex);
		}
	}

	@Override
	public boolean contains(String folder, String path) {
		return find(folder, path) != null;
	}

	/**
	 * A folder on disk, or an unpacked classpath, is searched by path, so a name with {@code #},
	 * {@code ?} or {@code %} is just a name, and a link that leads out of the folder is not
	 * followed. Files in a jar are looked up by name.
	 */
	@Override
	public @Nullable Resource find(String folder, String path) {
		try {
			Resource root = resolver.getResource(location);
			if (!root.exists()) {
				return null;
			}
			if (root.isFile()) {
				Path base = root.getFile().toPath().toRealPath();
				Path inside = base.resolve(folder).normalize();
				Path file = inside.resolve(path).normalize();
				if (!inside.startsWith(base) || !file.startsWith(inside) || !Files.isRegularFile(file)) {
					return null;
				}
				Path real = file.toRealPath();
				return real.startsWith(inside.toRealPath()) && Files.isReadable(real) ? new FileSystemResource(real) : null;
			}
			Resource resource = resolver.getResource(location + (folder.isEmpty() ? "" : folder + "/") + path);
			return resource.exists() && resource.isReadable() ? resource : null;
		}
		catch (IOException | InvalidPathException ex) {
			// A name this file system cannot hold, such as a * on Windows, is not a file here.
			return null;
		}
	}

	/** One file. A file that cannot be read, or is not UTF-8, is skipped with a reason, not thrown. */
	private static MarkdownFile read(Resource root, Resource resource) throws IOException {
		String path = relativePath(root, resource);
		try {
			return MarkdownFile.decode(path, resource.getContentAsByteArray());
		}
		catch (IOException ex) {
			return MarkdownFile.unreadable(path, "it could not be read: " + ex.getMessage());
		}
	}

	/**
	 * Where a file sits under the root, with {@code /} between folders and no percent-encoding.
	 * Files on disk are compared as absolute paths, so a relative location like
	 * {@code file:./posts/} keeps its subfolders. Files in a jar are compared by decoded URL.
	 */
	private static String relativePath(Resource root, Resource resource) throws IOException {
		if (root.isFile() && resource.isFile()) {
			Path rootPath = root.getFile().toPath().toAbsolutePath().normalize();
			Path filePath = resource.getFile().toPath().toAbsolutePath().normalize();
			if (filePath.startsWith(rootPath)) {
				return rootPath.relativize(filePath).toString().replace(filePath.getFileSystem().getSeparator(), "/");
			}
		}
		String rootUrl = decode(root.getURL().toString());
		String url = decode(resource.getURL().toString());
		if (url.startsWith(rootUrl)) {
			return url.substring(rootUrl.length());
		}
		return decode(String.valueOf(resource.getFilename()));
	}

	private static String decode(String text) {
		return StringUtils.uriDecode(text, StandardCharsets.UTF_8);
	}

}
