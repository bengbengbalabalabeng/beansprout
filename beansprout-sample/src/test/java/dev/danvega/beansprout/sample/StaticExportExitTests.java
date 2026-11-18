package dev.danvega.beansprout.sample;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The export's exit status, the way CI sees it: the sample app run in its own JVM, as
 * {@code java -jar app.jar --beansprout.export.enabled=true} would run it.
 */
class StaticExportExitTests {

	private static Process run(Path folder, Path log, String... extra) throws IOException {
		String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
		List<String> command = new ArrayList<>(List.of(java, "-cp", System.getProperty("java.class.path"),
				SampleApplication.class.getName(), "--beansprout.export.enabled=true",
				"--beansprout.export.output=" + folder));
		command.addAll(List.of(extra));
		return new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(log.toFile()).start();
	}

	private static int exitStatus(Process process, Path log) throws Exception {
		boolean ended = process.waitFor(120, TimeUnit.SECONDS);
		if (!ended) {
			process.destroyForcibly();
		}
		assertThat(ended).as("the app stopped on its own. Its output:\n" + Files.readString(log)).isTrue();
		return process.exitValue();
	}

	@Test
	@DisplayName("EXPORT-7 a successful export exits with status 0")
	void successExitsZero() throws Exception {
		Path dir = Files.createTempDirectory("beansprout-export");
		Path folder = dir.resolve("site");
		Path log = dir.resolve("out.log");
		int status = exitStatus(run(folder, log, "--beansprout.site-url=https://example.com"), log);
		assertThat(status).as(Files.readString(log)).isZero();
		assertThat(folder.resolve("blog.html")).exists();
	}

	@Test
	@DisplayName("EXPORT-8 a failed export exits with a status other than 0")
	void failureExitsNonZero() throws Exception {
		Path dir = Files.createTempDirectory("beansprout-export");
		Path folder = dir.resolve("site");
		Path log = dir.resolve("out.log");
		// No beansprout.site-url, so the export refuses to start.
		int status = exitStatus(run(folder, log), log);
		assertThat(status).as(Files.readString(log)).isNotZero();
		assertThat(Files.readString(log)).contains("beansprout.site-url");
		assertThat(folder).doesNotExist();
	}

}
