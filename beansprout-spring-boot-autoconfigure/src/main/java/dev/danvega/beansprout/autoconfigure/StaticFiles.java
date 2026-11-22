package dev.danvega.beansprout.autoconfigure;

import java.util.List;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

/** The app's static files, as Spring Boot serves them from its static locations. */
final class StaticFiles {

	/** Spring Boot's static locations when {@code spring.web.resources.static-locations} is not set. */
	private static final List<String> STATIC_LOCATIONS = List.of("classpath:/META-INF/resources/",
			"classpath:/resources/", "classpath:/static/", "classpath:/public/");

	private StaticFiles() {
	}

	/** The app's static file at the path, like {@code /about.html}, or null when it has none there. */
	static @Nullable Resource find(String path, Environment environment, ResourceLoader resources) {
		if (path.endsWith("/") || !path.startsWith("/")) {
			return null;
		}
		Binder binder = Binder.get(environment);
		if (!binder.bind("spring.web.resources.add-mappings", Boolean.class).orElse(true)) {
			return null;
		}
		List<String> locations = binder.bind("spring.web.resources.static-locations", Bindable.listOf(String.class))
			.orElse(STATIC_LOCATIONS);
		for (String location : locations) {
			String folder = location.endsWith("/") ? location : location + "/";
			Resource resource = resources.getResource(folder + path.substring(1));
			if (resource.isReadable()) {
				return resource;
			}
		}
		return null;
	}

}
