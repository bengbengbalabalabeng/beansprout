package dev.danvega.beansprout.code;

import java.io.Serial;

/**
 * The code, as the regex engine reads it, with a deadline. Some inputs make a regex retry from
 * every position, which takes time that grows with the square of the input. The engine reads
 * every character through {@link #charAt}, so checking the clock here bounds any input.
 */
final class BudgetedText implements CharSequence {

	private static final int CHECK_EVERY = 4096;

	private final String text;

	private final long deadline;

	private int reads;

	BudgetedText(String text, long deadline) {
		this.text = text;
		this.deadline = deadline;
	}

	@Override
	public char charAt(int index) {
		if (++reads % CHECK_EVERY == 0 && System.nanoTime() > deadline) {
			throw new OutOfTime();
		}
		return text.charAt(index);
	}

	@Override
	public int length() {
		return text.length();
	}

	@Override
	public CharSequence subSequence(int start, int end) {
		return text.subSequence(start, end);
	}

	@Override
	public String toString() {
		return text;
	}

	/** Highlighting a fence ran past its time budget. */
	static final class OutOfTime extends RuntimeException {

		@Serial
		private static final long serialVersionUID = 1L;

		OutOfTime() {
			super("highlighting ran out of time", null, false, false);
		}

	}

}
