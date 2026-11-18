package dev.danvega.beansprout.sample;

import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The export verifier. The sample's own posts are exported, then the live app is crawled from its
 * sitemap the way {@code SitemapCrawlTests} does it. Every page and every internal target the
 * crawl reaches must have an exported file with the same bytes.
 */
class ExportMatchesLiveTests {

	private static final String SITE = "https://example.com";

	private static final String SITEMAP = "/blog/sitemap.xml";

	private final HttpClient http = HttpClient.newHttpClient();

	@Test
	@DisplayName("EXPORT-31 every crawled page and target has an exported file with the bytes the live app answers")
	void bytesMatch() throws Exception {
		Path folder = Files.createTempDirectory("beansprout-export").resolve("site");
		new SpringApplicationBuilder(SampleApplication.class)
			.run("--beansprout.export.enabled=true", "--beansprout.export.output=" + folder, "--beansprout.site-url=" + SITE)
			.close();

		try (ConfigurableApplicationContext live = new SpringApplicationBuilder(SampleApplication.class)
			.run("--server.port=0", "--beansprout.site-url=" + SITE)) {
			String base = "http://localhost:" + live.getEnvironment().getProperty("local.server.port");
			SitemapCrawl.Result crawl = new SitemapCrawl(base).crawl(SITEMAP);
			assertThat(crawl.passed()).as("the live crawl").isTrue();

			Set<String> addresses = new LinkedHashSet<>(crawl.sitemapPaths());
			addresses.addAll(crawl.targets().keySet());
			addresses.add(SITEMAP);
			assertThat(addresses).contains("/beansprout/code.css", "/blog/feed.xml", "/images/beansprout.png",
					"/blog/posts-with-their-pictures/cover.png");

			for (String address : addresses) {
				HttpResponse<byte[]> response = http.send(HttpRequest.newBuilder(URI.create(base + address)).build(),
						HttpResponse.BodyHandlers.ofByteArray());
				assertThat(response.statusCode()).as(address).isEqualTo(200);
				boolean html = response.headers().firstValue("Content-Type").orElse("").startsWith("text/html");
				String name = URLDecoder.decode(address.substring(1).replace("+", "%2B"), StandardCharsets.UTF_8);
				Path file = folder.resolve(html ? name + ".html" : name);
				assertThat(file).as(address).exists();
				assertThat(Files.readAllBytes(file)).as(address).isEqualTo(response.body());
			}
		}
	}

}
