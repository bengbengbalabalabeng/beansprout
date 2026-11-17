package dev.danvega.beansprout.sample;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/** create_draft over a real MCP call, with the posts in a folder on disk. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class McpDraftTests {

	private static final Path POSTS = folder();

	@Value("${local.server.port}")
	private int port;

	private static Path folder() {
		try {
			Path posts = Files.createTempDirectory("beansprout-mcp-posts");
			Files.writeString(posts.resolve("hello.md"), "---\ntitle: Hello\ndate: 2026-11-01\n---\nHi.\n");
			return posts;
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	@DynamicPropertySource
	static void posts(DynamicPropertyRegistry registry) {
		registry.add("beansprout.posts.location", () -> POSTS.toUri().toString());
	}

	@Test
	@DisplayName("MCP-11 an MCP client's create_draft call writes a draft file in the posts folder, and a second call with the same title is refused")
	void createDraftOverMcp() throws IOException {
		try (McpSyncClient client = McpClient
			.sync(HttpClientStreamableHttpTransport.builder("http://localhost:" + port).endpoint("/mcp").build())
			.build()) {
			client.initialize();
			assertThat(client.listTools().tools()).extracting(McpSchema.Tool::name).contains("create_draft");

			Map<String, Object> arguments = Map.of("title", "Notes from the road", "markdown", "Written by an agent.",
					"tags", List.of("travel"));
			McpSchema.CallToolResult first = client.callTool(new McpSchema.CallToolRequest("create_draft", arguments, Map.of()));
			assertThat(first.isError()).isNotEqualTo(Boolean.TRUE);
			assertThat(Files.readString(POSTS.resolve("notes-from-the-road.md"))).contains("title: \"Notes from the road\"",
					"draft: true", "Written by an agent.");

			McpSchema.CallToolResult again = client.callTool(new McpSchema.CallToolRequest("create_draft", arguments, Map.of()));
			assertThat(again.isError()).isTrue();
			assertThat(again.content().toString()).contains("notes-from-the-road.md");
		}
	}

}
