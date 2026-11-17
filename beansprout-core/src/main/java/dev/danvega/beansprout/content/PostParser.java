package dev.danvega.beansprout.content;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiPredicate;

import dev.danvega.beansprout.code.CodeHighlighter;
import dev.danvega.beansprout.code.PrismHighlighter;
import org.jspecify.annotations.Nullable;

/** Turns one {@link MarkdownFile} into a {@link Post}, or says why it cannot. */
public final class PostParser {

	static final int SUMMARY_LIMIT = 200;

	static final int WORDS_PER_MINUTE = 200;

	private final ZoneId zone;

	private final MarkdownRenderer markdown;

	/** The context path and the blog's path, like {@code /app/blog}, for images in a bundle's body. */
	private final String address;

	/** The blog's path, like {@code /blog}, for a bundle's picture. Pages add the context path. */
	private final String base;

	/**
	 * Creates a parser for one blog that highlights code with the built-in highlighter.
	 *
	 * @param zone the blog's time zone: a date with no time starts at midnight here
	 */
	public PostParser(ZoneId zone) {
		this(zone, new PrismHighlighter());
	}

	/**
	 * Creates a parser for one blog.
	 *
	 * @param zone the blog's time zone: a date with no time starts at midnight here
	 * @param highlighter highlights code fences, or {@link CodeHighlighter#NONE} for none
	 */
	public PostParser(ZoneId zone, CodeHighlighter highlighter) {
		this(zone, highlighter, "", "/blog");
	}

	/**
	 * Creates a parser for one blog that knows where the blog is served, so a post that is a folder
	 * can link the images next to it.
	 *
	 * @param zone the blog's time zone: a date with no time starts at midnight here
	 * @param highlighter highlights code fences, or {@link CodeHighlighter#NONE} for none
	 * @param root the app's context path: empty, or like {@code /app}
	 * @param base the blog's path in the app, like {@code /blog}
	 */
	public PostParser(ZoneId zone, CodeHighlighter highlighter, String root, String base) {
		this.zone = zone;
		this.markdown = new MarkdownRenderer(highlighter);
		this.base = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
		this.address = (root.endsWith("/") ? root.substring(0, root.length() - 1) : root) + this.base;
	}

	/**
	 * Reads one file.
	 *
	 * @throws InvalidPostException when the frontmatter is missing or broken, or has no title or
	 *     date the parser can read
	 */
	public Post parse(MarkdownFile file) {
		return parse(file, (folder, path) -> false);
	}

