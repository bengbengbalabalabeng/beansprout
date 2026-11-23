package dev.danvega.beansprout.autoconfigure;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import dev.danvega.beansprout.view.Pages;
import dev.danvega.beansprout.view.Slots;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/** The slot files an app puts on its classpath, from specs/theme-slots.md. */
class SlotFilesTests {

	private static final String HEAD = "<script defer src=\"/stats.js\"></script>\n<!-- café -->\n";

	private static final String AFTER_POST = "<form action=\"/subscribe\"><button>Join</button></form>";

	private static final String FOOTER = "  <p>Made with <b>beans</b> &amp; care</p>\n";

	@TempDir
	Path classpath;

	private void write(String name, byte[] bytes) throws IOException {
		Path file = classpath.resolve("beansprout").resolve(name);
		Files.createDirectories(file.getParent());
		Files.write(file, bytes);
	}

	private void write(String name, String text) throws IOException {
		write(name, text.getBytes(StandardCharsets.UTF_8));
	}

	private ApplicationContextRunner runner() throws IOException {
		URLClassLoader loader = new URLClassLoader(new URL[] { classpath.toUri().toURL() }, getClass().getClassLoader());
		return new ApplicationContextRunner().withClassLoader(loader)
			.withConfiguration(AutoConfigurations.of(BeansproutAutoConfiguration.class))
			.withPropertyValues("beansprout.posts.location=classpath:no-such-folder/");
	}

	private static Slots slots(org.springframework.context.ApplicationContext context) {
		return context.getBean(Pages.class).archive().site().slots();
	}

	@Test
	@DisplayName("SLOT-1 beansprout/head.html on the classpath becomes the head slot, as written")
	void head() throws IOException {
		write("head.html", HEAD);
		runner().run(context -> assertThat(slots(context).head()).isEqualTo(HEAD));
	}

	@Test
	@DisplayName("SLOT-2 beansprout/after-post.html on the classpath becomes the after-post slot, as written")
	void afterPost() throws IOException {
		write("after-post.html", AFTER_POST);
		runner().run(context -> assertThat(slots(context).afterPost()).isEqualTo(AFTER_POST));
	}

	@Test
	@DisplayName("SLOT-3 beansprout/footer.html on the classpath becomes the footer slot, as written")
	void footer() throws IOException {
		write("footer.html", FOOTER);
		runner().run(context -> assertThat(slots(context).footer()).isEqualTo(FOOTER));
	}

	@Test
	@DisplayName("SLOT-5 a slot whose file is missing is empty, and the app starts")
	void missing() throws IOException {
		write("footer.html", FOOTER);
		runner().run(context -> {
			assertThat(context).hasNotFailed();
			assertThat(slots(context)).isEqualTo(new Slots("", "", FOOTER));
		});
		new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(BeansproutAutoConfiguration.class))
			.withPropertyValues("beansprout.posts.location=classpath:no-such-folder/")
			.run(context -> assertThat(slots(context)).isEqualTo(Slots.NONE));
	}

	@Test
	@DisplayName("SLOT-6 each slot's file is read once, at startup: a later change or removal is not seen")
	void readOnce() throws IOException {
		write("head.html", HEAD);
		write("footer.html", FOOTER);
		runner().run(context -> {
			assertThat(slots(context).head()).isEqualTo(HEAD);
			write("head.html", "<script src=\"/other.js\"></script>");
			Files.delete(classpath.resolve("beansprout/footer.html"));
			write("after-post.html", AFTER_POST);
			assertThat(slots(context)).isEqualTo(new Slots(HEAD, "", FOOTER));
			assertThat(context.getBean(Pages.class).index(1).orElseThrow().site().slots())
				.isEqualTo(new Slots(HEAD, "", FOOTER));
		});
	}

	@Test
	@DisplayName("SLOT-7 a slot file that is not UTF-8 stops the app at startup with a message that names the file")
	void notUtf8() throws IOException {
		write("after-post.html", new byte[] { '<', 'p', '>', (byte) 0xE9, 't', (byte) 0xE9, '<', '/', 'p', '>' });
		runner().run(context -> assertThat(context).getFailure().rootCause()
			.hasMessageContaining("beansprout/after-post.html")
			.hasMessageContaining("UTF-8"));
	}

}
