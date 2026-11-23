package dev.danvega.beansprout.view;

/**
 * The app's own HTML for every page, from the files {@code beansprout/head.html},
 * {@code beansprout/after-post.html} and {@code beansprout/footer.html} on its classpath. A theme
 * writes each one as it is, without escaping it: the HTML is the app owner's, like a template.
 *
 * @param head HTML for the end of every page's {@code <head>}, or empty
 * @param afterPost HTML for after the body of every post, or empty
 * @param footer HTML for every page's footer, or empty
 */
public record Slots(String head, String afterPost, String footer) {

	/** No slots: what an app without slot files gets. */
	public static final Slots NONE = new Slots("", "", "");

}
