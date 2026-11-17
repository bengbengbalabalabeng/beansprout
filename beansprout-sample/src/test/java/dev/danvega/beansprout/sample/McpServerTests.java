package dev.danvega.beansprout.sample;

import java.util.Map;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/** A real MCP client talking to the sample app over streamable HTTP. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class McpServerTests {

	@Value("${local.server.port}")
	private int port;

	private McpSyncClient client() {
		McpSyncClient client = McpClient
			.sync(HttpClientStreamableHttpTransport.builder("http://localhost:" + port).endpoint("/mcp").build())
			.build();
		client.initialize();
		return client;
	}

	@Test
	@DisplayName("MCP-3 with the MCP starter, the sample offers list_posts, list_tags, find_posts_by_tag and read_post at /mcp")
	void tools() {
		try (McpSyncClient client = client()) {
			// The name comes from Beansprout's MCP defaults, so this also shows they ran in a real app.
			assertThat(client.getServerInfo().name()).isEqualTo("beansprout");
			assertThat(client.listTools().tools()).extracting(McpSchema.Tool::name)
				.contains("list_posts", "list_tags", "find_posts_by_tag", "read_post")
				.doesNotContain("create_draft");
		}
	}

	@Test
	@DisplayName("MCP-16 in the running app with no token, a request that came through a proxy is answered 403, however the path is written")
	void proxiedRequest() throws Exception {
		for (String path : new String[] { "/mcp", "/mcp;x=1" }) {
			java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder(java.net.URI.create("http://localhost:" + port + path))
				.header("Content-Type", "application/json")
				.header("Accept", "application/json, text/event-stream")
				.header("X-Forwarded-For", "203.0.113.7")
				.POST(java.net.http.HttpRequest.BodyPublishers.ofString("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\",\"params\":{}}"))
				.build();
			assertThat(java.net.http.HttpClient.newHttpClient()
				.send(request, java.net.http.HttpResponse.BodyHandlers.discarding())
				.statusCode()).as(path).isEqualTo(403);
		}
	}

	@Test
	@DisplayName("MCP-20 the sample answers an MCP client's list_posts call with its published posts")
	void listPosts() {
		try (McpSyncClient client = client()) {
			McpSchema.CallToolResult result = client.callTool(new McpSchema.CallToolRequest("list_posts", Map.of(), Map.of()));
			assertThat(result.isError()).isNotEqualTo(Boolean.TRUE);
			String text = result.content().toString();
			assertThat(text).contains("Hello, Beansprout", "hello-beansprout")
				.doesNotContain("a-draft", "Not ready yet", "2027-a-future-post", "A post from the future");
		}
	}

}
