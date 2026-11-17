package dev.danvega.beansprout.autoconfigure;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.function.Supplier;

import dev.danvega.beansprout.content.PostSource;
import dev.danvega.beansprout.content.Posts;
import dev.danvega.beansprout.mcp.BlogTools;
import dev.danvega.beansprout.mcp.DraftTools;
import dev.danvega.beansprout.mcp.McpAccessFilter;
import dev.danvega.beansprout.view.Links;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnResource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * The blog's MCP tools, when the app adds {@code beansprout-mcp-spring-boot-starter}, which brings
 * Spring AI's MCP server. Spring AI serves them at {@code /mcp}. Classes are named as strings, so an
 * app without Spring AI never loads them, and an app with its own Spring AI MCP server but not
 * Beansprout's starter gets none of this.
 */
@AutoConfiguration(after = BeansproutAutoConfiguration.class)
@ConditionalOnClass(name = { "org.springframework.ai.mcp.annotation.McpTool", "io.modelcontextprotocol.server.McpSyncServer" })
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnResource(resources = "classpath:" + BeansproutMcpAutoConfiguration.MARKER)
public class BeansproutMcpAutoConfiguration {

	/** A file only beansprout-mcp-spring-boot-starter has, so an app's own MCP server is left alone. */
	static final String MARKER = "META-INF/beansprout/mcp-starter.properties";

	/** Spring AI's property for the MCP endpoint, and its default. */
	static final String ENDPOINT = "spring.ai.mcp.server.streamable-http.mcp-endpoint";

	@Bean
	@ConditionalOnMissingBean
	BlogTools beansproutBlogTools(Posts posts, PostSource source, Links links, BeansproutProperties properties,
			Environment environment) {
		return new BlogTools(posts, source, site(properties, environment), links);
	}

	/** Only when the posts are a folder on disk: drafts never go into a jar. */
	@Bean
	@ConditionalOnMissingBean
	@Conditional(OnPostsFolderCondition.class)
	DraftTools beansproutDraftTools(Posts posts, BeansproutProperties properties, ResourceLoader resourceLoader) {
		try {
			return new DraftTools(resourceLoader.getResource(properties.posts().location()).getFile().toPath(), posts);
		}
		catch (IOException ex) {
			throw new UncheckedIOException("Could not open the posts folder " + properties.posts().location(), ex);
		}
	}

	@Bean
	@ConditionalOnMissingBean
	McpAccessFilter beansproutMcpAccessFilter(BeansproutProperties properties, Environment environment) {
		return new McpAccessFilter(environment.getProperty(ENDPOINT, "/mcp"), properties.mcp().token());
	}

	/**
	 * The site's scheme, host and port: {@code beansprout.site-url}, or the request's when the tool
	 * runs on a request thread, or this machine's port.
	 */
	private static Supplier<String> site(BeansproutProperties properties, Environment environment) {
		return () -> {
			String configured = properties.siteUrl();
			if (configured != null && !configured.isBlank()) {
				return configured.endsWith("/") ? configured.substring(0, configured.length() - 1) : configured;
			}
			if (RequestContextHolder.getRequestAttributes() != null) {
				return ServletUriComponentsBuilder.fromCurrentRequestUri().replacePath(null).replaceQuery(null).toUriString();
			}
			return "http://localhost:" + environment.getProperty("local.server.port", environment.getProperty("server.port", "8080"));
		};
	}

	/** Matches when {@code beansprout.posts.location} is a folder that exists on disk. */
	static class OnPostsFolderCondition extends SpringBootCondition {

		@Override
		public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
			String location = context.getEnvironment().getProperty("beansprout.posts.location", "classpath:posts/");
			Resource resource = context.getResourceLoader().getResource(location);
			try {
				if (location.startsWith("file:") && resource.isFile() && resource.getFile().isDirectory()) {
					return ConditionOutcome.match("posts are in a folder on disk: " + location);
				}
			}
			catch (IOException ex) {
				return ConditionOutcome.noMatch("could not open " + location + ": " + ex.getMessage());
			}
			return ConditionOutcome.noMatch("posts are not in a folder on disk: " + location);
		}

	}

}
