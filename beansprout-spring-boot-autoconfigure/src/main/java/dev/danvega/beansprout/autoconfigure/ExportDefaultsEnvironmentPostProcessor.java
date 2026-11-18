package dev.danvega.beansprout.autoconfigure;

import java.util.Map;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * For a static export, serves the app on a free port on 127.0.0.1 only. The export fetches the blog
 * from itself, so a running copy of the app on the usual port does not block it, and nothing outside
 * this machine can reach it meanwhile. These rank above every other setting. Without
 * {@code beansprout.export.enabled=true} this does nothing.
 */
public class ExportDefaultsEnvironmentPostProcessor implements EnvironmentPostProcessor {

	static final String SOURCE = "beansproutExportDefaults";

	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
		if (!environment.getProperty("beansprout.export.enabled", Boolean.class, false)) {
			return;
		}
		environment.getPropertySources()
			.addFirst(new MapPropertySource(SOURCE, Map.of("server.port", "0", "server.address", "127.0.0.1")));
	}

}
