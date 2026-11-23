package dev.danvega.beansprout.autoconfigure;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

import dev.danvega.beansprout.view.Slots;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

/** The app's slot files, read from its classpath once, when the app starts. */
final class SlotFiles {

	private SlotFiles() {
	}

	/** The three slots, each empty when its file is missing. */
	static Slots read(ResourceLoader resources) {
		return new Slots(read(resources, "beansprout/head.html"), read(resources, "beansprout/after-post.html"),
				read(resources, "beansprout/footer.html"));
	}

	/** One file's text as it is, or empty when the classpath has no such file. */
	private static String read(ResourceLoader resources, String name) {
		Resource resource = resources.getResource("classpath:" + name);
		if (!resource.isReadable()) {
			return "";
		}
		byte[] bytes;
		try (InputStream in = resource.getInputStream()) {
			bytes = in.readAllBytes();
		}
		catch (IOException ex) {
			throw new UncheckedIOException("Beansprout could not read " + name + " from the classpath.", ex);
		}
		try {
			return StandardCharsets.UTF_8.newDecoder()
				.onMalformedInput(CodingErrorAction.REPORT)
				.onUnmappableCharacter(CodingErrorAction.REPORT)
				.decode(ByteBuffer.wrap(bytes))
				.toString();
		}
		catch (CharacterCodingException ex) {
			// The decoder's own message names no file, so this one stands alone.
			throw new IllegalStateException(
					"Beansprout could not read " + name + ": it is not UTF-8. Save it as UTF-8 and start the app again.");
		}
	}

}
