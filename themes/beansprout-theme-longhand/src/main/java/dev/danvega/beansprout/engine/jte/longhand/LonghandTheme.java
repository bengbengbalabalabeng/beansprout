package dev.danvega.beansprout.engine.jte.longhand;

import dev.danvega.beansprout.theme.Theme;

/**
 * The Longhand theme: one writer, a long post with a picture, light and dark. Listed in
 * {@code META-INF/services}, so Beansprout finds it and renders the blog with the templates
 * compiled next to it.
 */
public final class LonghandTheme implements Theme {

	/** The theme's name. */
	public static final String NAME = "longhand";

	/** Where the theme's stylesheet is served, after the app's context path. */
	public static final String STYLESHEET = "/beansprout/longhand/longhand.css";

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
		return "dev.danvega.beansprout.engine.jte.longhand.templates";
	}

	@Override
	public String stylesheet() {
		return STYLESHEET;
	}

}
