package dev.danvega.beansprout.content;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;

import org.jspecify.annotations.Nullable;

/**
 * A picture's width and height in pixels, read from the start of its file, never the whole picture.
 * WebP and GIF are read from their first bytes. PNG and JPEG go to the JDK's own image readers,
 * which stop at the header.
 */
record ImageSize(int width, int height) {

	/** The longest header read by hand: a lossy WebP's size ends at byte 30. */
	private static final int HEADER = 30;

	/** The JDK readers asked for a size. Other formats the JDK knows, like BMP, are not served. */
	private static final Set<String> READERS = Set.of("png", "jpeg");

	/**
	 * The size of the picture in this stream, or null when it is not a PNG, JPEG, GIF or WebP, or
	 * its header cannot be read. Does not close the stream.
	 */
	static @Nullable ImageSize read(InputStream in) {
		try {
			BufferedInputStream buffered = new BufferedInputStream(in);
			buffered.mark(HEADER);
			byte[] head = buffered.readNBytes(HEADER);
			buffered.reset();
			ImageSize size = startsWith(head, 0, "RIFF") && startsWith(head, 8, "WEBP") ? webp(head)
					: startsWith(head, 0, "GIF87a") || startsWith(head, 0, "GIF89a") ? gif(head) : jdk(buffered);
			return size != null && size.width > 0 && size.height > 0 ? size : null;
		}
		catch (IOException | RuntimeException ex) {
			// A broken file is only a picture with no size. The JDK readers throw unchecked
			// exceptions on some damaged files, so those count too.
			return null;
		}
	}

	/**
	 * A GIF's logical screen, which browsers show, and which can be bigger than its first frame.
	 */
	private static ImageSize gif(byte[] head) {
		return head.length < 10 ? new ImageSize(0, 0) : new ImageSize(le16(head, 6), le16(head, 8));
	}

	/** The three WebP kinds: lossy {@code VP8 }, lossless {@code VP8L} and extended {@code VP8X}. */
	private static @Nullable ImageSize webp(byte[] head) {
		if (startsWith(head, 12, "VP8 ") && head.length >= 30 && (head[23] & 0xff) == 0x9d && head[24] == 0x01
				&& head[25] == 0x2a) {
			return new ImageSize(le16(head, 26) & 0x3fff, le16(head, 28) & 0x3fff);
		}
		if (startsWith(head, 12, "VP8L") && head.length >= 25 && head[20] == 0x2f) {
			int bits = le16(head, 21) | (le16(head, 23) << 16);
			return new ImageSize((bits & 0x3fff) + 1, ((bits >>> 14) & 0x3fff) + 1);
		}
		if (startsWith(head, 12, "VP8X") && head.length >= 30) {
			return new ImageSize(le24(head, 24) + 1, le24(head, 27) + 1);
		}
		return null;
	}

	private static @Nullable ImageSize jdk(InputStream in) throws IOException {
		try (ImageInputStream image = new MemoryCacheImageInputStream(in)) {
			Iterator<ImageReader> readers = ImageIO.getImageReaders(image);
			while (readers.hasNext()) {
				ImageReader reader = readers.next();
				try {
					if (READERS.contains(reader.getFormatName().toLowerCase(Locale.ROOT))) {
						reader.setInput(image, true, true);
						return new ImageSize(reader.getWidth(0), reader.getHeight(0));
					}
				}
				finally {
					reader.dispose();
				}
			}
			return null;
		}
	}

	private static boolean startsWith(byte[] bytes, int at, String ascii) {
		byte[] expected = ascii.getBytes(StandardCharsets.US_ASCII);
		if (bytes.length < at + expected.length) {
			return false;
		}
		for (int i = 0; i < expected.length; i++) {
			if (bytes[at + i] != expected[i]) {
				return false;
			}
		}
		return true;
	}

	private static int le16(byte[] bytes, int at) {
		return (bytes[at] & 0xff) | ((bytes[at + 1] & 0xff) << 8);
	}

	private static int le24(byte[] bytes, int at) {
		return le16(bytes, at) | ((bytes[at + 2] & 0xff) << 16);
	}

}
