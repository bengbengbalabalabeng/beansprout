package dev.danvega.beansprout.engine.jte.dateline;

import dev.danvega.beansprout.theme.Theme;

/**
 * The Dateline theme: an engineering blog dressed as a status board, dark. Listed in
 * {@code META-INF/services}, so Beansprout finds it and renders the blog with the templates
 * compiled next to it.
 */
public final class DatelineTheme implements Theme {

	/** The theme's name. */
	public static final String NAME = "dateline";

	/** Where the theme's stylesheet is served, after the app's context path. */
	public static final String STYLESHEET = "/beansprout/dateline/dateline.css";

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
		return "dev.danvega.beansprout.engine.jte.dateline.templates";
	}

	@Override
	public String stylesheet() {
		return STYLESHEET;
	}

}
