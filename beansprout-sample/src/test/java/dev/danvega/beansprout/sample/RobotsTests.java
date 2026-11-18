package dev.danvega.beansprout.sample;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** robots.txt at the site's root, served by the sample app. */
@ExtendWith(OutputCaptureExtension.class)
class RobotsTests {

	private static final String POSTS = "--beansprout.posts.location=classpath:test-posts/";

	@Nested
	@SpringBootTest(properties = "beansprout.posts.location=classpath:test-posts/")
	@AutoConfigureMockMvc
	class Defaults {

		@Autowired
		private MockMvcTester mvc;

		@Test
		@DisplayName("ROBOTS-1 /robots.txt is served at the site's root as text/plain in UTF-8")
		void served() {
			assertThat(mvc.get().uri("/robots.txt")).hasStatusOk().hasContentType("text/plain;charset=UTF-8");
		}

		@Test
		@DisplayName("ROBOTS-2 the file starts with a User-agent: * group that allows every path")
		void allowsAll() {
			assertThat(mvc.get().uri("/robots.txt")).bodyText().startsWith("User-agent: *\nAllow: /\n");
		}

		@Test
		@DisplayName("ROBOTS-3 the file ends with a Sitemap: line for the blog's sitemap")
		void sitemapLine() {
			assertThat(mvc.get().uri("/robots.txt")).bodyText().endsWith("\nSitemap: http://localhost/blog/sitemap.xml\n");
		}

		@Test
		@DisplayName("ROBOTS-4 without beansprout.site-url, the sitemap address uses the request's scheme, host and port")
		void addressFromRequest() {
			assertThat(mvc.get().uri("http://blog.example:8080/robots.txt")).bodyText()
				.contains("Sitemap: http://blog.example:8080/blog/sitemap.xml");
		}

		@Test
		@DisplayName("ROBOTS-8 without beansprout.robots.ai-training, no crawler is named")
		void allowByDefault() {
			assertThat(mvc.get().uri("/robots.txt")).bodyText()
				.isEqualTo("User-agent: *\nAllow: /\n\nSitemap: http://localhost/blog/sitemap.xml\n");
		}

	}

	@Nested
	@SpringBootTest(properties = { "beansprout.posts.location=classpath:test-posts/",
			"beansprout.site-url=https://beans.example.com/", "beansprout.robots.ai-training=block" })
	@AutoConfigureMockMvc
	class BlockTraining {

		@Autowired
		private MockMvcTester mvc;

		@Test
		@DisplayName("ROBOTS-4 with beansprout.site-url set, the sitemap address uses it, not the request's")
		void siteUrl() {
			assertThat(mvc.get().uri("http://internal-host:9000/robots.txt")).bodyText()
				.contains("Sitemap: https://beans.example.com/blog/sitemap.xml")
				.doesNotContain("internal-host");
		}

		@Test
		@DisplayName("ROBOTS-5 with ai-training: block, one group names the five training crawlers")
		void blocksTraining() {
			assertThat(mvc.get().uri("/robots.txt")).bodyText()
				.isEqualTo("""
						User-agent: *
						Allow: /

						User-agent: GPTBot
						User-agent: ClaudeBot
						User-agent: Google-Extended
						User-agent: Applebot-Extended
						User-agent: CCBot
						Disallow: /blog

						Sitemap: https://beans.example.com/blog/sitemap.xml
						""");
		}

		@Test
		@DisplayName("ROBOTS-16 with ai-training: block, the training crawlers' group disallows the blog")
		void disallowsTheBlog() {
			assertThat(mvc.get().uri("/robots.txt")).bodyText()
				.contains("User-agent: CCBot\nDisallow: /blog\n\nSitemap:");
		}

		@Test
		@DisplayName("ROBOTS-6 with ai-training: block, search crawlers are not named, so they follow the * group")
		void searchStaysAllowed() {
			assertThat(mvc.get().uri("/robots.txt")).bodyText()
				.doesNotContain("OAI-SearchBot", "Claude-SearchBot", "PerplexityBot", "ChatGPT-User", "Claude-User");
		}

	}

	@Nested
	@SpringBootTest(properties = { "beansprout.posts.location=classpath:test-posts/",
			"beansprout.robots.ai-training=allow" })
	@AutoConfigureMockMvc
	class AllowTraining {

		@Autowired
		private MockMvcTester mvc;

