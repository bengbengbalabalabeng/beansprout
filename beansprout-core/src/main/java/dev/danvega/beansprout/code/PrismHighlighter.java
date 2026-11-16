/*
 * The grammars in this file are adapted from Prism (https://prismjs.com), which is under this
 * license:
 *
 * MIT LICENSE
 *
 * Copyright (c) 2012 Lea Verou
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software
 * and associated documentation files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package dev.danvega.beansprout.code;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * The built-in highlighter: Prism's grammars for the languages a Spring blog shows, ported to
 * Java. Token types and class names are Prism's, so any Prism theme colors the output.
 */
public final class PrismHighlighter implements CodeHighlighter {

	/** How long one fence may take. A normal fence takes a few milliseconds. */
	static final java.time.Duration TIME_BUDGET = java.time.Duration.ofMillis(250);

	private static final int I = Pattern.CASE_INSENSITIVE;

	private static final int M = Pattern.MULTILINE;

	private static final String C_COMMENT = "//.*|/\\*[\\s\\S]*?(?:\\*/|$)";

	/*
	 * Strings are written as "plain run, then (escape, plain run) repeated", with possessive
	 * quantifiers. Prism's shorter (?:\\.|[^"])* form makes Java's regex engine recurse once per
	 * character, which overflows the stack on a long literal.
	 */
	private static final String DOUBLE_STRING = "\"[^\"\\\\\\n]*+(?:\\\\.[^\"\\\\\\n]*+)*+\"";

	private static final String SINGLE_STRING = "'[^'\\\\\\n]*+(?:\\\\.[^'\\\\\\n]*+)*+'";

	private static final String NUMBER = "\\b0[xXbB][\\da-fA-F_]+[lLnN]?\\b|(?:\\b\\d[\\d_]*\\.?[\\d_]*|\\B\\.\\d[\\d_]*)(?:[eE][+-]?\\d[\\d_]*)?[dDfFlLnN]?";

	private static final String FUNCTION = "\\b[a-zA-Z_$][\\w$]*(?=\\s*\\()";

	private static final String CONSTANT = "\\b[A-Z][A-Z0-9_]{2,}\\b";

	private static final String CLASS_NAME = "\\b[A-Z][\\w$]*\\b";

	private static final String OPERATOR = "->|::|\\+\\+|--|&&|\\|\\||[-+*/%=!<>&|^~?:]=?";

	private static final String PUNCTUATION = "[{}\\[\\]();,.]";

	private static final Grammar JAVA = Grammar.builder()
		.token("comment", C_COMMENT)
		.token("string", "\"\"\"[\\s\\S]*?\"\"\"")
		.token("string", DOUBLE_STRING)
		.token("char", "'(?:\\\\.|[^'\\\\\\n])'")
		.token("annotation", "(?<![.\\w])@\\w+(?:\\.\\w+)*")
		.token("keyword", "\\bnon-sealed\\b|\\b(?:abstract|assert|boolean|break|byte|case|catch|char|class|const|continue"
				+ "|default|do|double|else|enum|exports|extends|final|finally|float|for|goto|if|implements|import"
				+ "|instanceof|int|interface|long|module|native|new|null|open|opens|package|permits|private|protected"
				+ "|provides|public|record|requires|return|sealed|short|static|strictfp|super|switch|synchronized|this"
				+ "|throw|throws|to|transient|transitive|try|uses|var|void|volatile|when|while|with|yield)\\b")
		.token("boolean", "\\b(?:true|false)\\b")
		.token("number", NUMBER)
		.token("function", FUNCTION)
		.token("constant", CONSTANT)
		.token("class-name", CLASS_NAME)
		.token("operator", OPERATOR)
		.token("punctuation", PUNCTUATION)
		.build();

	private static final Grammar KOTLIN = Grammar.builder()
		.token("comment", C_COMMENT)
		.token("string", "\"\"\"[\\s\\S]*?\"\"\"")
		.token("string", DOUBLE_STRING)
		.token("char", "'(?:\\\\.|[^'\\\\\\n])'")
		.token("annotation", "(?<![.\\w])@\\w+(?::\\w+)?")
		.token("keyword", "\\b(?:abstract|actual|annotation|as|break|by|catch|class|companion|const|constructor"
				+ "|continue|crossinline|data|do|dynamic|else|enum|expect|external|final|finally|for|fun|get|if"
				+ "|import|in|infix|init|inline|inner|interface|internal|is|lateinit|noinline|null|object|open"
				+ "|operator|out|override|package|private|protected|public|reified|return|sealed|set|super|suspend"
				+ "|tailrec|this|throw|try|typealias|val|value|var|vararg|when|where|while)\\b")
		.token("boolean", "\\b(?:true|false)\\b")
		.token("number", NUMBER)
		.token("function", FUNCTION)
		.token("constant", CONSTANT)
		.token("class-name", CLASS_NAME)
		.token("operator", "\\.\\.|\\?:|!!|" + OPERATOR)
		.token("punctuation", PUNCTUATION)
		.build();

