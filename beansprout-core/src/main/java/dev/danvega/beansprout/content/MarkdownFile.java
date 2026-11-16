package dev.danvega.beansprout.content;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

import org.jspecify.annotations.Nullable;

/**
 * One Markdown file as read from a {@link PostSource}.
 *
 * @param path where the file lives, relative to the posts folder, with {@code /} between folders.
 *     Used to sort files, to pick a slug, and to name the file in a warning.
 * @param text the whole file, frontmatter included
 * @param unreadable why the file could not be read, or null when it was. An unreadable file is
 *     skipped with this reason as its problem, and the other posts still load.
 */
public record MarkdownFile(String path, String text, @Nullable String unreadable) {

	/** A file that was read. */
	public MarkdownFile(String path, String text) {
		this(path, text, null);
	}

	/** A file that could not be read, and why, in words a writer can act on. */
	public static MarkdownFile unreadable(String path, String reason) {
		return new MarkdownFile(path, "", reason);
	}

	/** Decodes a file's bytes as UTF-8. Bytes that are not UTF-8 give an unreadable file. */
	public static MarkdownFile decode(String path, byte[] bytes) {
		try {
			String text = StandardCharsets.UTF_8.newDecoder()
				.onMalformedInput(CodingErrorAction.REPORT)
				.onUnmappableCharacter(CodingErrorAction.REPORT)
				.decode(ByteBuffer.wrap(bytes))
				.toString();
			return new MarkdownFile(path, text);
		}
		catch (CharacterCodingException ex) {
			return unreadable(path, "it is not UTF-8 text. Save it as UTF-8.");
		}
	}

	/** True for files a folder holds but nobody wrote as a post, such as macOS {@code ._name.md}. */
	public static boolean hidden(String fileName) {
		return fileName.startsWith(".");
	}

}
