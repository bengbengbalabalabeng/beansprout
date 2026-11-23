package dev.danvega.beansprout.content;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.jspecify.annotations.Nullable;

/**
 * The blog's posts. The one place that decides what is published.
 *
 * <p>Files are read once, or on every call when {@code reload} is on. Visibility is decided on
 * every call from the clock, so a post dated in the future appears at that moment with no reload.
 * An app with its own controllers can call this service directly.
 */
public final class Posts {

	/** Slugs that would clash with the blog's own routes. */
	public static final Set<String> RESERVED_SLUGS = Set.of("page", "tags", "archive");

	private static final Logger LOG = System.getLogger(Posts.class.getName());

	private static final Comparator<Post> NEWEST_FIRST = Comparator.comparing(Post::date).reversed()
		.thenComparing(Post::slug);

	private final PostSource source;

	private final PostParser parser;

	private final Clock clock;

	private final boolean reload;

	/**
	 * Taken while the files are read: once when posts are kept, or on each call when they are
	 * reloaded. Private, so no caller can hold it.
	 */
	private final Object reading = new Object();

	private volatile @Nullable Snapshot snapshot;

	public Posts(PostSource source, PostParser parser, Clock clock, boolean reload) {
		this.source = source;
		this.parser = parser;
		this.clock = clock;
		this.reload = reload;
	}

	/**
	 * Published posts, newest first: not drafts, not dated after now, and not unlisted. These are
	 * the posts the blog's lists, feeds and sitemap name.
	 */
	public List<Post> published() {
		return live().stream().filter(post -> !post.unlisted()).toList();
	}

	/**
	 * Live posts, newest first: not drafts and not dated after now. Unlisted posts are here: each
	 * has its page, though no list names it.
	 */
	public List<Post> live() {
		var now = clock.instant();
		return snapshot().posts().stream()
			.filter(post -> !post.draft())
			.filter(post -> !post.date().toInstant().isAfter(now))
			.toList();
	}

	/** A live post by its slug, unlisted ones included. Drafts and future posts are not found. */
	public Optional<Post> find(String slug) {
		return live().stream().filter(post -> post.slug().equals(slug)).findFirst();
	}

	/** Published posts with a tag, by the tag's slug, newest first. */
	public List<Post> tagged(String tagSlug) {
		return published().stream()
			.filter(post -> post.tags().stream().anyMatch(tag -> Slugs.slugify(tag).equals(tagSlug)))
			.toList();
	}

	/** Tags on published posts, most used first, then by name. */
	public List<TagSummary> tags() {
		Map<String, String> names = new LinkedHashMap<>();
		Map<String, Integer> counts = new HashMap<>();
		for (Post post : published()) {
			for (String tag : post.tags()) {
				String slug = Slugs.slugify(tag);
				names.putIfAbsent(slug, tag);
				counts.merge(slug, 1, Integer::sum);
			}
		}
		return names.entrySet().stream()
			.map(entry -> new TagSummary(entry.getKey(), entry.getValue(), counts.getOrDefault(entry.getKey(), 0)))
			.sorted(Comparator.comparingInt(TagSummary::count).reversed()
				.thenComparing(tag -> tag.name().toLowerCase(Locale.ROOT)))
			.toList();
	}

	/** One tag on published posts, by its slug. */
	public Optional<TagSummary> tag(String slug) {
		return tags().stream().filter(tag -> tag.slug().equals(slug)).findFirst();
	}

	/** Why each skipped file was skipped, one sentence per file, from the last read. */
	public List<String> problems() {
		return snapshot().problems();
	}

	/** The clock that decides what is published. */
	public Clock clock() {
		return clock;
	}

	private Snapshot snapshot() {
		if (reload) {
			synchronized (reading) {
				return read();
			}
		}
		Snapshot current = snapshot;
		if (current != null) {
			return current;
		}
		synchronized (reading) {
			current = snapshot;
			if (current == null) {
				current = read();
				snapshot = current;
			}
			return current;
		}
	}

	private Snapshot read() {
		List<MarkdownFile> files = new ArrayList<>(source.files());
		files.sort(Comparator.comparing(MarkdownFile::path));
		Map<String, Post> bySlug = new LinkedHashMap<>();
		List<String> problems = new ArrayList<>();
		// A folder with an index.md is one post. The other Markdown files inside it are not posts.
		List<String> bundles = new ArrayList<>();
		for (MarkdownFile file : files) {
			String folder = Bundles.folder(file.path());
			if (folder != null) {
				bundles.add(folder);
			}
		}
		for (MarkdownFile file : files) {
			if (Bundles.insideAnother(file.path(), bundles)) {
				continue;
			}
			if (file.unreadable() != null) {
				problems.add(problem(file.path(), file.unreadable()));
				continue;
			}
			try {
				Post post = parser.parse(file, source);
				if (post.slug().isEmpty()) {
					problems.add(problem(file.path(), "its slug is empty. Give it a slug: made of letters or digits."));
					continue;
				}
				if (RESERVED_SLUGS.contains(post.slug())) {
					problems.add(problem(file.path(), "its slug '" + post.slug()
							+ "' is taken by a blog page. Choose another slug: or rename the file."));
					continue;
				}
				Post first = bySlug.putIfAbsent(post.slug(), post);
				if (first != null) {
					problems.add(problem(file.path(), "it has the slug '" + post.slug() + "', like " + first.path()
							+ ". Keeping " + first.path() + "."));
				}
			}
			catch (InvalidPostException ex) {
				problems.add(problem(file.path(), ex.getMessage()));
			}
		}
		problems.forEach(problem -> LOG.log(Level.WARNING, problem));
		List<Post> posts = new ArrayList<>(bySlug.values());
		posts.sort(NEWEST_FIRST);
		return new Snapshot(List.copyOf(posts), List.copyOf(problems));
	}

	private static String problem(String path, @Nullable String reason) {
		return "Beansprout skipped " + path + ": " + reason;
	}

	private record Snapshot(List<Post> posts, List<String> problems) {
	}

}
