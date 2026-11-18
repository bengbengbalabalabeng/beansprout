package dev.danvega.beansprout.autoconfigure;

import java.net.URI;
import java.nio.file.Path;
import java.util.List;

import dev.danvega.beansprout.export.ExportException;
import dev.danvega.beansprout.export.ExportOnReady;
import dev.danvega.beansprout.export.StaticExport;
import dev.danvega.beansprout.view.Pages;
import dev.danvega.beansprout.web.ThemeRenderer;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

/**
 * The static export, when {@code beansprout.export.enabled=true}: the app starts, writes the blog
 * as files to {@code beansprout.export.output}, and stops. Settings an export cannot work with stop
 * the start, before anything is written.
 */
@AutoConfiguration(after = { BeansproutAutoConfiguration.class, BeansproutJteAutoConfiguration.class,
		BeansproutWebAutoConfiguration.class })
@ConditionalOnBooleanProperty("beansprout.export.enabled")
public class BeansproutExportAutoConfiguration {

	@Bean
	StaticExport beansproutStaticExport(Pages pages, ObjectProvider<ThemeRenderer> themes,
			BeansproutProperties properties, Environment environment) {
		String siteUrl = siteUrl(properties.siteUrl());
		if (!properties.web().enabled()) {
			throw new ExportException("beansprout.web.enabled is false, so the blog has no pages to export. "
					+ "Remove it, or set it to true, for the export.");
		}
		ThemeRenderer theme = themes.getIfAvailable();
		if (theme == null) {
			throw new ExportException("The static export found no theme to render the blog's pages. "
					+ "Add one, such as dev.danvega:beansprout-theme-longhand.");
		}
		Pages published = pages.forSite(siteUrl);
		return new StaticExport(pages, siteUrl, () -> theme.render("not-found", published.notFound("")),
				Path.of(properties.export().output()),
				// The MCP server answers this machine, so the crawl would reach it. A static site has none.
				List.of(pages.links().root() + environment.getProperty(BeansproutMcpAutoConfiguration.ENDPOINT, "/mcp")));
	}

	@Bean
	ExportOnReady beansproutExportOnReady(StaticExport export) {
		return new ExportOnReady(export);
	}

	/** The site's address, checked: it must be set, and must have no path. */
	private static String siteUrl(@org.jspecify.annotations.Nullable String configured) {
		if (configured == null || configured.isBlank()) {
			throw new ExportException("The static export needs beansprout.site-url, the address the site is published "
					+ "at, like https://name.github.io. Set it, so the feed, the sitemap and each page's canonical link "
					+ "point there and not at this machine.");
		}
		String siteUrl = configured.strip();
		URI uri;
		try {
			uri = URI.create(siteUrl);
		}
		catch (IllegalArgumentException ex) {
			throw new ExportException("beansprout.site-url '" + siteUrl + "' is not an address. Write it like "
					+ "https://name.github.io.", ex);
		}
		String path = uri.getRawPath();
		boolean hasPath = path != null && !path.isEmpty() && !path.equals("/");
		if (hasPath || uri.getRawQuery() != null || uri.getRawFragment() != null || uri.getHost() == null) {
			String origin = uri.getScheme() + "://" + uri.getRawAuthority();
			String prefix = path == null ? "" : path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
			throw new ExportException("beansprout.site-url is " + siteUrl + ". For the export it must be the site's "
					+ "address with no path. Set beansprout.site-url=" + origin + " and server.servlet.context-path="
					+ prefix + ", so every link starts with " + prefix + ".");
		}
		return siteUrl;
	}

}
