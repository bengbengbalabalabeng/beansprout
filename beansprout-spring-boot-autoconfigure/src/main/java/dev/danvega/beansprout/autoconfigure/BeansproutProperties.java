package dev.danvega.beansprout.autoconfigure;

import java.time.ZoneId;
import java.util.List;

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
 * @param relatedPosts how many posts that share a tag a post page shows after the post. 0 for none.
 * @param siteUrl the site's address, like {@code https://example.com}, for the feed and the
 *     sitemap. When not set, the request's scheme, host and port are used.
 * @param timeZone the zone a post's plain date starts in. Defaults to the server's zone.
 * @param author the writer's name, shown on each post page. Empty for none.
 * @param photo the writer's picture, shown above the blog's name: a path starting with {@code /}
 *     or a full http or https address. Empty for none.
 * @param theme which theme renders the blog, like {@code dateline}. Needed only when more than one
 *     theme jar is on the classpath.
 * @param posts where the posts come from
 * @param web the blog's pages
 * @param code how code fences are shown
 * @param mcp who may call the MCP server, when the app adds the MCP starter
 * @param export the static export: the blog written as files for a static host
 * @param robots the robots.txt served at the site's root
 * @param links links every theme adds to its main menu, after its own items, such as a way back
 *     to the rest of the site
 */
@ConfigurationProperties("beansprout")
public record BeansproutProperties(
		@DefaultValue("Blog") String title,
		@DefaultValue("") String description,
		@DefaultValue("/blog") String basePath,
		@DefaultValue("10") int pageSize,
		@DefaultValue("3") int relatedPosts,
		@Nullable String siteUrl,
		@Nullable ZoneId timeZone,
		@DefaultValue("") String author,
		@DefaultValue("") String photo,
		@Nullable String theme,
		@DefaultValue PostsSettings posts,
		@DefaultValue WebSettings web,
		@DefaultValue CodeSettings code,
		@DefaultValue McpSettings mcp,
		@DefaultValue ExportSettings export,
		@DefaultValue RobotsSettings robots,
		@DefaultValue List<LinkSettings> links) {

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

	/**
	 * How code fences are shown.
	 *
	 * @param highlight false to show every fence as plain code
	 */
	public record CodeSettings(@DefaultValue("true") boolean highlight) {
	}

	/**
	 * Who may call the MCP server at {@code /mcp}.
	 *
	 * @param token when set, every MCP request must send it as {@code Authorization: Bearer
	 *     <token>}, from any machine. When not set, only requests from the same machine are
	 *     answered.
	 */
	public record McpSettings(@Nullable String token) {
	}

	/**
	 * The static export.
	 *
	 * @param enabled true to start the app, write the blog to {@code output} as files, and stop
	 * @param output the folder to write to, relative to where the app runs
	 */
	public record ExportSettings(boolean enabled, @DefaultValue("target/blog") String output) {
	}

	/**
	 * The robots.txt served at the site's root. It is not served when the app has its own, or when
	 * the app has a servlet context path, since crawlers read robots.txt only at the root.
	 *
	 * @param enabled false to serve no robots.txt
	 * @param aiTraining {@code allow}, or {@code block} to keep the AI training crawlers (GPTBot,
	 *     ClaudeBot, Google-Extended, Applebot-Extended and CCBot) out of the blog. Search
	 *     crawlers, AI search included, may still read it.
	 */
	public record RobotsSettings(@DefaultValue("true") boolean enabled, @DefaultValue("allow") String aiTraining) {

		public RobotsSettings {
			// A String, not an enum: Spring reads an empty enum value as "not set", so a blank
			// line would quietly mean allow.
			String value = aiTraining.strip().toLowerCase(java.util.Locale.ROOT);
			if (!value.equals("allow") && !value.equals("block")) {
				throw new IllegalArgumentException(
						"beansprout.robots.ai-training must be allow or block, not '" + aiTraining + "'");
			}
			aiTraining = value;
		}

		/** True when the AI training crawlers are kept out of the blog. */
		public boolean blocksTraining() {
			return aiTraining.equals("block");
		}

	}

	/**
	 * One link in every theme's main menu.
	 *
	 * @param label the text the reader sees
	 * @param href a path starting with {@code /}, which gets the context path in front, or a full
	 *     http or https address
	 */
	public record LinkSettings(@Nullable String label, @Nullable String href) {
	}

}
