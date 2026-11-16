package dev.danvega.beansprout.content;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * One post, read from one Markdown file. Themes never see this type: they get
 * {@code dev.danvega.beansprout.view} records, formatted for reading.
 *
 * @param slug the last part of the post's address
 * @param title the title from frontmatter
 * @param date when the post goes live, in the blog's time zone
 * @param tags the tags as written, without duplicates
 * @param summary the frontmatter summary, or the start of the first paragraph
 * @param html the body as HTML
 * @param headings the {@code ##} headings, in order
 * @param readingMinutes words divided by 200, rounded up, at least 1
 * @param draft true when frontmatter says {@code draft: true}
 * @param path the file the post came from, relative to the posts folder
 */
public record Post(
		String slug,
		String title,
		ZonedDateTime date,
		List<String> tags,
		String summary,
		String html,
		List<Heading> headings,
		int readingMinutes,
		boolean draft,
		String path) {

	public Post {
		tags = List.copyOf(tags);
		headings = List.copyOf(headings);
	}

}
