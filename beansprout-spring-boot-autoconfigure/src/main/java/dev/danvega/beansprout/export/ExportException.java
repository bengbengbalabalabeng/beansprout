package dev.danvega.beansprout.export;

/** Why the static export stopped. The message says what to fix. */
public class ExportException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public ExportException(String message) {
		super(message);
	}

	public ExportException(String message, Throwable cause) {
		super(message, cause);
	}

}
