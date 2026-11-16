package dev.danvega.beansprout.sample;

import dev.danvega.beansprout.autoconfigure.BeansproutAutoConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class SampleApplicationTests {

	@Autowired
	ApplicationContext context;

	@Test
	@DisplayName("adding the starter is enough for Beansprout to switch on")
	void starterAppliesTheAutoConfiguration() {
		assertThat(context.getBeansOfType(BeansproutAutoConfiguration.class)).hasSize(1);
	}

}
