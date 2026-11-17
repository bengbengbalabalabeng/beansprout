package dev.danvega.beansprout.autoconfigure;

import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/** Matches when at least one JTE theme jar is on the classpath. */
class OnThemeCondition extends SpringBootCondition {

	@Override
	public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
		int count = Themes.found(context.getClassLoader(), BeansproutJteAutoConfiguration.ENGINE).size();
		return count > 0 ? ConditionOutcome.match("found " + count + " JTE theme(s)")
				: ConditionOutcome.noMatch("found no JTE theme on the classpath");
	}

}
