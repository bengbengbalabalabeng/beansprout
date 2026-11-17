package dev.danvega.beansprout.mcp;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import dev.danvega.beansprout.code.CodeHighlighter;
import dev.danvega.beansprout.content.Bundles;
import dev.danvega.beansprout.content.DirectoryPostSource;
import dev.danvega.beansprout.content.InvalidPostException;
import dev.danvega.beansprout.content.MarkdownFile;
import dev.danvega.beansprout.content.PostParser;
import dev.danvega.beansprout.content.Posts;
import dev.danvega.beansprout.content.Slugs;
import org.jspecify.annotations.Nullable;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;

/**
 * The one thing an agent may write: a new draft. It is always {@code draft: true}, it lands in the
 * posts folder on disk under a name made from its title, and it never replaces a file. Publishing
 * stays with a person, who sets {@code draft: false}.
 */
public final class DraftTools {

	/** The longest address a draft gets. */
	static final int LONGEST = 80;

	private final Path folder;

	private final Posts posts;

	/**
	 * Creates the tool.
	 *
	 * @param folder the posts folder on disk
	 * @param posts the blog's posts, for today's date in the blog's time zone
	 */
	public DraftTools(Path folder, Posts posts) {
		this.folder = folder.toAbsolutePath().normalize();
		this.posts = posts;
	}

	@McpTool(name = "create_draft", description = "Start a new blog post as a draft. It is saved as a Markdown file"
			+ " with draft: true, so it stays hidden until a person publishes it. The file is named from the title."
			+ " It never replaces a post that is already there.")
	public Draft createDraft(@McpToolParam(description = "The post's title", required = true) String title,
			@McpToolParam(description = "The post's body in Markdown, without frontmatter", required = true) String markdown,
			@McpToolParam(description = "Tags, like java or Spring Boot", required = false) @Nullable List<String> tags,
			@McpToolParam(description = "One or two sentences for the post's summary", required = false) @Nullable String summary) {
		String slug = shorten(Slugs.slugify(title));
		if (slug.isEmpty()) {
			throw new IllegalArgumentException("The title needs a letter or a digit, so the draft can be named after it.");
		}
		if (Posts.RESERVED_SLUGS.contains(slug)) {
			throw new IllegalArgumentException("The address '" + slug + "' is taken by a blog page. Choose another title.");
		}
		String taken = takenBy(slug);
		if (taken != null) {
			throw new IllegalArgumentException("A post already has the address '" + slug + "': " + taken
					+ ". Nothing was written. Choose another title.");
		}
		String name = slug + ".md";
		Path file = folder.resolve(name).normalize();
		if (!folder.equals(file.getParent())) {
			throw new IllegalArgumentException("The draft can only go in the posts folder.");
		}
		List<String> kept = new ArrayList<>();
		List<String> skipped = new ArrayList<>();
		if (tags != null) {
			for (String tag : tags) {
				if (tag == null || tag.isBlank()) {
					continue;
				}
				(Slugs.slugify(tag).isEmpty() ? skipped : kept).add(tag.strip());
			}
		}
		try {
			Files.writeString(file, text(title, markdown, kept, summary), StandardCharsets.UTF_8,
					StandardOpenOption.CREATE_NEW);
		}
		catch (FileAlreadyExistsException ex) {
			throw new IllegalArgumentException("A post already has the address '" + slug + "': " + name
					+ ". Nothing was written. Choose another title.", ex);
		}
		catch (IOException ex) {
			throw new UncheckedIOException("Could not write the draft " + name, ex);
		}
		String note = "Saved as a draft. It stays hidden until someone sets draft: false in " + name + ".";
		if (!skipped.isEmpty()) {
			note += " Skipped these tags, since the blog cannot make an address from them: " + String.join(", ", skipped) + ".";
		}
		return new Draft(slug, name, note);
	}

	/** The file of any post with this slug, drafts and future posts included, or null. */
	private @Nullable String takenBy(String slug) {
		PostParser parser = new PostParser(ZoneOffset.UTC, CodeHighlighter.NONE);
		for (MarkdownFile file : new DirectoryPostSource(folder).files()) {
			String bundle = Bundles.folder(file.path());
			String named = bundle != null ? bundle.substring(bundle.lastIndexOf('/') + 1)
					: file.path().substring(file.path().lastIndexOf('/') + 1).replaceFirst("\\.md$", "");
			String own;
			try {
				own = parser.parse(file, (dir, path) -> true).slug();
			}
			catch (InvalidPostException ex) {
				own = Slugs.slugify(named);
			}
			if (own.equals(slug) || Slugs.slugify(named).equals(slug)) {
				return file.path();
			}
		}
		return null;
	}

	/** At most {@value #LONGEST} characters, cut at a dash, so the file name fits on any disk. */
	private static String shorten(String slug) {
		if (slug.length() <= LONGEST) {
			return slug;
		}
		String cut = slug.substring(0, LONGEST);
		int dash = cut.lastIndexOf('-');
		return dash > 0 ? cut.substring(0, dash) : cut;
	}

	private String text(String title, String markdown, List<String> tags, @Nullable String summary) {
		StringBuilder out = new StringBuilder("---\n");
		out.append("title: ").append(quote(title)).append('\n');
		out.append("date: ").append(LocalDate.now(posts.clock())).append('\n');
		if (!tags.isEmpty()) {
			out.append("tags: [").append(tags.stream().map(DraftTools::quote).collect(Collectors.joining(", "))).append("]\n");
		}
		if (summary != null && !summary.isBlank()) {
			out.append("summary: ").append(quote(summary)).append('\n');
		}
		out.append("draft: true\n---\n");
		out.append(markdown);
		if (!markdown.endsWith("\n")) {
			out.append('\n');
		}
		return out.toString();
	}

	/** A value in double quotes, one line, as Beansprout's frontmatter reads it back. */
	private static String quote(String value) {
		String line = value.strip().replace("\r\n", " ").replace('\n', ' ').replace('\r', ' ');
		return "\"" + line.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
	}

	/**
	 * A draft that was written.
	 *
	 * @param slug its address, under the blog's path
	 * @param file its file, in the posts folder
	 * @param note what happens next, for the person reading the agent's answer
	 */
	public record Draft(String slug, String file, String note) {
	}

}