	private static final Grammar TAG_INSIDE = Grammar.builder()
		.token("attr-value", "(?<==\\s{0,10})(?:\"[^\"]*\"|'[^']*'|[^\\s'\">=]+)")
		.token("tag", "(?<=</?)[^\\s>/]+")
		.token("punctuation", "</?|/?>|=")
		.token("attr-name", "[^\\s>/=\"']+")
		.build();

	private static final Grammar MARKUP = Grammar.builder()
		.token("comment", "<!--[\\s\\S]*?(?:-->|$)")
		.token("prolog", "<\\?[\\s\\S]+?\\?>")
		.token("doctype", "<!DOCTYPE[\\s\\S]+?>", I)
		.token("cdata", "<!\\[CDATA\\[[\\s\\S]*?\\]\\]>", I)
		.nested("tag", "</?[A-Za-z][\\w:.-]*(?:\\s++[^\\s>/=]++(?:\\s*=\\s*(?:\"[^\"]*+\"|'[^']*+'|[^\\s'\">=]++))?)*+\\s*/?>",
				TAG_INSIDE)
		.token("entity", "&#?[\\da-zA-Z]+;")
		.build();

	private static final Grammar YAML = Grammar.builder()
		.token("comment", "(?<![^\\s])#.*")
		.token("directive important", "^%.+", M)
		.token("string", DOUBLE_STRING + "|'[^'\\n]*+(?:''[^'\\n]*+)*+'")
		.after("key atrule", "(^[ \\t]*(?:-[ \\t]+)?|[{,][ \\t]*)([^\\s:#\\[\\]{},'\"](?:[^:#\\[\\]{},\\n]*[^\\s:#\\[\\]{},\\n])?)(?=[ \\t]*:(?:[ \\t]|$))", M)
		.token("important", "(?<![\\w])[&*][\\w-]+")
		.token("tag", "(?<![\\w])!\\S*")
		.token("boolean", "(?<![\\w-])(?:true|false)(?![\\w-])", I)
		.token("null keyword", "(?<![\\w-])(?:null|~)(?![\\w-])", I)
		.token("number", "(?<![\\w.-])[-+]?(?:\\d[\\d_]*(?:\\.\\d*)?|\\.\\d+)(?:[eE][-+]?\\d+)?(?![\\w.])")
		.token("punctuation", "^(?:---|\\.\\.\\.)$|[:\\[\\]{}|>,]|(?<=^[ \\t]{0,40})-(?=[ \\t])", M)
		.build();

	private static final Grammar PROPERTIES = Grammar.builder()
		.token("comment", "^[ \\t]*[#!].*", M)
		.after("attr-name", "(^[ \\t]*)([^\\\\\\s:=]*+(?:\\\\.[^\\\\\\s:=]*+)*+)", M)
		.token("attr-value", "(?<=[=:][ \\t]{0,20})\\S.*")
		.token("punctuation", "[=:]")
		.build();

	private static final Grammar JSON = Grammar.builder()
		.token("comment", "//.*|/\\*[\\s\\S]*?(?:\\*/|$)")
		.token("property", DOUBLE_STRING + "(?=\\s*:)")
		.token("string", DOUBLE_STRING)
		.token("number", "-?\\b\\d+(?:\\.\\d+)?(?:[eE][+-]?\\d+)?\\b")
		.token("boolean", "\\b(?:true|false)\\b")
		.token("null keyword", "\\bnull\\b")
		.token("punctuation", "[{}\\[\\],]")
		.token("operator", ":")
		.build();

	private static final Grammar SQL = Grammar.builder()
		.token("comment", "--.*|#.*|/\\*[\\s\\S]*?(?:\\*/|$)")
		.token("string", "'[^']*+(?:''[^']*+)*+'|\"[^\"\\\\]*+(?:\\\\.[^\"\\\\]*+)*+\"")
		.token("identifier", "`[^`]*`")
		.token("function", "\\b(?:avg|coalesce|count|lower|max|min|now|round|substring|sum|trim|upper)(?=\\s*\\()", I)
		.token("boolean", "\\b(?:true|false)\\b", I)
		.token("keyword", "\\b(?:add|all|alter|and|as|asc|begin|between|bigint|boolean|by|cascade|case|check|column"
				+ "|commit|constraint|create|cross|date|decimal|default|delete|desc|distinct|drop|else|end|exists"
				+ "|foreign|from|full|grant|group|having|if|in|index|inner|insert|int|integer|into|is|join|key|left"
				+ "|like|limit|not|null|numeric|offset|on|or|order|outer|primary|references|returning|revoke|right"
				+ "|rollback|select|serial|set|table|text|then|timestamp|transaction|union|unique|update|using"
				+ "|values|varchar|view|when|where|with|recursive)\\b", I)
		.token("number", "\\b0x[\\da-f]+\\b|\\b\\d+(?:\\.\\d*)?|\\B\\.\\d+", I)
		.token("operator", "<=|>=|<>|!=|\\|\\||[-+*/%=<>^~]")
		.token("punctuation", "[;\\[\\]().,]")
		.build();

