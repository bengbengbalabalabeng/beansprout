package dev.danvega.beansprout.autoconfigure;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

import dev.danvega.beansprout.theme.Theme;
import org.jspecify.annotations.Nullable;

/** Finds the theme jars on the classpath and picks the one that renders the blog. */
final class Themes {

	private Themes() {
	}

	/** Every theme on the classpath written for this engine, in the order the classpath lists them. */
	static List<Theme> found(@Nullable ClassLoader classLoader, String engine) {
		List<Theme> themes = new ArrayList<>();
		for (Theme theme : ServiceLoader.load(Theme.class, classLoader)) {
			if (theme.engine().equals(engine)) {
				themes.add(theme);
			}
		}
		return themes;
	}

	/**
	 * The theme to use: the one named by {@code beansprout.theme}, or the only one there is. Two
	 * or more with no choice, or a choice that is not there, stop the app with a message that
	 * says what was found, rather than quietly picking one.
	 */
	static Theme choose(List<Theme> found, @Nullable String chosen) {
		String names = found.stream().map(Theme::name).collect(Collectors.joining(", "));
		if (chosen != null && !chosen.isBlank()) {
			List<Theme> named = found.stream().filter(theme -> theme.name().equals(chosen.strip())).toList();
			if (named.isEmpty()) {
				throw new IllegalStateException("beansprout.theme is '" + chosen
						+ "', but no theme by that name is on the classpath. Themes found: " + names + ".");
			}
			if (named.size() > 1) {
				throw new IllegalStateException("There is more than one theme jar on the classpath named '" + chosen.strip()
						+ "'. Keep one of them.");
			}
			return named.getFirst();
		}
		if (found.size() != 1) {
			throw new IllegalStateException("Beansprout found " + found.size() + " themes: " + names
					+ ". Choose one with beansprout.theme, like beansprout.theme=" + found.getFirst().name() + ".");
		}
		return found.getFirst();
	}

}
