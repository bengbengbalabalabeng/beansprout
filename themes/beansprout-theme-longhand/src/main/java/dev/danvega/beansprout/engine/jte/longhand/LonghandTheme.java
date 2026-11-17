package dev.danvega.beansprout.engine.jte.longhand;

/**
 * Marks the Longhand theme as present. Beansprout's JTE auto-configuration looks for this class
 * by name and renders the blog with the templates compiled next to it.
 */
public final class LonghandTheme {

	/** The theme's name. */
	public static final String NAME = "longhand";

	/** Where the theme's stylesheet is served, after the app's context path. */
	public static final String STYLESHEET = "/beansprout/longhand/longhand.css";

	private LonghandTheme() {
	}

}
