package dev.danvega.beansprout.view;

/** A link to another post: the newer or the older one at the foot of a post, or a related one. */
public record PostLink(String title, String url) {

	/** No post that way: this one is the newest, or the oldest. */
	public static final PostLink NONE = new PostLink("", "");

}
