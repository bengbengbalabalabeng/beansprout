package dev.danvega.beansprout.engine.jte.callingcard;

import dev.danvega.beansprout.theme.Theme;

/**
 * The Calling Card theme: the author first, in poster-sized type. The index opens on one indigo
 * block with the blog's name, the description and the writer's photo, then every tag with its
 * count and the posts as title and date rows. Listed in {@code META-INF/services}, so Beansprout
 * finds it and renders the blog with the templates compiled next to it.
 */
public final class CallingCardTheme implements Theme {

	/** The theme's name. */
	public static final String NAME = "calling-card";

	/** Where the theme's stylesheet is served, after the app's context path. */
	public static final String STYLESHEET = "/beansprout/calling-card/calling-card.css";

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
		return "dev.danvega.beansprout.engine.jte.callingcard.templates";
	}

	@Override
	public String stylesheet() {
		return STYLESHEET;
	}

}
