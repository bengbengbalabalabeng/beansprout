package dev.danvega.beansprout.autoconfigure;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import dev.danvega.beansprout.mcp.BlogTools;
import dev.danvega.beansprout.mcp.DraftTools;
import dev.danvega.beansprout.mcp.McpAccessFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.DefaultResourceLoader;

import static org.assertj.core.api.Assertions.assertThat;

/** When Beansprout's MCP tools are there, and when they are not. */
class McpAutoConfigurationTests {

	@TempDir
	Path posts;

	private final WebApplicationContextRunner web = new WebApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(BeansproutAutoConfiguration.class, BeansproutMcpAutoConfiguration.class));

	@Test
	@DisplayName("MCP-1 without the MCP starter on the classpath, the app has no MCP tools and no MCP guard")
	void offWithoutTheStarter() {
		web.withClassLoader(new FilteredClassLoader("org.springframework.ai.mcp", "io.modelcontextprotocol"))
			.run(context -> {
				assertThat(context).hasNotFailed();
				assertThat(context).doesNotHaveBean(BlogTools.class)
					.doesNotHaveBean(DraftTools.class)
					.doesNotHaveBean(McpAccessFilter.class);
			});
	}

	@Test
	@DisplayName("MCP-1 an app with Spring AI's own MCP server but not the Beansprout MCP starter gets no Beansprout tools or guard")
	void offForAnAppsOwnServer() {
		web.withClassLoader(new FilteredClassLoader(new ClassPathResource(BeansproutMcpAutoConfiguration.MARKER)))
			.run(context -> assertThat(context).doesNotHaveBean(BlogTools.class).doesNotHaveBean(McpAccessFilter.class));
	}

	@Test
	@DisplayName("MCP-3 with the MCP starter, Spring AI serves over streamable HTTP and the server is named beansprout, unless the app says otherwise")
	void streamableByDefault() {
		StandardEnvironment environment = new StandardEnvironment();
		new McpDefaultsEnvironmentPostProcessor().postProcessEnvironment(environment, new SpringApplication());
		assertThat(environment.getProperty("spring.ai.mcp.server.protocol")).isEqualTo("STREAMABLE");
		assertThat(environment.getProperty("spring.ai.mcp.server.name")).isEqualTo("beansprout");

		StandardEnvironment chosen = new StandardEnvironment();
		chosen.getPropertySources().addFirst(new MapPropertySource("app", Map.of("spring.ai.mcp.server.protocol", "SSE")));
		new McpDefaultsEnvironmentPostProcessor().postProcessEnvironment(chosen, new SpringApplication());
		assertThat(chosen.getProperty("spring.ai.mcp.server.protocol")).isEqualTo("SSE");

		StandardEnvironment other = new StandardEnvironment();
		SpringApplication withoutStarter = new SpringApplication();
		withoutStarter.setResourceLoader(new DefaultResourceLoader(
				new FilteredClassLoader(new ClassPathResource(BeansproutMcpAutoConfiguration.MARKER))));
		new McpDefaultsEnvironmentPostProcessor().postProcessEnvironment(other, withoutStarter);
		assertThat(other.getProperty("spring.ai.mcp.server.protocol")).isNull();
	}

	@Test
	@DisplayName("MCP-2 the main starter does not depend on Spring AI, and autoconfigure only optionally")
	void starterHasNoSpringAi() throws IOException {
		assertThat(Files.readString(Path.of("../beansprout-spring-boot-starter/pom.xml"))).doesNotContain("spring-ai")
			.doesNotContain("modelcontextprotocol");
		String autoconfigure = Files.readString(Path.of("pom.xml"));
		Matcher dependency = Pattern.compile("<dependency>(.*?)</dependency>", Pattern.DOTALL).matcher(autoconfigure);
		int found = 0;
		while (dependency.find()) {
			String block = dependency.group(1);
			if (block.contains("spring-ai") || block.contains("modelcontextprotocol")) {
				found++;
				assertThat(block).as(block).containsAnyOf("<optional>true</optional>", "<scope>test</scope>");
			}
		}
		assertThat(found).isPositive();
	}

	@Test
	@DisplayName("MCP-9 with posts in a folder on disk, the app offers create_draft")
	void draftsForAFolder() {
		web.withPropertyValues("beansprout.posts.location=" + posts.toUri())
			.run(context -> assertThat(context).hasSingleBean(BlogTools.class).hasSingleBean(DraftTools.class));
	}

	@Test
	@DisplayName("MCP-10 with posts on the classpath, the app offers the read tools but not create_draft")
	void noDraftsFromTheClasspath() {
		web.run(context -> assertThat(context).hasSingleBean(BlogTools.class)
			.doesNotHaveBean(DraftTools.class)
			.hasSingleBean(McpAccessFilter.class));
		web.withPropertyValues("beansprout.posts.location=file:" + posts.resolve("missing") + "/")
			.run(context -> assertThat(context).doesNotHaveBean(DraftTools.class));
	}

	@Test
	@DisplayName("MCP-19 beansprout-bom manages beansprout-mcp-spring-boot-starter")
	void inTheBom() throws IOException {
		String bom = Files.readString(Path.of("../beansprout-bom/pom.xml"));
		assertThat(bom.substring(bom.indexOf("<dependencyManagement>"))).contains(
				"<artifactId>beansprout-mcp-spring-boot-starter</artifactId>");
	}

}
