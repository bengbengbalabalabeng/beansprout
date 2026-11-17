package dev.danvega.beansprout.content;

import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PostsThreadsTests {

	private final Clock clock = Clock.fixed(Instant.parse("2026-11-16T12:00:00Z"), ZoneOffset.UTC);

	private final AtomicInteger reads = new AtomicInteger();

	private Posts posts(CountDownLatch gate) {
		PostSource source = () -> {
			reads.incrementAndGet();
			try {
				gate.await(5, TimeUnit.SECONDS);
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
			return List.of(new MarkdownFile("hello.md", "---\ntitle: Hello\ndate: 2026-11-01\n---\nHi.\n"));
		};
		return new Posts(source, new PostParser(ZoneOffset.UTC), clock, false);
	}

	@Test
	@DisplayName("THREADS-1 many virtual threads asking at once read the posts files once")
	void readOnceUnderLoad() throws Exception {
		CountDownLatch gate = new CountDownLatch(1);
		Posts posts = posts(gate);
		List<CompletableFuture<List<Post>>> calls = new ArrayList<>();
		try (ExecutorService threads = Executors.newVirtualThreadPerTaskExecutor()) {
			for (int i = 0; i < 200; i++) {
				calls.add(CompletableFuture.supplyAsync(posts::published, threads));
			}
			Thread.sleep(100);
			gate.countDown();
			for (CompletableFuture<List<Post>> call : calls) {
				assertThat(call.get(5, TimeUnit.SECONDS)).extracting(Post::title).containsExactly("Hello");
			}
		}
		assertThat(reads).hasValue(1);
	}

	@Test
	@DisplayName("THREADS-4 with reload on, calls at the same time read the posts files one after another")
	void reloadReadsOneAtATime() throws Exception {
		AtomicInteger inside = new AtomicInteger();
		AtomicInteger most = new AtomicInteger();
		PostSource slow = () -> {
			most.accumulateAndGet(inside.incrementAndGet(), Math::max);
			reads.incrementAndGet();
			try {
				Thread.sleep(20);
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
			inside.decrementAndGet();
			return List.of(new MarkdownFile("hello.md", "---\ntitle: Hello\ndate: 2026-11-01\n---\nHi.\n"));
		};
		Posts posts = new Posts(slow, new PostParser(ZoneOffset.UTC), clock, true);
		List<CompletableFuture<List<Post>>> calls = new ArrayList<>();
		try (ExecutorService threads = Executors.newVirtualThreadPerTaskExecutor()) {
			for (int i = 0; i < 20; i++) {
				calls.add(CompletableFuture.supplyAsync(posts::published, threads));
			}
			for (CompletableFuture<List<Post>> call : calls) {
				assertThat(call.get(10, TimeUnit.SECONDS)).extracting(Post::title).containsExactly("Hello");
			}
		}
		assertThat(reads).as("every call reads the files again").hasValue(20);
		assertThat(most).as("reads at the same time").hasValue(1);
	}

	@Test
	@DisplayName("THREADS-2 once read, posts are served without waiting, even while another thread holds every lock Posts has")
	void noWaitingOnceRead() throws Exception {
		CountDownLatch open = new CountDownLatch(0);
		Posts posts = posts(open);
		posts.published();

		// The lock Posts reads under is private, so the test reaches it by reflection.
		Field field = Posts.class.getDeclaredField("reading");
		field.setAccessible(true);
		Object reading = field.get(posts);

		CountDownLatch locked = new CountDownLatch(1);
		CountDownLatch release = new CountDownLatch(1);
		Thread holder = Thread.ofVirtual().start(() -> {
			synchronized (posts) {
				synchronized (reading) {
					locked.countDown();
					try {
						release.await(5, TimeUnit.SECONDS);
					}
					catch (InterruptedException ex) {
						Thread.currentThread().interrupt();
					}
				}
			}
		});
		ExecutorService threads = Executors.newVirtualThreadPerTaskExecutor();
		try {
			assertThat(locked.await(5, TimeUnit.SECONDS)).isTrue();
			CompletableFuture<List<Post>> call = CompletableFuture.supplyAsync(posts::published, threads);
			assertThat(call.get(2, TimeUnit.SECONDS)).extracting(Post::title).containsExactly("Hello");
		}
		finally {
			release.countDown();
			holder.join();
			threads.close();
		}
		assertThat(reads).hasValue(1);
	}

}
