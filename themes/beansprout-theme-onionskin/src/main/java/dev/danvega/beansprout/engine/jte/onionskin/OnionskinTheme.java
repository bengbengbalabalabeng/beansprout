package dev.danvega.beansprout.engine.jte.onionskin;

import dev.danvega.beansprout.theme.Theme;

/**
 * The Onionskin theme: a blog typed like a memo, set in monospace on plain paper. Each post on a
 * list is a memo header (Date, Subject, Filed, Length) over its summary, and a yellow highlighter
 * marks the current page and the link under the pointer. Listed in {@code META-INF/services}, so
 * Beansprout finds it and renders the blog with the templates compiled next to it.
 */
public final class OnionskinTheme implements Theme {

	/** The theme's name. */
	public static final String NAME = "onionskin";

	/** Where the theme's stylesheet is served, after the app's context path. */
	public static final String STYLESHEET = "/beansprout/onionskin/onionskin.css";

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
		return "dev.danvega.beansprout.engine.jte.onionskin.templates";
	}

	@Override
	public String stylesheet() {
		return STYLESHEET;
	}

}
