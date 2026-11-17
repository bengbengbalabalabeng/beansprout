package dev.danvega.beansprout.engine.jte.marginalia;

import dev.danvega.beansprout.theme.Theme;

/**
 * The Marginalia theme: a link blog kept like a notebook, with short entries grouped by day and
 * the small facts in a margin beside the words. Listed in {@code META-INF/services}, so
 * Beansprout finds it and renders the blog with the templates compiled next to it.
 */
public final class MarginaliaTheme implements Theme {

	/** The theme's name. */
	public static final String NAME = "marginalia";

	/** Where the theme's stylesheet is served, after the app's context path. */
	public static final String STYLESHEET = "/beansprout/marginalia/marginalia.css";

	@Override
	public String name() {
		return NAME;
	}

	@Override
	public String engine() {
		return "jte";
	}

	@Override
	public String templates() {
		return "dev.danvega.beansprout.engine.jte.marginalia.templates";
	}

	@Override
	public String stylesheet() {
		return STYLESHEET;
	}

}
