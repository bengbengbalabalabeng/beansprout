package dev.danvega.beansprout.view;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import dev.danvega.beansprout.content.Posts;
import dev.danvega.beansprout.content.Slugs;
import dev.danvega.beansprout.content.TagSummary;

/**
 * Builds the model for every page from an address's parts alone: a page number, a slug, a tag.
 * Controllers call this, and so can a static export. An empty result means 404.
 */
public final class Pages {

	private static final DateTimeFormatter LONG_DATE = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH);

	private static final DateTimeFormatter DAY_MONTH = DateTimeFormatter.ofPattern("d MMMM", Locale.ENGLISH);

	private static final DateTimeFormatter MONTH_YEAR = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH);

	private static final int RECENT = 5;

	private final Posts posts;

	private final Links links;

	private final BlogSettings settings;

	public Pages(Posts posts, Links links, BlogSettings settings) {
		this.posts = posts;
		this.links = links;
		this.settings = settings;
	}

	/** The links this blog uses. */
	public Links links() {
		return links;
	}

	/** The settings this blog uses. */
	public BlogSettings settings() {
		return settings;
	}

	/** Page n of the index. Page 1 always exists, even with no posts. */
	public Optional<IndexPage> index(int page) {
		List<dev.danvega.beansprout.content.Post> published = posts.published();
		return listing(published, page, links::page).map(listing -> new IndexPage(site(""), listing));
	}

	/** A published post by its slug. */
	public Optional<PostPage> post(String slug) {
		List<dev.danvega.beansprout.content.Post> published = posts.published();
		for (int i = 0; i < published.size(); i++) {
			dev.danvega.beansprout.content.Post post = published.get(i);
			if (post.slug().equals(slug)) {
				PostLink newer = i > 0 ? link(published.get(i - 1)) : PostLink.NONE;
				PostLink older = i + 1 < published.size() ? link(published.get(i + 1)) : PostLink.NONE;
				List<Tag> tags = tags(post);
				Tag crumb = tags.isEmpty() ? new Tag("Latest", links.home()) : tags.getFirst();
				Post view = new Post(post.title(), crumb, LONG_DATE.format(post.date()), isoDate(post.date().toLocalDate()),
						reading(post), tags, post.html(),
						post.headings().stream().map(h -> new Heading(h.id(), h.text())).toList(), newer, older);
				return Optional.of(new PostPage(site(""), view));
			}
		}
		return Optional.empty();
	}

	/** Page n of a tag, by the tag's slug. A tag no published post uses has no page. */
	public Optional<TagPage> tag(String tagSlug, int page) {
		Optional<TagSummary> tag = posts.tag(tagSlug);
		if (tag.isEmpty()) {
			return Optional.empty();
		}
		List<dev.danvega.beansprout.content.Post> tagged = posts.tagged(tagSlug);
		return listing(tagged, page, n -> links.tagPage(tagSlug, n))
			.map(listing -> new TagPage(site(tagSlug), tag.get().name(), count(tagged.size()), listing));
	}

	/** Every published post by year, newest year first. */
	public ArchivePage archive() {
		List<dev.danvega.beansprout.content.Post> published = posts.published();
		Map<Integer, List<ArchiveRow>> byYear = new LinkedHashMap<>();
		for (dev.danvega.beansprout.content.Post post : published) {
			byYear.computeIfAbsent(post.date().getYear(), year -> new ArrayList<>())
				.add(new ArchiveRow(post.title(), links.post(post.slug()), DAY_MONTH.format(post.date()),
						isoDate(post.date().toLocalDate()), post.readingMinutes() + " min"));
		}
		List<Year> years = byYear.entrySet().stream()
			.map(entry -> new Year(String.valueOf(entry.getKey()), count(entry.getValue().size()), List.copyOf(entry.getValue())))
			.toList();
		String since = published.isEmpty() ? "" : MONTH_YEAR.format(published.getLast().date());
		return new ArchivePage(site(""), new Archive(count(published.size()), since, years));
	}

	/** The page for an address with nothing at it. */
	public NotFoundPage notFound(String path) {
		List<dev.danvega.beansprout.content.Post> published = posts.published();
		List<MiniPost> recent = published.stream().limit(RECENT)
			.map(post -> new MiniPost(post.title(), links.post(post.slug()), LONG_DATE.format(post.date()),
					isoDate(post.date().toLocalDate()), reading(post)))
			.toList();
		return new NotFoundPage(site(""), path, count(published.size()), recent);
	}

	/**
	 * Every address the blog serves: the index pages, the archive, every post, and every tag
	 * page. The sitemap is built from this, and so would a static export be.
	 */
	public List<PageUrl> urls() {
		List<dev.danvega.beansprout.content.Post> published = posts.published();
		List<PageUrl> urls = new ArrayList<>();
		for (int n = 1; n <= pageCount(published.size()); n++) {
			urls.add(new PageUrl(links.page(n), ""));
		}
		urls.add(new PageUrl(links.archive(), ""));
		for (dev.danvega.beansprout.content.Post post : published) {
			urls.add(new PageUrl(links.post(post.slug()), isoDate(post.date().toLocalDate())));
		}
		for (TagSummary tag : posts.tags()) {
			for (int n = 1; n <= pageCount(tag.count()); n++) {
				urls.add(new PageUrl(links.tagPage(tag.slug(), n), ""));
			}
		}
		return urls;
	}

	private Optional<Listing> listing(List<dev.danvega.beansprout.content.Post> all, int page,
			java.util.function.IntFunction<String> pageLink) {
		int pages = pageCount(all.size());
		if (page < 1 || page > pages) {
			return Optional.empty();
		}
		int from = (page - 1) * settings.pageSize();
		List<Card> cards = all.subList(from, Math.min(all.size(), from + settings.pageSize())).stream()
			.map(this::card)
			.toList();
		String newer = page > 1 ? pageLink.apply(page - 1) : "";
		String older = page < pages ? pageLink.apply(page + 1) : "";
		return Optional.of(new Listing(cards, String.valueOf(page), String.valueOf(pages), newer, older));
	}

	private int pageCount(int posts) {
		return Math.max(1, (posts + settings.pageSize() - 1) / settings.pageSize());
	}

	private Card card(dev.danvega.beansprout.content.Post post) {
		return new Card(post.title(), links.post(post.slug()), LONG_DATE.format(post.date()),
				isoDate(post.date().toLocalDate()), reading(post), post.summary(), tags(post));
	}

	private List<Tag> tags(dev.danvega.beansprout.content.Post post) {
		return post.tags().stream().map(tag -> new Tag(tag, links.tag(Slugs.slugify(tag)))).toList();
	}

	private PostLink link(dev.danvega.beansprout.content.Post post) {
		return new PostLink(post.title(), links.post(post.slug()));
	}

	private Site site(String currentTag) {
		List<TagCount> tags = posts.tags().stream()
			.map(tag -> new TagCount(tag.name(), links.tag(tag.slug()), String.valueOf(tag.count()),
					tag.slug().equals(currentTag)))
			.toList();
		return new Site(settings.title(), settings.description(),
				String.valueOf(LocalDate.now(posts.clock()).getYear()), links.home(), links.archive(), links.feed(),
				links.root(), tags);
	}

	private static String reading(dev.danvega.beansprout.content.Post post) {
		return post.readingMinutes() + " min read";
	}

	private static String isoDate(LocalDate date) {
		return date.toString();
	}

	private static String count(int posts) {
		return posts == 1 ? "1 post" : posts + " posts";
	}

}
