package dev.danvega.beansprout.autoconfigure;

import dev.danvega.beansprout.feed.Feeds;
import dev.danvega.beansprout.view.Pages;
import dev.danvega.beansprout.web.BlogController;
import dev.danvega.beansprout.web.FeedController;
import dev.danvega.beansprout.web.ThemeRenderer;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;

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

	@Bean
	@ConditionalOnBean(ThemeRenderer.class)
	@ConditionalOnMissingBean
	BlogController beansproutBlogController(Pages pages, ThemeRenderer theme, BeansproutProperties properties) {
		return new BlogController(pages, theme, properties.siteUrl());
	}

	/** The feed and the sitemap need no template, so they are served with or without a theme. */
	@Bean
	@ConditionalOnMissingBean
	FeedController beansproutFeedController(Feeds feeds, BeansproutProperties properties) {
		return new FeedController(feeds, properties.siteUrl());
	}

}
