package dev.danvega.beansprout.view;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import dev.danvega.beansprout.content.MarkdownFile;
import dev.danvega.beansprout.content.PostParser;
import dev.danvega.beansprout.content.Posts;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** The app's own HTML on every page, from specs/theme-slots.md. */
class SlotsTests {

	private static final String HEAD = "<script defer src=\"/stats.js\" data-site=\"a&b\"></script>\n";

	private static final String AFTER_POST = "<form action=\"/subscribe\"><input name=\"email\"> <b>Join</b></form>\n";

	private static final String FOOTER = "<p>Hosted &amp; <em>loved</em> by me</p>\n";

	private final Posts posts = new Posts(
			() -> List.of(new MarkdownFile("hello.md", "---\ntitle: Hello\ndate: 2026-11-01\ntags: [java]\n---\nHi.\n")),
			new PostParser(ZoneOffset.UTC), Clock.fixed(Instant.parse("2026-11-16T12:00:00Z"), ZoneOffset.UTC), false);

	private Pages pages(Slots slots) {
		return new Pages(posts, new Links("", "/blog"), new BlogSettings("Bean Notes", "", 10), slots);
	}

	private static List<Site> everyPage(Pages pages) {
		return List.of(pages.index(1).orElseThrow().site(), pages.post("hello").orElseThrow().site(),
				pages.tag("java", 1).orElseThrow().site(), pages.archive().site(), pages.notFound("/blog/nope").site());
	}

	@Test
	@DisplayName("SLOT-4 each slot reaches the theme as it is, without escaping, on every page")
	void asItIs() {
		Pages pages = pages(new Slots(HEAD, AFTER_POST, FOOTER)).forSite("https://example.com");

		for (Site site : everyPage(pages)) {
			assertThat(site.slots().head()).isEqualTo(HEAD);
			assertThat(site.slots().afterPost()).isEqualTo(AFTER_POST);
			assertThat(site.slots().footer()).isEqualTo(FOOTER);
		}
	}

	@Test
	@DisplayName("SLOT-5 with no slot files every slot is empty")
	void emptyWithoutFiles() {
		assertThat(Slots.NONE).isEqualTo(new Slots("", "", ""));
		Pages plain = new Pages(posts, new Links("", "/blog"), new BlogSettings("Bean Notes", "", 10));
		for (Site site : everyPage(plain)) {
			assertThat(site.slots()).isEqualTo(Slots.NONE);
		}
		for (Site site : everyPage(pages(Slots.NONE).forSite("https://example.com"))) {
			assertThat(site.slots()).isEqualTo(Slots.NONE);
		}
	}

}
