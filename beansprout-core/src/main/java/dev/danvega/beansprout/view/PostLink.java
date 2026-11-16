package dev.danvega.beansprout.view;

/** The newer or the older post, at the foot of a post. */
public record PostLink(String title, String url) {

	/** No post that way: this one is the newest, or the oldest. */
	public static final PostLink NONE = new PostLink("", "");

}
