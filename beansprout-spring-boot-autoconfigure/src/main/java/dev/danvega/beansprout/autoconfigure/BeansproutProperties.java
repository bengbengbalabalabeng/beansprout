package dev.danvega.beansprout.autoconfigure;

import java.time.ZoneId;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Everything an app can set for its blog, under {@code beansprout.*}. Every value has a default,
 * so an app that sets nothing still gets a blog.
 *
 * @param title the blog's name, shown on every page and in the feed
 * @param description a line under the name, or empty
 * @param basePath where the blog lives, like {@code /blog}, with no trailing slash
 * @param pageSize posts per page on the index and on tag pages
 * @param siteUrl the site's address, like {@code https://example.com}, for the feed and the
 *     sitemap. When not set, the request's scheme, host and port are used.
 * @param timeZone the zone a post's plain date starts in. Defaults to the server's zone.
 * @param author the writer's name, shown on each post page. Empty for none.
 * @param photo the writer's picture, shown above the blog's name: a path starting with {@code /}
 *     or a full http or https address. Empty for none.
 * @param posts where the posts come from
 * @param web the blog's pages
 */
@ConfigurationProperties("beansprout")
public record BeansproutProperties(
		@DefaultValue("Blog") String title,
		@DefaultValue("") String description,
		@DefaultValue("/blog") String basePath,
		@DefaultValue("10") int pageSize,
		@Nullable String siteUrl,
		@Nullable ZoneId timeZone,
		@DefaultValue("") String author,
		@DefaultValue("") String photo,
		@DefaultValue PostsSettings posts,
		@DefaultValue WebSettings web) {

	public BeansproutProperties {
		if (siteUrl != null && !siteUrl.isBlank()) {
			String scheme = siteUrl.strip().toLowerCase(java.util.Locale.ROOT);
			if (!scheme.startsWith("http://") && !scheme.startsWith("https://")) {
				throw new IllegalArgumentException(
						"beansprout.site-url must start with http:// or https://, like https://blog.example.com, not '" + siteUrl + "'");
			}
		}
	}

	/**
	 * Where the posts come from.
	 *
	 * @param location a Spring resource location: {@code classpath:posts/} or a folder like
	 *     {@code file:./posts/}. Subfolders are read too.
	 * @param reload read the files again on every request, so edits show with no restart. For
	 *     development: leave it off in production.
	 */
	public record PostsSettings(@DefaultValue("classpath:posts/") String location, boolean reload) {
	}

	/**
	 * The blog's pages.
	 *
	 * @param enabled false to register no controllers, and call the posts service from your own
	 */
	public record WebSettings(@DefaultValue("true") boolean enabled) {
	}

}
