package dev.danvega.beansprout.autoconfigure;

import java.util.Optional;

import dev.danvega.beansprout.code.CodeHighlighter;
import dev.danvega.beansprout.content.Posts;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class CodeHighlighterConfigurationTests {

	@TempDir
	java.nio.file.Path dir;

	private ApplicationContextRunner runner() throws java.io.IOException {
		java.nio.file.Files.writeString(dir.resolve("code.md"),
				"---\ntitle: Code\ndate: 2026-01-01\n---\n```java\npublic class A {}\n```\n");
		return new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(BeansproutAutoConfiguration.class))
			.withPropertyValues("beansprout.posts.location=file:" + dir + "/");
	}

	private static String html(org.springframework.context.ApplicationContext context) {
		return context.getBean(Posts.class).find("code").orElseThrow().html();
	}

	@Test
	@DisplayName("HL-7 the built-in highlighter is on by default")
	void onByDefault() throws Exception {
		runner().run(context -> assertThat(html(context)).contains("<span class=\"token keyword\">public</span>"));
	}

	@Test
	@DisplayName("HL-9 an app's own CodeHighlighter bean is used instead of the built-in one")
	void appHighlighter() throws Exception {
		runner().withUserConfiguration(Shouting.class)
			.run(context -> assertThat(html(context)).contains("<code class=\"language-java\">PUBLIC CLASS A {}"));
	}

	@Test
	@DisplayName("HL-10 beansprout.code.highlight=false leaves every fence unhighlighted")
	void turnedOff() throws Exception {
		runner().withPropertyValues("beansprout.code.highlight=false")
			.run(context -> assertThat(html(context)).contains("<code class=\"language-java\">public class A {}")
				.doesNotContain("token"));
	}

	@Test
	@DisplayName("HL-10 beansprout.code.highlight=false also turns off an app's own highlighter")
	void turnedOffWithAppHighlighter() throws Exception {
		runner().withUserConfiguration(Shouting.class).withPropertyValues("beansprout.code.highlight=false")
			.run(context -> assertThat(html(context)).contains("<code class=\"language-java\">public class A {}"));
	}

	@Configuration(proxyBeanMethods = false)
	static class Shouting {

		@Bean
		CodeHighlighter shouting() {
			return (language, code) -> Optional.of(code.toUpperCase(java.util.Locale.ROOT));
		}

	}

}
