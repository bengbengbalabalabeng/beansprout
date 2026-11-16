package dev.danvega.beansprout.engine.jte;

import dev.danvega.beansprout.web.ThemeRenderer;
import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.output.StringOutput;

/**
 * Renders a JTE theme whose templates were compiled ahead of time into a package. The theme has
 * its own engine, so it works whatever JTE settings the app has, and with none.
 */
public final class JteThemeRenderer implements ThemeRenderer {

	private final TemplateEngine engine;

	private JteThemeRenderer(TemplateEngine engine) {
		this.engine = engine;
	}

	/**
	 * A renderer for templates compiled into a package, such as the Longhand theme's.
	 *
	 * @param packageName the package the theme's compiled templates live in
	 * @param classLoader the class loader that can see them
	 */
	public static JteThemeRenderer precompiled(String packageName, ClassLoader classLoader) {
		return new JteThemeRenderer(TemplateEngine.createPrecompiled(null, ContentType.Html, classLoader, packageName));
	}

	@Override
	public String render(String template, Object page) {
		StringOutput output = new StringOutput();
		engine.render(template + ".jte", page, output);
		return output.toString();
	}

}
