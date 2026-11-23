package dev.danvega.beansprout.view;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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

	private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ssXXX");

	private final Posts posts;

	private final Links links;

	private final BlogSettings settings;

	private final String siteUrl;

	public Pages(Posts posts, Links links, BlogSettings settings) {
		this(posts, links, settings, "");
	}

	private Pages(Posts posts, Links links, BlogSettings settings, String siteUrl) {
		this.posts = posts;
		this.links = links;
		this.settings = settings;
		this.siteUrl = tidy(siteUrl);
	}

	/**
	 * The same pages, with absolute addresses for canonical links, link previews and structured
	 * data. The site's address is {@code https://example.com}, with or without a trailing slash.
	 */
	public Pages forSite(String siteUrl) {
		return new Pages(posts, links, settings, siteUrl);
	}

	/** The links this blog uses. */
	public Links links() {
		return links;
	}

	/** The settings this blog uses. */
	public BlogSettings settings() {
		return settings;
	}

	/** The posts behind these pages, for {@link Aliases}. */
	Posts posts() {
		return posts;
	}

	/** Page n of the index. Page 1 always exists, even with no posts. */
	public Optional<IndexPage> index(int page) {
		List<dev.danvega.beansprout.content.Post> published = posts.published();
		String title = page == 1 ? settings.title() : "Page " + page + " · " + settings.title();
		String description = settings.description().isEmpty() ? "The latest posts from " + settings.title() + "."
				: settings.description();
		return listing(published, page, links::page)
			.map(listing -> new IndexPage(site("", "latest"), pageMeta(title, description, links.page(page)), listing));
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
				String picture = links.picture(post.image());
				boolean laterDay = post.modified().toLocalDate().isAfter(post.date().toLocalDate());
				Post view = new Post(post.title(), links.post(post.slug()), post.lede(), crumb, author(post), LONG_DATE.format(post.date()),
						isoDate(post.date().toLocalDate()), reading(post),
						laterDay ? LONG_DATE.format(post.modified()) : "",
						laterDay ? isoDate(post.modified().toLocalDate()) : "", tags, picture, post.imageAlt(),
						picture.isEmpty() || post.imageWidth() == 0 ? "" : Integer.toString(post.imageWidth()),
						picture.isEmpty() || post.imageHeight() == 0 ? "" : Integer.toString(post.imageHeight()),
						picture.isEmpty() ? "" : post.caption(), post.html(),
						post.headings().stream().map(h -> new Heading(h.id(), h.text())).toList(), newer, older);
				return Optional.of(new PostPage(site("", ""), postMeta(post), view));
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
			.map(listing -> new TagPage(site(tagSlug, ""),
					pageMeta(tag.get().name(), "Posts tagged " + tag.get().name() + " on " + settings.title() + ".",
							links.tagPage(tagSlug, page)),
					tag.get().name(), count(tagged.size()), listing, links.tagFeed(tagSlug)));
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
		return new ArchivePage(site("", "archive"),
				pageMeta("Archive", "Every post on " + settings.title() + ", by year.", links.archive()),
				new Archive(count(published.size()), since, years));
	}

	/** The page for an address with nothing at it. */
	public NotFoundPage notFound(String path) {
		List<dev.danvega.beansprout.content.Post> published = posts.published();
		List<MiniPost> recent = published.stream().limit(RECENT)
			.map(post -> new MiniPost(post.title(), links.post(post.slug()), LONG_DATE.format(post.date()),
					isoDate(post.date().toLocalDate()), reading(post)))
			.toList();
		Meta meta = new Meta("Page not found", "There is no page at this address on " + settings.title() + ".", "", "website",
				settings.title(),
				photo(), "", "summary", "", "", List.of(), "");
		return new NotFoundPage(site("", ""), meta, path, count(published.size()), recent);
	}

	/**
	 * Every address the blog serves: the index pages, the archive, every post, and every tag
	 * page. The static export writes each one. The sitemap is built from {@link #sitemapUrls()}.
	 */
	public List<PageUrl> urls() {
		List<dev.danvega.beansprout.content.Post> published = posts.published();
		List<PageUrl> urls = new ArrayList<>();
		for (int n = 1; n <= pageCount(published.size()); n++) {
			urls.add(new PageUrl(links.page(n), ""));
		}
		urls.add(new PageUrl(links.archive(), ""));
		for (dev.danvega.beansprout.content.Post post : published) {
			urls.add(new PageUrl(links.post(post.slug()), isoDate(post.modified().toLocalDate())));
		}
		for (TagSummary tag : posts.tags()) {
			for (int n = 1; n <= pageCount(tag.count()); n++) {
				urls.add(new PageUrl(links.tagPage(tag.slug(), n), ""));
			}
		}
		return urls;
	}

	/**
	 * The addresses for the sitemap: every one in {@link #urls()}, less the posts that set a
	 * {@code canonical}. A sitemap that lists a page whose canonical link points elsewhere gives
	 * search engines two signals that disagree. The static export still writes those posts.
	 */
	public List<PageUrl> sitemapUrls() {
		Set<String> elsewhere = new HashSet<>();
		for (dev.danvega.beansprout.content.Post post : posts.published()) {
			if (!post.canonical().isEmpty()) {
				elsewhere.add(links.post(post.slug()));
			}
		}
		return urls().stream().filter(url -> !elsewhere.contains(url.path())).toList();
	}

	private Meta pageMeta(String title, String description, String path) {
		return new Meta(title, description, absolute(path), "website", settings.title(), photo(), "", "summary", "", "",
				List.of(), "");
	}

	private Meta postMeta(dev.danvega.beansprout.content.Post post) {
		String url = post.canonical().isEmpty() ? absolute(links.post(post.slug())) : post.canonical();
		String picture = post.image().isEmpty() ? "" : absolutePicture(post.image());
		String published = TIMESTAMP.format(post.date());
		String modified = TIMESTAMP.format(post.modified());

		Map<String, Object> json = new LinkedHashMap<>();
		json.put("@context", "https://schema.org");
		json.put("@type", "BlogPosting");
		json.put("headline", post.title());
		json.put("description", post.summary());
		json.put("url", url);
		json.put("mainEntityOfPage", url);
		json.put("datePublished", published);
		json.put("dateModified", modified);
		if (!picture.isEmpty()) {
			json.put("image", picture);
		}
		String name = author(post);
		if (!name.isEmpty()) {
			Map<String, Object> author = new LinkedHashMap<>();
			author.put("@type", "Person");
			author.put("name", name);
			// The blog is the writer's home page, not a guest's.
			if (name.equals(settings.author())) {
				author.put("url", absolute(links.home()));
			}
			json.put("author", author);
		}

		return new Meta(post.title(), post.summary(), url, "article", settings.title(),
				picture.isEmpty() ? photo() : picture, picture.isEmpty() ? "" : post.imageAlt(),
				picture.isEmpty() ? "summary" : "summary_large_image", published, modified, post.tags(), JsonLd.object(json));
	}

	/** The post's own author, or the blog's when it has none. Empty when neither is set. */
	private String author(dev.danvega.beansprout.content.Post post) {
		return post.author().isEmpty() ? settings.author() : post.author();
	}

	/** A site address without spaces around it or slashes at its end. */
	static String tidy(String siteUrl) {
		String tidy = siteUrl.strip();
		while (tidy.endsWith("/")) {
			tidy = tidy.substring(0, tidy.length() - 1);
		}
		return tidy;
	}

	/** A path in the app becomes an absolute address on this site. */
	private String absolute(String path) {
		return siteUrl + path;
	}

	/** A picture's absolute address: a full address as written, a path on this site. */
	private String absolutePicture(String address) {
		String picture = links.picture(address);
		return picture.startsWith("/") ? absolute(picture) : picture;
	}

	private String photo() {
		return settings.photo().isEmpty() ? "" : absolutePicture(settings.photo());
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
				isoDate(post.date().toLocalDate()), reading(post), post.summary(), links.picture(post.image()), post.imageAlt(),
				tags(post));
	}

	private List<Tag> tags(dev.danvega.beansprout.content.Post post) {
		return post.tags().stream().map(tag -> new Tag(tag, links.tag(Slugs.slugify(tag)))).toList();
	}

	private PostLink link(dev.danvega.beansprout.content.Post post) {
		return new PostLink(post.title(), links.post(post.slug()));
	}

	private Site site(String currentTag, String section) {
		List<TagCount> tags = posts.tags().stream()
			.map(tag -> new TagCount(tag.name(), links.tag(tag.slug()), String.valueOf(tag.count()),
					tag.slug().equals(currentTag)))
			.toList();
		return new Site(settings.title(), settings.description(),
				String.valueOf(LocalDate.now(posts.clock()).getYear()), links.home(), links.archive(), links.feed(),
				links.root(), links.picture(settings.photo()), section, tags,
				settings.links().stream().map(link -> new NavLink(link.label(), links.inApp(link.href()))).toList());
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
