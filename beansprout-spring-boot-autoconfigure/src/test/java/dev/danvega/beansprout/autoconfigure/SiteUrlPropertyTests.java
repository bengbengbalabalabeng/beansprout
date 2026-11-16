package dev.danvega.beansprout.autoconfigure;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class SiteUrlPropertyTests {

	private final ApplicationContextRunner runner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(BeansproutAutoConfiguration.class));

	@Test
	@DisplayName("SEO-16 a beansprout.site-url without http:// or https:// fails at startup with a clear message")
	void siteUrlNeedsAScheme() {
		runner.withPropertyValues("beansprout.site-url=example.com")
			.run(context -> assertThat(context).getFailure().rootCause()
				.hasMessageContaining("beansprout.site-url must start with http:// or https://"));
		runner.withPropertyValues("beansprout.site-url=https://example.com")
			.run(context -> assertThat(context).hasNotFailed());
		runner.run(context -> assertThat(context).hasNotFailed());
	}

}
