package dev.danvega.beansprout.verification;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The definition of done, as checks. Each returns the problems it found. An empty list is a pass.
 */
final class Judges {

	private Judges() {
	}

	/** Every requirement in {@code specs/} has a test whose {@code @DisplayName} names its ID. */
	static List<String> everyRequirementHasATest(Repo repo) {
		List<String> problems = new ArrayList<>();
		Map<String, String> requirements = repo.requirements(problems);
		Map<String, String> tested = repo.testedIds();
		requirements.forEach((id, spec) -> {
			if (!tested.containsKey(id)) {
				problems.add(id + " (" + spec + ") has no test. Put the ID in a @DisplayName.");
			}
		});
		return problems;
	}

	/** Every ID a test claims to cover exists in a spec, so a typo cannot fake coverage. */
	static List<String> everyTestedIdExists(Repo repo) {
		List<String> problems = new ArrayList<>();
		Set<String> defined = repo.requirements(new ArrayList<>()).keySet();
		Set<String> prefixes = Repo.prefixes(defined);
		repo.testedIds().forEach((id, where) -> {
			boolean looksLikeOurs = prefixes.contains(id.substring(0, id.lastIndexOf('-')));
			if (looksLikeOurs && !defined.contains(id)) {
				problems.add(where + " names " + id + ", which no spec defines.");
			}
		});
		return problems;
	}

	/** No TODO, FIXME or XXX in shipped code. */
	static List<String> noMarkersInShippedCode(Repo repo) {
		return repo.markers().stream().map(hit -> "Unfinished work in shipped code: " + hit).toList();
	}

}
