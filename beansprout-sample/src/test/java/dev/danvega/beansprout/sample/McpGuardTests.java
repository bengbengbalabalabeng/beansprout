package dev.danvega.beansprout.sample;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/** The guard in front of /mcp, in the running sample app, with a token set. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "beansprout.mcp.token=s3cret")
class McpGuardTests {

	private static final String INITIALIZE = """
			{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-06-18","capabilities":{},\
			"clientInfo":{"name":"test","version":"1"}}}""";

	@Value("${local.server.port}")
	private int port;

	private int post(String path, String... headers) throws IOException, InterruptedException {
		HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
			.header("Content-Type", "application/json")
			.header("Accept", "application/json, text/event-stream")
			.POST(HttpRequest.BodyPublishers.ofString(INITIALIZE));
		for (int i = 0; i < headers.length; i += 2) {
			request.header(headers[i], headers[i + 1]);
		}
		return HttpClient.newHttpClient().send(request.build(), HttpResponse.BodyHandlers.discarding()).statusCode();
	}

	@Test
	@DisplayName("MCP-17 in the running app, a request that sends the token as a Bearer header is answered")
	void withTheToken() throws Exception {
		assertThat(post("/mcp", "Authorization", "Bearer s3cret")).isEqualTo(200);
	}

	@Test
	@DisplayName("MCP-18 in the running app, a request without the token is answered 401, however the path is written")
	void withoutTheToken() throws Exception {
		assertThat(post("/mcp")).isEqualTo(401);
		assertThat(post("/mcp;x=1")).isEqualTo(401);
		assertThat(post("/%6Dcp")).isEqualTo(401);
		assertThat(post("/mcp", "Authorization", "Bearer wrong")).isEqualTo(401);
	}

}
