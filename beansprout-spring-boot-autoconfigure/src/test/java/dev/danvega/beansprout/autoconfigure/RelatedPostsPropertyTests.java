package dev.danvega.beansprout.autoconfigure;

import dev.danvega.beansprout.view.Pages;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/** The beansprout.related-posts setting, from specs/related-posts.md. */
class RelatedPostsPropertyTests {

	private final ApplicationContextRunner runner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(BeansproutAutoConfiguration.class));

	@Test
	@DisplayName("RELATED-7 beansprout.related-posts is 3 when not set")
	void threeByDefault() {
		runner.run(context -> assertThat(context.getBean(Pages.class).settings().relatedPosts()).isEqualTo(3));
		runner.withPropertyValues("beansprout.related-posts=5")
			.run(context -> assertThat(context.getBean(Pages.class).settings().relatedPosts()).isEqualTo(5));
		runner.withPropertyValues("beansprout.related-posts=0")
			.run(context -> assertThat(context.getBean(Pages.class).settings().relatedPosts()).isZero());
	}

	@Test
	@DisplayName("RELATED-9 a beansprout.related-posts below 0 stops the app at startup with a message that names the setting")
	void belowZeroStopsTheApp() {
		runner.withPropertyValues("beansprout.related-posts=-1")
			.run(context -> assertThat(context).getFailure().rootCause()
				.hasMessageContaining("beansprout.related-posts")
				.hasMessageContaining("-1"));
	}

}
