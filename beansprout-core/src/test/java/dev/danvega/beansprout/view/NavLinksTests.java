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
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class NavLinksTests {

	private final Clock clock = Clock.fixed(Instant.parse("2026-11-20T12:00:00Z"), ZoneOffset.UTC);

	private Pages pages(String root, List<NavLink> links) {
		Posts posts = new Posts(
				() -> List.of(new MarkdownFile("hello.md", "---\ntitle: Hello\ndate: 2026-01-01\n---\nHi.\n")),
				new PostParser(ZoneOffset.UTC), clock, false);
		return new Pages(posts, new Links(root, "/blog"), new BlogSettings("Notes", "", 10, "", "", links));
	}

	@Test
	@DisplayName("THEME-24 the links reach every page's Site, in the order given")
	void inOrderOnEveryPage() {
		Pages pages = pages("", List.of(new NavLink("Shop", "/shop"), new NavLink("Docs", "https://docs.example.com")));

		List<NavLink> expected = List.of(new NavLink("Shop", "/shop"), new NavLink("Docs", "https://docs.example.com"));
		assertThat(pages.index(1).orElseThrow().site().links()).isEqualTo(expected);
		assertThat(pages.post("hello").orElseThrow().site().links()).isEqualTo(expected);
		assertThat(pages.archive().site().links()).isEqualTo(expected);
		assertThat(pages.notFound("/blog/nope").site().links()).isEqualTo(expected);
	}

	@Test
	@DisplayName("THEME-25 a link's path gets the context path in front, and a full address stays as it is")
	void contextPath() {
		Pages pages = pages("/app", List.of(new NavLink("Shop", "/shop"), new NavLink("Docs", "https://docs.example.com")));

		assertThat(pages.index(1).orElseThrow().site().links()).extracting(NavLink::href)
			.containsExactly("/app/shop", "https://docs.example.com");
	}

	@Test
	@DisplayName("THEME-27 with no links set, the menu has none")
	void noneByDefault() {
		assertThat(pages("", List.of()).index(1).orElseThrow().site().links()).isEmpty();
		assertThat(new BlogSettings("Notes", "", 10).links()).isEmpty();
	}

	@Test
	@DisplayName("THEME-26 a link with no label, or an href that is not a path or an http or https address, is refused")
	void refused() {
		assertThatIllegalArgumentException().isThrownBy(() -> new NavLink(" ", "/shop"))
			.withMessageContaining("beansprout.links needs a label");
		assertThatIllegalArgumentException().isThrownBy(() -> new NavLink(null, "/shop"))
			.withMessageContaining("beansprout.links needs a label");
		for (String href : new String[] { "shop", "//evil.example.com", "javascript:alert(1)", "ftp://example.com", "" }) {
			assertThatIllegalArgumentException().as(href).isThrownBy(() -> new NavLink("Shop", href))
				.withMessageContaining("beansprout.links 'Shop' needs an href");
		}
		assertThatIllegalArgumentException().isThrownBy(() -> new NavLink("Shop", null))
			.withMessageContaining("beansprout.links 'Shop' needs an href");
		assertThat(new NavLink("Shop", "HTTPS://Example.com/shop").href()).isEqualTo("HTTPS://Example.com/shop");
	}

}
