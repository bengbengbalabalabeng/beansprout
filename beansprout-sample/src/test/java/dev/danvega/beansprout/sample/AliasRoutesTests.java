package dev.danvega.beansprout.sample;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerResponse;

import static org.assertj.core.api.Assertions.assertThat;

/** Old addresses answered with a 301 to the post, as a real app serves them. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AliasRoutesTests {

	private final HttpClient http = HttpClient.newHttpClient();

	private ConfigurableApplicationContext app;

	/** A route of the app's own, at the path one post also names as an alias. Not scanned, so no other test sees it. */
	static class OwnRoute {

		@Bean
		RouterFunction<ServerResponse> ownRoute() {
			return RouterFunctions.route().GET("/own-route", request -> ServerResponse.ok().body("mine")).build();
		}

	}

	@BeforeAll
	void start() {
		app = new SpringApplicationBuilder(SampleApplication.class, OwnRoute.class).run("--server.port=0",
				"--beansprout.posts.location=classpath:test-posts-aliases/", "--beansprout.site-url=https://example.com");
	}

	@AfterAll
	void stop() {
		app.close();
	}

	private HttpResponse<String> send(ConfigurableApplicationContext context, String method, String path)
			throws IOException, InterruptedException {
		String url = "http://localhost:" + context.getEnvironment().getProperty("local.server.port") + path;
		return http.send(HttpRequest.newBuilder(URI.create(url)).method(method, HttpRequest.BodyPublishers.noBody()).build(),
				HttpResponse.BodyHandlers.ofString());
	}

	private HttpResponse<String> get(String path) throws IOException, InterruptedException {
		return send(app, "GET", path);
	}

	private void movedTo(HttpResponse<String> response, String location) {
		assertThat(response.statusCode()).as(response.uri().toString()).isEqualTo(301);
		assertThat(response.headers().firstValue("Location")).as(response.uri().toString()).contains(location);
	}

	@Test
	@DisplayName("ALIAS-1 a GET for an alias answers 301 to the post's address")
	void aliasRedirects() throws Exception {
		movedTo(get("/2019/03/14/hello.html"), "/blog/hello");
		movedTo(send(app, "HEAD", "/2019/03/14/hello.html"), "/blog/hello");
		movedTo(get("/old/hello?utm_source=feed"), "/blog/hello");
		assertThat(get("/blog/hello").statusCode()).isEqualTo(200);
	}

	@Test
	@DisplayName("ALIAS-1 an alias under the blog's own path answers 301 too")
	void aliasUnderTheBlog() throws Exception {
		movedTo(get("/blog/hi"), "/blog/hello");
		movedTo(get("/blog/hi/"), "/blog/hello");
		assertThat(get("/blog/nothing-here").statusCode()).isEqualTo(404);
	}

	@Test
	@DisplayName("ALIAS-1 an alias written with %XX escapes answers the request for that path")
	void percentEscapes() throws Exception {
		movedTo(get("/caf%C3%A9.html"), "/blog/clash");
		movedTo(get("/blog/clash.html"), "/blog/clash");
	}

	@Test
	@DisplayName("ALIAS-7 an alias at /robots.txt leaves the blog's robots.txt answering")
	void robotsTxt() throws Exception {
		HttpResponse<String> response = get("/robots.txt");

		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(response.body()).startsWith("User-agent: *");
	}

	@Test
	@DisplayName("ALIAS-2 a path in redirect_from answers 301 to the post")
	void redirectFrom() throws Exception {
		movedTo(get("/old/hello"), "/blog/hello");
	}

	@Test
	@DisplayName("ALIAS-4 an alias answers with or without a trailing slash")
	void trailingSlash() throws Exception {
		movedTo(get("/posts/hello/"), "/blog/hello");
		movedTo(get("/posts/hello"), "/blog/hello");
		movedTo(get("/old/hello/"), "/blog/hello");
	}

	@Test
	@DisplayName("ALIAS-5 an alias answers under the app's context path")
	void contextPath() throws Exception {
		try (ConfigurableApplicationContext under = new SpringApplicationBuilder(SampleApplication.class).run(
				"--server.port=0", "--server.servlet.context-path=/app",
				"--beansprout.posts.location=classpath:test-posts-aliases/")) {
			movedTo(send(under, "GET", "/app/old/hello"), "/app/blog/hello");
			movedTo(send(under, "GET", "/app/blog/2024-05-01-dated"), "/app/blog/dated");
			assertThat(send(under, "GET", "/old/hello").statusCode()).isEqualTo(404);
		}
	}

	@Test
	@DisplayName("ALIAS-9 a draft's alias has no answer")
	void draft() throws Exception {
		assertThat(get("/draft-old").statusCode()).isEqualTo(404);
	}

	@Test
	@DisplayName("ALIAS-10 the app's own mapping answers an alias's path")
	void appMapping() throws Exception {
		HttpResponse<String> response = get("/own-route");

		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(response.body()).isEqualTo("mine");
	}

	@Test
	@DisplayName("ALIAS-11 the app's static file answers an alias's path")
	void staticFile() throws Exception {
		HttpResponse<String> response = get("/export-test/about.html");

		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(response.body()).contains("The app's own page.");
	}

	@Test
	@DisplayName("ALIAS-11 under the blog's path an app's static file at an alias's path answers, not the alias")
	void staticFileUnderTheBlog() throws Exception {
		HttpResponse<String> response = get("/blog/own.html");

		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(response.headers().firstValue("Location")).isEmpty();
		assertThat(response.headers().firstValue("Content-Type")).hasValueSatisfying(type -> assertThat(type).startsWith("text/html"));
		assertThat(response.body()).contains("The app's own file under the blog path.");
	}

	@Test
	@DisplayName("ALIAS-11 a static file two folders under the blog's path answers too, not the alias")
	void staticFileInAFolderUnderTheBlog() throws Exception {
		HttpResponse<String> response = get("/blog/old/own.css");

		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(response.headers().firstValue("Location")).isEmpty();
		assertThat(response.headers().firstValue("Content-Type")).hasValueSatisfying(type -> assertThat(type).startsWith("text/css"));
		assertThat(response.body()).contains("own-css");
	}

	@Test
	@DisplayName("ALIAS-12 a post's old dated address answers 301 to its address without the date")
	void datedName() throws Exception {
		movedTo(get("/blog/2024-05-01-dated"), "/blog/dated");
		assertThat(get("/blog/dated").statusCode()).isEqualTo(200);
	}

	@Test
	@DisplayName("ALIAS-15 the served sitemap leaves aliases out")
	void sitemap() throws Exception {
		String sitemap = get("/blog/sitemap.xml").body();

		assertThat(sitemap).contains("<loc>https://example.com/blog/hello</loc>")
			.doesNotContain("hello.html", "/old/hello", "/blog/hi<", "2024-05-01-dated");
	}

}
