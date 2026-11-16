package dev.danvega.beansprout.verification;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The gate. Runs last in the build, against the whole checkout. Agents may not edit this module.
 */
class DefinitionOfDoneTests {

	private final Repo repo = Repo.current();

	@Test
	@DisplayName("every spec requirement has a test that names its ID")
	void everyRequirementHasATest() {
		assertThat(Judges.everyRequirementHasATest(repo)).isEmpty();
	}

	@Test
	@DisplayName("every ID a test names is defined in a spec")
	void everyTestedIdExists() {
		assertThat(Judges.everyTestedIdExists(repo)).isEmpty();
	}

	@Test
	@DisplayName("shipped code has no TODO, FIXME or XXX")
	void noMarkersInShippedCode() {
		assertThat(Judges.noMarkersInShippedCode(repo)).isEmpty();
	}

}
