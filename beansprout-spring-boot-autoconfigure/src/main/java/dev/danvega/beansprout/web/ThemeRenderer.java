package dev.danvega.beansprout.web;

/**
 * Renders one page of a theme. Each template engine has its own. The controllers only know this.
 */
@FunctionalInterface
public interface ThemeRenderer {

	/**
	 * Renders a page.
	 *
	 * @param template the page's template name: {@code index}, {@code post}, {@code tag},
	 *     {@code archive} or {@code not-found}
	 * @param page the page's view record, such as {@code IndexPage}
	 * @return the page as HTML
	 */
	String render(String template, Object page);

}