		@Test
		@DisplayName("ROBOTS-7 with ai-training: allow, no crawler is named")
		void namesNoCrawler() {
			assertThat(mvc.get().uri("/robots.txt")).bodyText()
				.isEqualTo("User-agent: *\nAllow: /\n\nSitemap: http://localhost/blog/sitemap.xml\n");
		}

	}

	@Nested
	@SpringBootTest(properties = { "beansprout.posts.location=classpath:test-posts/",
			"spring.web.resources.static-locations=classpath:/robots-own/,classpath:/static/" })
	@AutoConfigureMockMvc
	class OwnStaticFile {

		@Autowired
		private MockMvcTester mvc;

		@Test
		@DisplayName("ROBOTS-10 an app with a robots.txt in its static locations gets its own file, not Beansprout's")
		void appFileWins() {
			assertThat(mvc.get().uri("/robots.txt")).hasStatusOk().bodyText()
				.isEqualTo("User-agent: *\nDisallow: /private\n");
		}

	}

	@Nested
	@SpringBootTest(properties = "beansprout.posts.location=classpath:test-posts/")
	@AutoConfigureMockMvc
	@Import(OwnController.Robots.class)
	class OwnController {

		@Autowired
		private MockMvcTester mvc;

		@Test
		@DisplayName("ROBOTS-11 an app that maps /robots.txt itself starts, and its controller answers")
		void appControllerWins() {
			assertThat(mvc.get().uri("/robots.txt")).hasStatusOk().bodyText().isEqualTo("the app's own\n");
		}

		@RestController
		static class Robots {

			@GetMapping("/robots.txt")
			String robots() {
				return "the app's own\n";
			}

		}

	}

	@Nested
	@SpringBootTest(properties = { "beansprout.posts.location=classpath:test-posts/", "beansprout.robots.enabled=false" })
	@AutoConfigureMockMvc
	class Disabled {

		@Autowired
		private MockMvcTester mvc;

		@Test
		@DisplayName("ROBOTS-14 with beansprout.robots.enabled=false, no robots.txt is served")
		void off() {
			assertThat(mvc.get().uri("/robots.txt")).hasStatus(404);
		}

	}

	@Test
	@DisplayName("ROBOTS-9 an unknown ai-training value stops the start and lists allow and block")
	void unknownValue(CapturedOutput output) {
		assertThatThrownBy(() -> new SpringApplicationBuilder(SampleApplication.class)
			.run("--server.port=0", POSTS, "--beansprout.robots.ai-training=maybe")
			.close()).isInstanceOf(RuntimeException.class);
		assertThat(output).contains("beansprout.robots.ai-training must be allow or block, not 'maybe'");
	}

	@Test
	@DisplayName("ROBOTS-9 an empty ai-training value stops the start too, rather than meaning allow")
	void emptyValue(CapturedOutput output) {
		assertThatThrownBy(() -> new SpringApplicationBuilder(SampleApplication.class)
			.run("--server.port=0", POSTS, "--beansprout.robots.ai-training=")
			.close()).isInstanceOf(RuntimeException.class);
		assertThat(output).contains("beansprout.robots.ai-training must be allow or block, not ''");
	}

	@Test
	@DisplayName("ROBOTS-12 with a servlet context path, no robots.txt is served, at the root or under the path")
	void contextPathOff() throws IOException, InterruptedException {
		try (ConfigurableApplicationContext app = new SpringApplicationBuilder(SampleApplication.class)
			.run("--server.port=0", POSTS, "--server.servlet.context-path=/site")) {
			assertThat(status(app, "/robots.txt")).isEqualTo(404);
			assertThat(status(app, "/site/robots.txt")).isEqualTo(404);
			assertThat(status(app, "/site/blog")).isEqualTo(200);
		}
	}

	@Test
	@DisplayName("ROBOTS-13 with a servlet context path, the start logs once that robots.txt is off and why")
	void contextPathLogged(CapturedOutput output) {
		new SpringApplicationBuilder(SampleApplication.class)
			.run("--server.port=0", POSTS, "--server.servlet.context-path=/site")
			.close();
		String log = output.getAll();
		String line = "robots.txt is off";
		assertThat(log).contains(line, "only at the root");
		assertThat(log.indexOf(line)).isEqualTo(log.lastIndexOf(line));
	}

	private static int status(ConfigurableApplicationContext app, String path) throws IOException, InterruptedException {
		String port = app.getEnvironment().getRequiredProperty("local.server.port");
		try (HttpClient http = HttpClient.newHttpClient()) {
			HttpRequest request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path)).GET().build();
			return http.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
		}
	}

}
