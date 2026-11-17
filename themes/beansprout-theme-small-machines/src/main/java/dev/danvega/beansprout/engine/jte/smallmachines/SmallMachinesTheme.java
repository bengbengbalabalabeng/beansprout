package dev.danvega.beansprout.engine.jte.smallmachines;

import dev.danvega.beansprout.theme.Theme;

/**
 * The Small Machines theme: the blog dressed as an old desktop, where every post is a window and
 * the archive and tags are folders. Light only. Listed in {@code META-INF/services}, so Beansprout
 * finds it and renders the blog with the templates compiled next to it.
 */
public final class SmallMachinesTheme implements Theme {

	/** The theme's name. */
	public static final String NAME = "small-machines";

	/** Where the theme's stylesheet is served, after the app's context path. */
	public static final String STYLESHEET = "/beansprout/small-machines/small-machines.css";

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
		return "dev.danvega.beansprout.engine.jte.smallmachines.templates";
	}

	@Override
	public String stylesheet() {
		return STYLESHEET;
	}

}
