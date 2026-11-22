package dev.danvega.beansprout.autoconfigure;

import dev.danvega.beansprout.web.AliasHandlerMapping;
import dev.danvega.beansprout.web.AliasRedirects;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;

/**
 * A post's aliases, such as {@code /2019/03/14/hello.html}, answered with a 301 to the post.
 *
 * <p>They have their own handler mapping, ordered after the app's controllers and before its
 * static files, like robots.txt. An app that maps an alias's path, or has a static file there,
 * answers it itself. An alias under the blog's own path is answered by the blog's controllers,
 * with the same {@link AliasRedirects}.
 *
 * <p>It waits for Spring MVC's request mappings. A handler mapping bean in an app without them
 * would stop the DispatcherServlet from adding its defaults, and every other page would be lost.
 */
@AutoConfiguration(after = { BeansproutAutoConfiguration.class, BeansproutWebAutoConfiguration.class },
		afterName = "org.springframework.boot.webmvc.autoconfigure.WebMvcAutoConfiguration")
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(name = "org.springframework.web.servlet.DispatcherServlet")
@ConditionalOnBean(type = "org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping")
@ConditionalOnBooleanProperty(name = "beansprout.web.enabled", matchIfMissing = true)
public class BeansproutAliasesAutoConfiguration {

	@Bean
	AliasHandlerMapping beansproutAliasHandlerMapping(AliasRedirects redirects) {
		return new AliasHandlerMapping(redirects);
	}

}
