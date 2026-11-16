package dev.danvega.beansprout.autoconfigure;

import dev.danvega.beansprout.engine.jte.JteThemeRenderer;
import dev.danvega.beansprout.web.ThemeRenderer;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Renders the pages with the Longhand theme when {@code beansprout-theme-longhand} is on the
 * classpath. Classes are named as strings, so an app without JTE never loads them.
 */
@AutoConfiguration
@ConditionalOnClass(name = { "gg.jte.TemplateEngine", BeansproutJteAutoConfiguration.LONGHAND_MARKER })
public class BeansproutJteAutoConfiguration {

	/** A class only the Longhand theme jar has. */
	static final String LONGHAND_MARKER = "dev.danvega.beansprout.engine.jte.longhand.LonghandTheme";

	/** The package the Longhand theme's compiled templates live in. */
	static final String LONGHAND_TEMPLATES = "dev.danvega.beansprout.engine.jte.longhand.templates";

	@Bean
	@ConditionalOnMissingBean
	ThemeRenderer beansproutThemeRenderer() {
		return JteThemeRenderer.precompiled(LONGHAND_TEMPLATES, BeansproutJteAutoConfiguration.class.getClassLoader());
	}

}
