package dev.danvega.beansprout.web;

import java.util.Optional;
import java.util.regex.Pattern;

import dev.danvega.beansprout.view.Pages;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

/** The blog pages: index, post, tag and archive, under {@code beansprout.base-path}. */
@Controller
@RequestMapping("${beansprout.base-path:/blog}")
public class BlogController {

	private static final Pattern PAGE_NUMBER = Pattern.compile("[1-9][0-9]{0,8}");

	private static final MediaType HTML = new MediaType(MediaType.TEXT_HTML, java.nio.charset.StandardCharsets.UTF_8);

	private final Pages pages;

	private final ThemeRenderer theme;

	public BlogController(Pages pages, ThemeRenderer theme) {
		this.pages = pages;
		this.theme = theme;
	}

	@GetMapping({ "", "/" })
	public ResponseEntity<String> index(HttpServletRequest request) {
		return render("index", pages.index(1), request);
	}

	@GetMapping("/page/{number}")
	public ResponseEntity<String> page(@PathVariable String number, HttpServletRequest request) {
		if (number.equals("1")) {
			return redirect(pages.links().home());
		}
		return render("index", pageNumber(number).flatMap(pages::index), request);
	}

	@GetMapping("/archive")
	public ResponseEntity<String> archive(HttpServletRequest request) {
		return render("archive", Optional.of(pages.archive()), request);
	}

	@GetMapping("/tags/{tag}")
	public ResponseEntity<String> tag(@PathVariable String tag, HttpServletRequest request) {
		return render("tag", pages.tag(tag, 1), request);
	}

	@GetMapping("/tags/{tag}/page/{number}")
	public ResponseEntity<String> tagPage(@PathVariable String tag, @PathVariable String number,
			HttpServletRequest request) {
		if (number.equals("1")) {
			return pages.tag(tag, 1).isPresent() ? redirect(pages.links().tag(tag)) : render("tag", Optional.empty(), request);
		}
		return render("tag", pageNumber(number).flatMap(n -> pages.tag(tag, n)), request);
	}

	@GetMapping("/{slug}")
	public ResponseEntity<String> post(@PathVariable String slug, HttpServletRequest request) {
		return render("post", pages.post(slug), request);
	}

	private ResponseEntity<String> render(String template, Optional<?> page, HttpServletRequest request) {
		if (page.isEmpty()) {
			String html = theme.render("not-found", pages.notFound(request.getRequestURI()));
			return ResponseEntity.status(HttpStatus.NOT_FOUND).contentType(HTML).body(html);
		}
		return ResponseEntity.ok().contentType(HTML).body(theme.render(template, page.get()));
	}

	private static ResponseEntity<String> redirect(String location) {
		return ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY).header(HttpHeaders.LOCATION, location).build();
	}

	private static Optional<Integer> pageNumber(String text) {
		return PAGE_NUMBER.matcher(text).matches() ? Optional.of(Integer.parseInt(text)) : Optional.empty();
	}

}