	private static final Grammar BASH_STRING_INSIDE = Grammar.builder()
		.token("variable", "\\$\\{[^}]+\\}|\\$\\w+|\\$[#?*@$!0-9-]")
		.build();

	private static final Grammar BASH = Grammar.builder()
		.token("shebang important", "^#!.*", M)
		.token("comment", "(?<![^\\s;])#.*")
		.nested("string", "\"[^\"\\\\]*+(?:\\\\.[^\"\\\\]*+)*+\"", BASH_STRING_INSIDE)
		.token("string", "'[^']*'")
		.token("variable", "\\$\\{[^}]+\\}|\\$\\w+|\\$[#?*@$!0-9-]")
		.token("keyword", "\\b(?:if|then|else|elif|fi|for|while|until|do|done|case|esac|in|function|select|return"
				+ "|break|continue|local|declare|readonly|time)\\b")
		.token("function", "(?<![\\w./-])(?:alias|awk|cat|cd|chmod|chown|cp|curl|docker|echo|eval|exec|exit|export"
				+ "|find|git|gradle|grep|java|kill|ls|mkdir|mv|npm|printf|ps|read|rm|sed|set|source|ssh|sudo|tar"
				+ "|test|touch|unset|which)(?![\\w./-])")
		.token("operator", "&&|\\|\\||>>|[|&;<>]")
		.token("number", "(?<![\\w-])\\d+(?![\\w.])")
		.token("punctuation", "[\\[\\]{}()]")
		.build();

	private static final String JS_KEYWORDS = "as|async|await|break|case|catch|class|const|continue|debugger|default"
			+ "|delete|do|else|enum|export|extends|finally|for|from|function|get|if|implements|import|in"
			+ "|instanceof|interface|let|new|null|of|package|private|protected|public|return|set|static|super"
			+ "|switch|this|throw|try|typeof|undefined|var|void|while|with|yield";

	private static final Grammar JAVASCRIPT = javascript(Grammar.builder(), JS_KEYWORDS).build();

	private static final Grammar TYPESCRIPT = javascript(Grammar.builder()
		.token("builtin", "\\b(?:any|bigint|boolean|never|number|object|string|symbol|unknown|void)\\b"),
			JS_KEYWORDS + "|abstract|asserts|declare|infer|is|keyof|module|namespace|override|readonly|satisfies|type")
		.build();

	private static final Map<String, Grammar> LANGUAGES = Map.ofEntries(
			Map.entry("java", JAVA),
			Map.entry("kotlin", KOTLIN), Map.entry("kt", KOTLIN),
			Map.entry("xml", MARKUP), Map.entry("html", MARKUP), Map.entry("markup", MARKUP), Map.entry("svg", MARKUP),
			Map.entry("yaml", YAML), Map.entry("yml", YAML),
			Map.entry("properties", PROPERTIES),
			Map.entry("json", JSON),
			Map.entry("sql", SQL),
			Map.entry("bash", BASH), Map.entry("sh", BASH), Map.entry("shell", BASH), Map.entry("zsh", BASH),
			Map.entry("javascript", JAVASCRIPT), Map.entry("js", JAVASCRIPT),
			Map.entry("typescript", TYPESCRIPT), Map.entry("ts", TYPESCRIPT));

	private static Grammar.Builder javascript(Grammar.Builder builder, String keywords) {
		return builder.token("comment", C_COMMENT)
			.token("template-string string", "`[^`\\\\]*+(?:\\\\.[^`\\\\]*+)*+`")
			.token("string", DOUBLE_STRING + "|" + SINGLE_STRING)
			.token("keyword", "\\b(?:" + keywords + ")\\b")
			.token("boolean", "\\b(?:true|false)\\b")
			.token("number", NUMBER)
			.token("function", FUNCTION)
			.token("class-name", CLASS_NAME)
			.token("operator", "=>|\\.{3}|\\?\\?=?|" + OPERATOR)
			.token("punctuation", PUNCTUATION);
	}

	@Override
	public Optional<String> highlight(String language, String code) {
		Grammar grammar = LANGUAGES.get(language.toLowerCase(Locale.ROOT));
		if (grammar == null) {
			return Optional.empty();
		}
		StringBuilder out = new StringBuilder(code.length() * 2);
		try {
			grammar.render(code, out, System.nanoTime() + TIME_BUDGET.toNanos());
		}
		catch (BudgetedText.OutOfTime | StackOverflowError ex) {
			// A pathological input ran out of time or of stack. Show the code plain.
			out.setLength(0);
			Html.escape(code, 0, code.length(), out);
		}
		return Optional.of(out.toString());
	}

}
