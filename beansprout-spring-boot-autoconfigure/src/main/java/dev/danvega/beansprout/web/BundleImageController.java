package dev.danvega.beansprout.web;

import java.io.IOException;

import dev.danvega.beansprout.content.Bundles;
import dev.danvega.beansprout.content.Posts;

import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * The images of a post that is a folder, at the post's address: {@code /blog/welcome/cover.png}
 * serves {@code cover.png} from the folder that holds {@code welcome}'s {@code index.md}. Only a
 * published post's images are served, only image files, and never anything outside its folder.
 * {@code Last-Modified} lets a browser ask again cheaply and get a 304.
 */
@Controller
@RequestMapping("${beansprout.base-path:/blog}")
public class BundleImageController {

	private final Posts posts;

	private final BundleFiles files;

	public BundleImageController(Posts posts, BundleFiles files) {
		this.posts = posts;
		this.files = files;
	}

	@GetMapping("/{slug}/{*file}")
	public ResponseEntity<byte[]> image(@PathVariable String slug, @PathVariable String file) {
		String path = Bundles.relative(file.startsWith("/") ? file.substring(1) : file);
		String type = path == null ? null : Bundles.imageType(path);
		String folder = posts.find(slug).map(post -> Bundles.folder(post.path())).orElse(null);
		if (path == null || type == null || folder == null) {
			return ResponseEntity.notFound().build();
		}
		Resource image = files.find(folder, path);
		if (image == null) {
			return ResponseEntity.notFound().build();
		}
		// Bytes, not the Resource: every Spring MVC setup can write a byte array. Blog images are small.
		try {
			return ResponseEntity.ok()
				.contentType(MediaType.parseMediaType(type))
				.lastModified(image.lastModified())
				.body(image.getContentAsByteArray());
		}
		catch (IOException ex) {
			// Gone since it was found, or not a file after all, such as a folder in a jar.
			return ResponseEntity.notFound().build();
		}
	}

}
