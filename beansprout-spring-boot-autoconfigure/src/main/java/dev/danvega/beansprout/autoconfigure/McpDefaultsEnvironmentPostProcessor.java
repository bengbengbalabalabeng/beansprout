package dev.danvega.beansprout.autoconfigure;

import java.util.Map;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.util.ClassUtils;

/**
 * Serves the MCP tools over streamable HTTP at {@code /mcp}, as specs/mcp.md says. Spring AI 2.0
 * falls back to the older SSE transport when {@code spring.ai.mcp.server.protocol} is not set, so
 * this sets it, and names the server {@code beansprout}. Both rank below every other setting, so an
 * app's own choice wins. Without beansprout-mcp-spring-boot-starter on the classpath it does
 * nothing, so an app's own Spring AI MCP server keeps its settings.
 */
public class McpDefaultsEnvironmentPostProcessor implements EnvironmentPostProcessor {

	static final String SOURCE = "beansproutMcpDefaults";

	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
		ClassLoader loader = application.getClassLoader();
		if (!ClassUtils.isPresent("org.springframework.ai.mcp.annotation.McpTool", loader)
				|| loader.getResource(BeansproutMcpAutoConfiguration.MARKER) == null) {
			return;
		}
		environment.getPropertySources()
			.addLast(new MapPropertySource(SOURCE,
					Map.of("spring.ai.mcp.server.protocol", "STREAMABLE", "spring.ai.mcp.server.name", "beansprout")));
	}

}
