package dev.danvega.beansprout.autoconfigure;

import java.time.Clock;
import java.time.ZoneId;

import dev.danvega.beansprout.code.CodeHighlighter;
import dev.danvega.beansprout.code.PrismHighlighter;
import dev.danvega.beansprout.content.PostParser;
import dev.danvega.beansprout.content.PostSource;
import dev.danvega.beansprout.content.Posts;
import dev.danvega.beansprout.feed.Feeds;
import dev.danvega.beansprout.view.BlogSettings;
import dev.danvega.beansprout.view.Links;
import dev.danvega.beansprout.view.Pages;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.core.io.support.ResourcePatternResolver;

/**
 * The blog without its pages: posts, the page models and the feeds. Works in any app, web or not.
 * Every bean backs off when the app defines its own.
 */
@AutoConfiguration
@EnableConfigurationProperties(BeansproutProperties.class)
public class BeansproutAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	PostSource beansproutPostSource(ResourcePatternResolver resolver, BeansproutProperties properties) {
		return new ResourcePostSource(resolver, properties.posts().location());
	}

	@Bean
	@ConditionalOnMissingBean
	CodeHighlighter beansproutCodeHighlighter(BeansproutProperties properties) {
		return properties.code().highlight() ? new PrismHighlighter() : CodeHighlighter.NONE;
	}

	@Bean
	@ConditionalOnMissingBean
	Posts beansproutPosts(PostSource source, CodeHighlighter highlighter, BeansproutProperties properties,
			Environment environment) {
		ZoneId zone = properties.timeZone() != null ? properties.timeZone() : ZoneId.systemDefault();
		// The property turns off highlighting even when the app supplies its own highlighter.
		CodeHighlighter used = properties.code().highlight() ? highlighter : CodeHighlighter.NONE;
		// The parser links a bundle's images under the blog's address, context path included.
		PostParser parser = new PostParser(zone, used, environment.getProperty("server.servlet.context-path", ""),
				properties.basePath());
		return new Posts(source, parser, Clock.system(zone), properties.posts().reload());
	}

	@Bean
	@ConditionalOnMissingBean
	Links beansproutLinks(BeansproutProperties properties, Environment environment) {
		return new Links(environment.getProperty("server.servlet.context-path", ""), properties.basePath());
	}

	@Bean
	@ConditionalOnMissingBean
	Pages beansproutPages(Posts posts, Links links, BeansproutProperties properties) {
		return new Pages(posts, links, new BlogSettings(properties.title(), properties.description(), properties.pageSize(),
				properties.author(), properties.photo()));
	}

	@Bean
	@ConditionalOnMissingBean
	Feeds beansproutFeeds(Posts posts, Links links, Pages pages) {
		return new Feeds(posts, links, pages);
	}

}
