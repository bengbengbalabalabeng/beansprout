package dev.danvega.beansprout.mcp;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.Nullable;

import org.springframework.http.server.PathContainer;
import org.springframework.http.server.RequestPath;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Who may call the MCP server. With no token set, only requests from the same machine, and never
 * one that came through a proxy, since a proxy on the same machine makes public traffic look local.
 * With a token set, any machine that sends it as a Bearer header. Blog pages are not touched.
 */
public final class McpAccessFilter extends OncePerRequestFilter {

	private final String endpoint;

	private final byte @Nullable [] token;

	/**
	 * Creates the filter.
	 *
	 * @param endpoint the MCP endpoint's path, like {@code /mcp}
	 * @param token {@code beansprout.mcp.token}, or null or blank for same-machine requests only
	 */
	public McpAccessFilter(String endpoint, @Nullable String token) {
		String trimmed = endpoint.endsWith("/") ? endpoint.substring(0, endpoint.length() - 1) : endpoint;
		this.endpoint = trimmed.toLowerCase(Locale.ROOT);
		this.token = token == null || token.isBlank() ? null : token.strip().getBytes(StandardCharsets.UTF_8);
	}

	/**
	 * Skips only requests that are surely not for the endpoint. The path is read the way Spring MVC
	 * routes it: segments decoded, {@code ;} parameters dropped, empty segments skipped, so
	 * {@code /mcp;x=1}, {@code /%6Dcp} and {@code /mcp/} are all guarded. The path inside the
	 * servlet's mapping is checked too, so with {@code spring.mvc.servlet.path=/api} the endpoint at
	 * {@code /api/mcp} is guarded. Case is ignored, and a path that cannot be read is guarded.
	 */
	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		StringBuilder routed = new StringBuilder();
		try {
			for (PathContainer.Element element : RequestPath.parse(request.getRequestURI(), request.getContextPath())
				.pathWithinApplication()
				.elements()) {
				if (element instanceof PathContainer.PathSegment segment && !segment.valueToMatch().isEmpty()) {
					routed.append('/').append(segment.valueToMatch());
				}
			}
		}
		catch (IllegalArgumentException ex) {
			return false;
		}
		String pathInfo = request.getPathInfo();
		return !endpoint(routed.toString()) && (pathInfo == null || !endpoint(decoded(pathInfo)));
	}

	private boolean endpoint(String path) {
		String lower = path.toLowerCase(Locale.ROOT);
		return lower.equals(endpoint) || lower.startsWith(endpoint + "/");
	}

	/** A decoded path with {@code ;} parameters and empty segments dropped. */
	private static String decoded(String path) {
		StringBuilder out = new StringBuilder();
		for (String segment : path.split("/", -1)) {
			int semicolon = segment.indexOf(';');
			String value = semicolon >= 0 ? segment.substring(0, semicolon) : segment;
			if (!value.isEmpty()) {
				out.append('/').append(value);
			}
		}
		return out.toString();
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		byte[] expected = token;
		if (expected != null) {
			if (!bearer(request.getHeader("Authorization"), expected)) {
				response.setHeader("WWW-Authenticate", "Bearer");
				response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Send beansprout.mcp.token as a Bearer token.");
				return;
			}
		}
		else if (request.getHeader("Forwarded") != null || request.getHeader("X-Forwarded-For") != null
				|| !local(request.getRemoteAddr())) {
			response.sendError(HttpServletResponse.SC_FORBIDDEN,
					"The MCP server only answers this machine. Set beansprout.mcp.token to call it from elsewhere.");
			return;
		}
		chain.doFilter(request, response);
	}

	/** True when the header is {@code Bearer <token>}, the scheme in any case, the token exactly. */
	private static boolean bearer(@Nullable String header, byte[] expected) {
		if (header == null) {
			return false;
		}
		String value = header.strip();
		int space = value.indexOf(' ');
		if (space < 0 || !value.substring(0, space).equalsIgnoreCase("Bearer")) {
			return false;
		}
		return MessageDigest.isEqual(expected, value.substring(space + 1).strip().getBytes(StandardCharsets.UTF_8));
	}

	/** True for a loopback address. The remote address is an IP literal, so nothing is looked up. */
	private static boolean local(@Nullable String address) {
		if (address == null) {
			return false;
		}
		try {
			return InetAddress.ofLiteral(address).isLoopbackAddress();
		}
		catch (IllegalArgumentException ex) {
			return false;
		}
	}

}
