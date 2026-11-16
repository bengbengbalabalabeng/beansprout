package dev.danvega.beansprout.content;

import java.io.Serial;

/** A Markdown file that cannot become a post. The message says why, in words a writer can act on. */
public class InvalidPostException extends RuntimeException {

	@Serial
	private static final long serialVersionUID = 1L;

	public InvalidPostException(String message) {
		super(message);
	}

}
