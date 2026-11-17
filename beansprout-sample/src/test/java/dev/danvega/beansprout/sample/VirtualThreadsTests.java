package dev.danvega.beansprout.sample;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

/** The sample app over real HTTP, with the virtual threads its application.yaml turns on. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(VirtualThreadsTests.ThreadProbe.class)
class VirtualThreadsTests {

	@Value("${local.server.port}")
	private int port;

	@Test
	@DisplayName("THREADS-3 the sample serves the blog on virtual threads")
	void blogOnVirtualThreads() {
		RestClient http = RestClient.create("http://localhost:" + port);

		assertThat(http.get().uri("/test/thread").retrieve().body(String.class))
			.as("the request ran on a virtual thread")
			.isEqualTo("virtual");
		ResponseEntity<String> blog = http.get().uri("/blog").retrieve().toEntity(String.class);
		assertThat(blog.getStatusCode().value()).isEqualTo(200);
		assertThat(blog.getBody()).contains("Beansprout Notes");
	}

	@TestConfiguration(proxyBeanMethods = false)
	@RestController
	static class ThreadProbe {

		@GetMapping("/test/thread")
		String thread() {
			return Thread.currentThread().isVirtual() ? "virtual" : "platform";
		}

	}

}
