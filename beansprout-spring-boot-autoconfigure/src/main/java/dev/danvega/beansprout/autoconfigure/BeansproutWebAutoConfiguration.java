package dev.danvega.beansprout.autoconfigure;

import java.util.Optional;

import dev.danvega.beansprout.content.Posts;
import dev.danvega.beansprout.feed.Feeds;
import dev.danvega.beansprout.view.Aliases;
import dev.danvega.beansprout.web.AliasRedirects;
import dev.danvega.beansprout.view.Pages;
import dev.danvega.beansprout.web.BlogController;
import dev.danvega.beansprout.web.BundleImageController;
import dev.danvega.beansprout.web.FeedController;
import dev.danvega.beansprout.web.ThemeRenderer;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternResolver;

/**
 * The blog's pages (when a theme is present), feed and sitemap, in a servlet web app. Turn them off with
 * {@code beansprout.web.enabled=false} and call {@code Posts} or {@code Pages} from your own
 * controllers.
 */
@AutoConfiguration(after = { BeansproutAutoConfiguration.class, BeansproutJteAutoConfiguration.class })
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(name = "org.springframework.web.servlet.DispatcherServlet")
@ConditionalOnBooleanProperty(name = "beansprout.web.enabled", matchIfMissing = true)
public class BeansproutWebAutoConfiguration {

	/** Where a request for a post's alias goes: to the post, unless the app has a static file there. */
	@Bean
	@ConditionalOnMissingBean
	AliasRedirects beansproutAliasRedirects(Aliases aliases, Environment environment, ResourceLoader resources) {
		return new AliasRedirects(aliases, path -> Optional.ofNullable(StaticFiles.find(path, environment, resources)));
	}

	@Bean
	@ConditionalOnBean(ThemeRenderer.class)
	@ConditionalOnMissingBean
	BlogController beansproutBlogController(Pages pages, ThemeRenderer theme, BeansproutProperties properties,
			AliasRedirects aliases) {
		return new BlogController(pages, theme, properties.siteUrl(), aliases);
	}

	/** The feed and the sitemap need no template, so they are served with or without a theme. */
	@Bean
	@ConditionalOnMissingBean
	FeedController beansproutFeedController(Feeds feeds, BeansproutProperties properties) {
		return new FeedController(feeds, properties.siteUrl());
	}

	/**
	 * Stops the start when {@code beansprout.theme} names a theme but no theme renders the blog,
	 * because no theme jar is on the classpath. Without this the app would start with no blog.
	 */
	@Bean
	@ConditionalOnProperty("beansprout.theme")
	@ConditionalOnMissingBean(ThemeRenderer.class)
	ThemeCheck beansproutThemeCheck(BeansproutProperties properties) {
		String theme = properties.theme();
		if (theme != null && !theme.isBlank()) {
			throw new IllegalStateException("beansprout.theme is '" + theme + "', but there is no theme on the classpath. "
					+ "Add one, such as dev.danvega:beansprout-theme-" + theme.strip() + ". Themes found: none.");
		}
		return new ThemeCheck();
	}

	/** The mark that the theme check ran. */
	static final class ThemeCheck {

	}

	/** The images in a post's folder, served with or without a theme. */
	@Bean
	@ConditionalOnMissingBean
	BundleImageController beansproutBundleImageController(Posts posts, ResourcePatternResolver resolver,
			BeansproutProperties properties, AliasRedirects aliases) {
		return new BundleImageController(posts, new ResourcePostSource(resolver, properties.posts().location()),
				aliases);
	}

}
