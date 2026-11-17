package dev.danvega.beansprout.engine.jte.linocut;

import dev.danvega.beansprout.theme.Theme;

/**
 * The Linocut theme: bold and playful, black ink on warm paper with one red. Listed in
 * {@code META-INF/services}, so Beansprout finds it and renders the blog with the templates
 * compiled next to it.
 */
public final class LinocutTheme implements Theme {

	/** The theme's name. */
	public static final String NAME = "linocut";

	/** Where the theme's stylesheet is served, after the app's context path. */
	public static final String STYLESHEET = "/beansprout/linocut/linocut.css";

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
		return "dev.danvega.beansprout.engine.jte.linocut.templates";
	}

	@Override
	public String stylesheet() {
		return STYLESHEET;
	}

}
