package dev.danvega.beansprout.code;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;

/**
 * An ordered list of token rules, and the tokenizer that applies them.
 *
 * <p>The tokenizer scans left to right. At each step it takes the rule whose next match starts
 * first, and the earlier rule when two start at the same place. Text no rule matches is plain.
 * So a comment that starts before a string wins, and a string that starts before a comment wins,
 * the same result Prism's greedy rules give.
 */
final class Grammar {

	private final List<Rule> rules;

	private Grammar(List<Rule> rules) {
		this.rules = List.copyOf(rules);
	}

	static Builder builder() {
		return new Builder();
	}

	/**
	 * Appends the highlighted HTML for {@code text} to {@code out}.
	 *
	 * @param deadline a {@link System#nanoTime()} after which the regex engine stops with
	 *     {@link BudgetedText.OutOfTime}
	 */
	void render(String text, StringBuilder out, long deadline) {
		BudgetedText input = new BudgetedText(text, deadline);
		int count = rules.size();
		Matcher[] matchers = new Matcher[count];
		int[] starts = new int[count];
		for (int i = 0; i < count; i++) {
			matchers[i] = rules.get(i).pattern().matcher(input);
			starts[i] = -2;
		}
		int pos = 0;
		while (pos < text.length()) {
			int best = -1;
			for (int i = 0; i < count; i++) {
				if (starts[i] == -2 || (starts[i] >= 0 && starts[i] < pos)) {
					starts[i] = next(matchers[i], rules.get(i), pos, text.length());
				}
				if (starts[i] >= 0 && (best < 0 || starts[i] < starts[best])) {
					best = i;
				}
			}
			if (best < 0) {
				break;
			}
			Rule rule = rules.get(best);
			Matcher match = matchers[best];
			int tokenStart = rule.lookbehind() ? match.end(1) : match.start();
			Html.escape(text, pos, tokenStart, out);
			out.append("<span class=\"token ").append(rule.type()).append("\">");
			Grammar inside = rule.inside();
			if (inside != null) {
				inside.render(text.substring(tokenStart, match.end()), out, deadline);
			}
			else {
				Html.escape(text, tokenStart, match.end(), out);
			}
			out.append("</span>");
			pos = match.end();
		}
		Html.escape(text, pos, text.length(), out);
	}

	/** Where the rule's next non-empty token starts, at or after {@code from}, or -1. */
	private static int next(Matcher matcher, Rule rule, int from, int length) {
		int at = from;
		while (at <= length && matcher.find(at)) {
			int tokenStart = rule.lookbehind() ? matcher.end(1) : matcher.start();
			if (matcher.end() > tokenStart) {
				return matcher.start();
			}
			at = matcher.start() + 1;
		}
		return -1;
	}

	static final class Builder {

		private final List<Rule> rules = new ArrayList<>();

		/** A token of this type wherever the pattern matches. */
		Builder token(String type, String regex) {
			return add(type, Pattern.compile(regex), false, null);
		}

		/** A token of this type, compiled with these {@link Pattern} flags. */
		Builder token(String type, String regex, int flags) {
			return add(type, Pattern.compile(regex, flags), false, null);
		}

		/** A token whose first group is only context: the token is the rest of the match. */
		Builder after(String type, String regex, int flags) {
			return add(type, Pattern.compile(regex, flags), true, null);
		}

		/** A token whose text is highlighted again with another grammar. */
		Builder nested(String type, String regex, Grammar inside) {
			return add(type, Pattern.compile(regex), false, inside);
		}

		private Builder add(String type, Pattern pattern, boolean lookbehind, @Nullable Grammar inside) {
			rules.add(new Rule(type, pattern, lookbehind, inside));
			return this;
		}

		Grammar build() {
			return new Grammar(rules);
		}

	}

}
