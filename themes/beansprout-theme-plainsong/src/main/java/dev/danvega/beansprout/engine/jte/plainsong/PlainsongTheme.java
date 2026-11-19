package dev.danvega.beansprout.engine.jte.plainsong;

import dev.danvega.beansprout.theme.Theme;

/**
 * The Plainsong theme: one column in black and white, with no accent color. The index is a plain
 * list of titles, dates and one-line summaries, with no pictures. Listed in
 * {@code META-INF/services}, so Beansprout finds it and renders the blog with the templates
 * compiled next to it.
 */
public final class PlainsongTheme implements Theme {

	/** The theme's name. */
	public static final String NAME = "plainsong";

	/** Where the theme's stylesheet is served, after the app's context path. */
	public static final String STYLESHEET = "/beansprout/plainsong/plainsong.css";

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
		return "dev.danvega.beansprout.engine.jte.plainsong.templates";
	}

	@Override
	public String stylesheet() {
		return STYLESHEET;
	}

}
