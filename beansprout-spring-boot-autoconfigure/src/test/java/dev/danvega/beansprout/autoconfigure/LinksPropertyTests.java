package dev.danvega.beansprout.autoconfigure;

import dev.danvega.beansprout.view.NavLink;
import dev.danvega.beansprout.view.Pages;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class LinksPropertyTests {

	private final ApplicationContextRunner runner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(BeansproutAutoConfiguration.class))
		.withPropertyValues("beansprout.posts.location=classpath:no-such-folder/");

	@Test
	@DisplayName("THEME-24 beansprout.links reaches the menu, in the order given")
	void bound() {
		runner.withPropertyValues("beansprout.links[0].label=Shop", "beansprout.links[0].href=/shop",
				"beansprout.links[1].label=Docs", "beansprout.links[1].href=https://docs.example.com")
			.run(context -> assertThat(context.getBean(Pages.class).archive().site().links()).containsExactly(
					new NavLink("Shop", "/shop"), new NavLink("Docs", "https://docs.example.com")));
	}

	@Test
	@DisplayName("THEME-27 with no beansprout.links, the menu has no extra links")
	void noneByDefault() {
		runner.run(context -> assertThat(context.getBean(Pages.class).archive().site().links()).isEmpty());
	}

	@Test
	@DisplayName("THEME-26 a link with no label or a bad href stops the app at startup, naming the setting")
	void refusedAtStartup() {
		runner.withPropertyValues("beansprout.links[0].href=/shop")
			.run(context -> assertThat(context).getFailure().rootCause()
				.hasMessageContaining("beansprout.links needs a label"));
		runner.withPropertyValues("beansprout.links[0].label=Shop", "beansprout.links[0].href=shop")
			.run(context -> assertThat(context).getFailure().rootCause()
				.hasMessageContaining("beansprout.links 'Shop' needs an href"));
	}

}
