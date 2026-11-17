package dev.danvega.beansprout.autoconfigure;

import dev.danvega.beansprout.engine.jte.JteThemeRenderer;
import dev.danvega.beansprout.theme.Theme;
import dev.danvega.beansprout.web.ThemeRenderer;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.core.io.ResourceLoader;

/**
 * Renders the pages with a JTE theme jar: the only one on the classpath, or the one
 * {@code beansprout.theme} names. JTE classes are named as strings, so an app without JTE never
 * loads them. An app with its own {@link ThemeRenderer} skips all of this, theme jars or not.
 */
@AutoConfiguration
@ConditionalOnClass(name = "gg.jte.TemplateEngine")
@Conditional(OnThemeCondition.class)
@ConditionalOnMissingBean(ThemeRenderer.class)
public class BeansproutJteAutoConfiguration {

	/** The engine name JTE themes give in {@link Theme#engine()}. */
	static final String ENGINE = "jte";

	/** The theme that renders the blog, chosen as {@link Themes#choose} says. */
	@Bean
	Theme beansproutTheme(BeansproutProperties properties, ResourceLoader resourceLoader) {
		return Themes.choose(Themes.found(resourceLoader.getClassLoader(), ENGINE), properties.theme());
	}

	@Bean
	ThemeRenderer beansproutThemeRenderer(Theme theme) {
		return JteThemeRenderer.precompiled(theme.templates(), theme.getClass().getClassLoader());
	}

}