	/**
	 * Reads one file, checking a bundle's relative picture against the files next to it.
	 *
	 * @param exists says whether a file is really in a post's folder, given the folder and the path
	 * @throws InvalidPostException when the frontmatter is missing or broken, has no title or date
	 *     the parser can read, or names a picture that is not there
	 */
	public Post parse(MarkdownFile file, BiPredicate<String, String> exists) {
		Frontmatter frontmatter = Frontmatter.parse(file.text());

		String title = frontmatter.text("title");
		if (title == null || title.isBlank()) {
			throw new InvalidPostException("it has no title. Add a line like title: My first post to its frontmatter.");
		}
		String dateText = frontmatter.text("date");
		if (dateText == null) {
			throw new InvalidPostException("it has no date. Add a line like date: 2026-11-16 to its frontmatter.");
		}
		ZonedDateTime date = date("date", dateText);
		String updatedText = frontmatter.text("updated");
		ZonedDateTime modified = updatedText == null ? date : date("updated", updatedText);
		boolean draft = draft(frontmatter.raw("draft"));

		// A slug: line that is there but empty gives an empty slug, which Posts skips. Only a
		// missing slug: line falls back to the file name.
		Object chosen = frontmatter.raw("slug");
		String folder = Bundles.folder(file.path());
		String named = folder != null ? folder.substring(folder.lastIndexOf('/') + 1) : fileName(file.path());
		String slug = Slugs.slugify(chosen == null ? named : chosen instanceof String text ? text : "");

		String image = orEmpty(frontmatter.text("picture"));
		String inFolder = folder != null && !image.isEmpty() ? Bundles.relative(image) : null;
		if (folder != null && inFolder != null) {
			if (!exists.test(folder, inFolder)) {
				throw new InvalidPostException("its picture '" + image + "' is not in its folder " + folder
						+ ". Put the file there, or fix the name.");
			}
			image = base + "/" + slug + "/" + Bundles.encode(inFolder);
		}
		else if (!image.isEmpty() && !Pictures.valid(image)) {
			throw new InvalidPostException("its picture '" + image + "' is not a path starting with / or an http or https address.");
		}

		String images = address + "/" + slug + "/";
		MarkdownRenderer.Rendered rendered = markdown.render(frontmatter.body(), folder == null ? null : images);
		String summary = frontmatter.text("summary");
		String lede = summary != null ? summary.strip() : "";

		return new Post(slug, title.strip(), date, tags(frontmatter.list("tags")),
				!lede.isEmpty() ? lede : cut(rendered.firstParagraph()), rendered.html(),
				rendered.headings(), Math.max(1, (rendered.words() + WORDS_PER_MINUTE - 1) / WORDS_PER_MINUTE), draft,
				file.path(), image, orEmpty(frontmatter.text("picture-alt")), orEmpty(frontmatter.text("caption")), lede,
				modified);
	}

	private ZonedDateTime date(String key, String text) {
		String value = text.strip();
		try {
			return OffsetDateTime.parse(value).atZoneSameInstant(zone);
		}
		catch (DateTimeParseException ignored) {
			// not a date with an offset; try the next shape
		}
		try {
			return LocalDateTime.parse(value.replace(' ', 'T')).atZone(zone);
		}
		catch (DateTimeParseException ignored) {
			// not a date and time; try a plain date
		}
		try {
			return LocalDate.parse(value).atStartOfDay(zone);
		}
		catch (DateTimeParseException ex) {
			throw new InvalidPostException("its " + key + " '" + value + "' is not a date. Write it like 2026-11-16 or 2026-11-16T09:30.");
		}
	}

	/** Missing means not a draft. Otherwise only the words true and false are read. */
	private static boolean draft(@Nullable Object value) {
		if (value == null || "false".equals(value)) {
			return false;
		}
		if ("true".equals(value)) {
			return true;
		}
		String shown = value instanceof List<?> list && list.isEmpty() ? "" : String.valueOf(value);
		throw new InvalidPostException("its draft value '" + shown + "' is not true or false.");
	}

	private static List<String> tags(List<String> written) {
		Set<String> seen = new LinkedHashSet<>();
		List<String> tags = new ArrayList<>();
		for (String tag : written) {
			String name = tag.strip();
			String slug = Slugs.slugify(name);
			if (!slug.isEmpty() && seen.add(slug)) {
				tags.add(name);
			}
		}
		return tags;
	}

	private static String orEmpty(@Nullable String value) {
		return value == null ? "" : value.strip();
	}

	/** The last part of a path, without {@code .md}. */
	private static String fileName(String path) {
		String name = path.substring(path.lastIndexOf('/') + 1);
		return name.endsWith(".md") ? name.substring(0, name.length() - 3) : name;
	}

	/** At most {@value #SUMMARY_LIMIT} characters, cut at a word, with an ellipsis when cut. */
	static String cut(String text) {
		if (text.length() <= SUMMARY_LIMIT) {
			return text;
		}
		int end = text.lastIndexOf(' ', SUMMARY_LIMIT - 1);
		if (end <= 0) {
			end = SUMMARY_LIMIT - 1;
			if (Character.isHighSurrogate(text.charAt(end - 1))) {
				end--;
			}
		}
		String start = text.substring(0, end);
		return start.stripTrailing() + "…";
	}

}
