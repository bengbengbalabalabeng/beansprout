package dev.danvega.beansprout.mcp;

import java.io.IOException;

import jakarta.servlet.ServletException;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

/** Who may call the MCP server at /mcp. */
class McpAccessFilterTests {

	/** The status the filter answers, or 200 when it lets the request through. */
	private int call(@Nullable String token, String remote, String... headers) throws ServletException, IOException {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/mcp");
		request.setRemoteAddr(remote);
		for (int i = 0; i < headers.length; i += 2) {
			request.addHeader(headers[i], headers[i + 1]);
		}
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain chain = new MockFilterChain();
		new McpAccessFilter("/mcp", token).doFilter(request, response, chain);
		return chain.getRequest() != null ? 200 : response.getStatus();
	}

	@Test
	@DisplayName("MCP-15 with no token set, requests from the same machine are answered")
	void localWithoutToken() throws Exception {
		assertThat(call(null, "127.0.0.1")).isEqualTo(200);
		assertThat(call(null, "::1")).isEqualTo(200);
		assertThat(call("", "127.0.0.1")).isEqualTo(200);
	}

	@Test
	@DisplayName("MCP-16 with no token set, a request from another machine or through a proxy is answered 403")
	void remoteWithoutToken() throws Exception {
		assertThat(call(null, "203.0.113.7")).isEqualTo(403);
		assertThat(call(null, "10.0.0.5")).isEqualTo(403);
		assertThat(call(null, "127.0.0.1", "X-Forwarded-For", "203.0.113.7")).isEqualTo(403);
		assertThat(call(null, "127.0.0.1", "Forwarded", "for=203.0.113.7")).isEqualTo(403);
	}

	@Test
	@DisplayName("MCP-17 with a token set, a request that sends it as a Bearer header is answered from any machine")
	void withToken() throws Exception {
		assertThat(call("s3cret", "203.0.113.7", "Authorization", "Bearer s3cret")).isEqualTo(200);
		assertThat(call("s3cret", "127.0.0.1", "Authorization", "Bearer s3cret", "X-Forwarded-For", "203.0.113.7"))
			.isEqualTo(200);
	}

	@Test
	@DisplayName("MCP-18 with a token set, a request without it, or with a wrong one, is answered 401, even from the same machine")
	void withoutTheToken() throws Exception {
		assertThat(call("s3cret", "127.0.0.1")).isEqualTo(401);
		assertThat(call("s3cret", "203.0.113.7", "Authorization", "Bearer wrong")).isEqualTo(401);
		assertThat(call("s3cret", "203.0.113.7", "Authorization", "s3cret")).isEqualTo(401);
	}

	@Test
	@DisplayName("MCP-18 path parameters, encoding, case and a trailing slash do not get past the guard")
	void noWayAround() throws Exception {
		for (String uri : new String[] { "/mcp;x=1", "/mcp;jsessionid=abc", "/%6Dcp", "/mcp/", "/MCP", "/mcp/;x=1/sub" }) {
			MockHttpServletRequest request = new MockHttpServletRequest("POST", uri);
			request.setRemoteAddr("203.0.113.7");
			MockHttpServletResponse response = new MockHttpServletResponse();
			MockFilterChain chain = new MockFilterChain();
			new McpAccessFilter("/mcp", "s3cret").doFilter(request, response, chain);
			assertThat(chain.getRequest()).as(uri).isNull();
			assertThat(response.getStatus()).as(uri).isEqualTo(401);
		}
	}

	@Test
	@DisplayName("MCP-18 with the app's servlet mapped under a path, the MCP endpoint there is still guarded")
	void underAServletPath() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/mcp");
		request.setServletPath("/api");
		request.setPathInfo("/mcp");
		request.setRemoteAddr("203.0.113.7");
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain chain = new MockFilterChain();
		new McpAccessFilter("/mcp", "s3cret").doFilter(request, response, chain);
		assertThat(chain.getRequest()).isNull();
		assertThat(response.getStatus()).isEqualTo(401);
	}

	@Test
	@DisplayName("MCP-17 the Bearer scheme is read in any case, as HTTP says")
	void bearerInAnyCase() throws Exception {
		assertThat(call("s3cret", "203.0.113.7", "Authorization", "bearer s3cret")).isEqualTo(200);
		assertThat(call("s3cret", "203.0.113.7", "Authorization", "BEARER s3cret")).isEqualTo(200);
		assertThat(call("s3cret", "203.0.113.7", "Authorization", "Bearer S3CRET")).isEqualTo(401);
	}

	@Test
	@DisplayName("MCP-16 the guard covers only the MCP endpoint, so blog pages stay open")
	void onlyTheEndpoint() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/blog");
		request.setRemoteAddr("203.0.113.7");
		MockFilterChain chain = new MockFilterChain();
		new McpAccessFilter("/mcp", null).doFilter(request, new MockHttpServletResponse(), chain);
		assertThat(chain.getRequest()).isNotNull();
	}

}
