package dev.danvega.beansprout.verification;

import java.net.URISyntaxException;
import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A gate that cannot fail proves nothing. Each judge runs against a small fixture repo in
 * {@code src/test/resources/fixtures} that breaks its rule, and must report it.
 */
class JudgesCanFailTests {

	@Test
	@DisplayName("a requirement with no test is reported")
	void untestedRequirementIsReported() {
		assertThat(Judges.everyRequirementHasATest(fixture("untested")))
			.singleElement().asString().startsWith("DEMO-2 ");
	}

	@Test
	@DisplayName("a requirement defined twice is reported")
	void duplicateRequirementIsReported() {
		assertThat(Judges.everyRequirementHasATest(fixture("duplicate")))
			.anySatisfy(problem -> assertThat(problem).contains("defined in both"));
	}

	@Test
	@DisplayName("a test naming an ID no spec defines is reported, and unrelated codes are not")
	void typoInTestedIdIsReported() {
		assertThat(Judges.everyTestedIdExists(fixture("typo")))
			.singleElement().asString().contains("DEMO-9");
	}

	@Test
	@DisplayName("a TODO in shipped code is reported, and one in a test is not")
	void markerInShippedCodeIsReported() {
		assertThat(Judges.noMarkersInShippedCode(fixture("todo")))
			.singleElement().asString().contains("demo/src/main/java/Demo.java:3");
	}

	@Test
	@DisplayName("a clean repo passes every judge")
	void cleanRepoPasses() {
		Repo clean = fixture("clean");
		assertThat(Judges.everyRequirementHasATest(clean)).isEmpty();
		assertThat(Judges.everyTestedIdExists(clean)).isEmpty();
		assertThat(Judges.noMarkersInShippedCode(clean)).isEmpty();
	}

	private static Repo fixture(String name) {
		try {
			return new Repo(Path.of(JudgesCanFailTests.class.getResource("/fixtures/" + name).toURI()));
		}
		catch (URISyntaxException ex) {
			throw new IllegalStateException(ex);
		}
	}

}
