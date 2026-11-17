package dev.danvega.beansprout.theme;

/**
 * A theme jar's name tag. Each theme has one class that implements this, listed in
 * {@code META-INF/services/dev.danvega.beansprout.theme.Theme} so Beansprout finds every theme on
 * the classpath with {@link java.util.ServiceLoader}. The class needs a public constructor with no
 * arguments.
 */
public interface Theme {

	/** The name an app picks it by with {@code beansprout.theme}, like {@code longhand}. */
	String name();

	/** The template engine its pages are written for, like {@code jte}. */
	String engine();

	/** Where its compiled templates live, such as a Java package for precompiled JTE templates. */
	String templates();

	/** Its stylesheet's address after the app's context path, like {@code /beansprout/longhand/longhand.css}. */
	String stylesheet();

}
