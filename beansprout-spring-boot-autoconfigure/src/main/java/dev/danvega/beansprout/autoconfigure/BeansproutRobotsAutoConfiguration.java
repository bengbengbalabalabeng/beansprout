package dev.danvega.beansprout.autoconfigure;

import java.util.List;
import java.util.Map;

import dev.danvega.beansprout.view.Links;
import dev.danvega.beansprout.web.RobotsTxt;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;

/**
 * The robots.txt at the site's root. Turn it off with {@code beansprout.robots.enabled=false}.
 *
 * <p>It has its own handler mapping, ordered after the app's controllers and before its static
 * files, so an app that maps {@code /robots.txt} itself still starts and its mapping answers. An
 * app's own static robots.txt is looked for here, and wins too.
 *
 * <p>It waits for Spring MVC's request mappings. A handler mapping bean in an app without them
 * would stop the DispatcherServlet from adding its defaults, and every other page would be lost.
 */
@AutoConfiguration(after = BeansproutAutoConfiguration.class,
		afterName = "org.springframework.boot.webmvc.autoconfigure.WebMvcAutoConfiguration")
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(name = "org.springframework.web.servlet.DispatcherServlet")
@ConditionalOnBean(type = "org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping")
@ConditionalOnBooleanProperty(name = "beansprout.web.enabled", matchIfMissing = true)
@ConditionalOnBooleanProperty(name = "beansprout.robots.enabled", matchIfMissing = true)
public class BeansproutRobotsAutoConfiguration {

	private static final Log LOG = LogFactory.getLog(BeansproutRobotsAutoConfiguration.class);

	/** Spring Boot's static locations when {@code spring.web.resources.static-locations} is not set. */
	private static final List<String> STATIC_LOCATIONS = List.of("classpath:/META-INF/resources/",
			"classpath:/resources/", "classpath:/static/", "classpath:/public/");

	@Bean
	SimpleUrlHandlerMapping beansproutRobotsHandlerMapping(Links links, BeansproutProperties properties,
			Environment environment, ResourceLoader resources) {
		Map<String, Object> urls = Map.of();
		String contextPath = environment.getProperty("server.servlet.context-path", "");
		if (!contextPath.isEmpty() && !contextPath.equals("/")) {
			LOG.info("Beansprout's robots.txt is off: the app has the context path " + contextPath
					+ ", and crawlers read robots.txt only at the root of a site.");
		}
		else if (!hasOwnRobots(environment, resources)) {
			urls = Map.of("/robots.txt", new RobotsTxt(links, properties.basePath(), properties.siteUrl(),
					properties.robots().blocksTraining()));
		}
		return new SimpleUrlHandlerMapping(urls, Ordered.LOWEST_PRECEDENCE - 2);
	}

	/** True when the app serves a robots.txt of its own from one of its static locations. */
	private static boolean hasOwnRobots(Environment environment, ResourceLoader resources) {
		Binder binder = Binder.get(environment);
		if (!binder.bind("spring.web.resources.add-mappings", Boolean.class).orElse(true)) {
			return false;
		}
		List<String> locations = binder.bind("spring.web.resources.static-locations", Bindable.listOf(String.class))
			.orElse(STATIC_LOCATIONS);
		for (String location : locations) {
			String folder = location.endsWith("/") ? location : location + "/";
			if (resources.getResource(folder + "robots.txt").exists()) {
				return true;
			}
		}
		return false;
	}

}
