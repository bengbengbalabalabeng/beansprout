package dev.danvega.beansprout.engine.jte.kilntui;

import dev.danvega.beansprout.theme.Theme;

/**
 * The Kiln TUI theme: a terminal interface aesthetic, carbon black with amber and bracket-prefixed
 * panels. Listed in {@code META-INF/services}, so Beansprout finds it and renders the blog with
 * the templates compiled next to it.
 */
public final class KilntuiTheme implements Theme {

	/** The theme's name. */
	public static final String NAME = "kilntui";

	/** Where the theme's stylesheet is served, after the app's context path. */
	public static final String STYLESHEET = "/beansprout/kilntui/kilntui.css";

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
		return "dev.danvega.beansprout.engine.jte.kilntui.templates";
	}

	@Override
	public String stylesheet() {
		return STYLESHEET;
	}

}
