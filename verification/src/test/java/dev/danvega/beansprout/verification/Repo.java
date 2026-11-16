package dev.danvega.beansprout.verification;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.MalformedInputException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Reads a Beansprout checkout from disk. The judges below take a root folder, so the same code
 * runs against the real repo and against the small fixture repos that prove each judge can fail.
 */
final class Repo {

	/** A requirement ID such as {@code POSTS-1}: capitals, a dash, a number. */
	static final Pattern REQUIREMENT_ID = Pattern.compile("\\b([A-Z][A-Z0-9]+)-(\\d+)\\b");

	private static final Pattern SPEC_ROW = Pattern.compile("^\\|\\s*([A-Z][A-Z0-9]+-\\d+)\\s*\\|");

	private static final Pattern DISPLAY_NAME = Pattern.compile("@DisplayName\\(\\s*\"((?:[^\"\\\\]|\\\\.)*)\"\\s*\\)");

	private static final Pattern MARKER = Pattern.compile("\\b(TODO|FIXME|XXX)\\b");

	private final Path root;

	Repo(Path root) {
		this.root = root;
	}

	/** The checkout Maven is building. Set by the verification module's pom. */
	static Repo current() {
		String root = System.getProperty("beansprout.root");
		return new Repo(root != null ? Path.of(root) : Path.of("..").toAbsolutePath().normalize());
	}

	/** Requirement ID to the spec file that defines it, in file order. Reports an ID defined twice. */
	Map<String, String> requirements(List<String> problems) {
		Map<String, String> ids = new LinkedHashMap<>();
		for (Path spec : files(root.resolve("specs"), 1)) {
			String name = spec.getFileName().toString();
			if (!name.endsWith(".md") || name.equals("README.md")) {
				continue;
			}
			for (String line : lines(spec)) {
				Matcher row = SPEC_ROW.matcher(line);
				if (row.find()) {
					String previous = ids.putIfAbsent(row.group(1), name);
					if (previous != null) {
						problems.add(row.group(1) + " is defined in both " + previous + " and " + name);
					}
				}
			}
		}
		return ids;
	}

	/** Every requirement ID named in a test's {@code @DisplayName}, in any module, to where it was found. */
	Map<String, String> testedIds() {
		Map<String, String> found = new LinkedHashMap<>();
		for (Path module : modules()) {
			for (Path source : files(module.resolve("src/test/java"), Integer.MAX_VALUE)) {
				if (!source.toString().endsWith(".java")) {
					continue;
				}
				Matcher name = DISPLAY_NAME.matcher(String.join("\n", lines(source)));
				while (name.find()) {
					Matcher id = REQUIREMENT_ID.matcher(name.group(1));
					while (id.find()) {
						found.putIfAbsent(id.group(), root.relativize(source).toString());
					}
				}
			}
		}
		return found;
	}

	/** Every TODO, FIXME or XXX in shipped code: each module's {@code src/main}. */
	List<String> markers() {
		List<String> hits = new ArrayList<>();
		for (Path module : modules()) {
			for (Path file : files(module.resolve("src/main"), Integer.MAX_VALUE)) {
				List<String> lines = lines(file);
				for (int i = 0; i < lines.size(); i++) {
					if (MARKER.matcher(lines.get(i)).find()) {
						hits.add(root.relativize(file) + ":" + (i + 1) + ": " + lines.get(i).strip());
					}
				}
			}
		}
		return hits;
	}

	/** The prefixes in use, such as {@code POSTS}, so a stray "UTF-8" in a test name is not read as an ID. */
	static Set<String> prefixes(Set<String> ids) {
		Set<String> prefixes = new LinkedHashSet<>();
		for (String id : ids) {
			prefixes.add(id.substring(0, id.lastIndexOf('-')));
		}
		return prefixes;
	}

	private List<Path> modules() {
		return files(root, 1).stream().filter(dir -> Files.isRegularFile(dir.resolve("pom.xml"))).toList();
	}

	private static List<Path> files(Path dir, int depth) {
		if (!Files.isDirectory(dir)) {
			return List.of();
		}
		try (Stream<Path> paths = Files.walk(dir, depth)) {
			return paths.filter(path -> !path.equals(dir)).sorted().toList();
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	private static List<String> lines(Path file) {
		if (!Files.isRegularFile(file)) {
			return List.of();
		}
		try {
			return Files.readAllLines(file);
		}
		catch (MalformedInputException ex) {
			return List.of(); // an image or other binary file: nothing to read
		}
		catch (IOException ex) {
			throw new UncheckedIOException("Could not read " + file, ex);
		}
	}

}
