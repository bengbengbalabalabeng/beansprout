package dev.danvega.beansprout.autoconfigure;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

class AutoConfigurationImportsTests {

	private static final Path IMPORTS = Path.of(
			"src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports");

	@Test
	@DisplayName("every @AutoConfiguration class is registered, and nothing else is")
	void importsFileMatchesTheAnnotatedClasses() throws IOException {
		Set<String> registered = Files.readAllLines(IMPORTS).stream()
			.map(String::strip)
			.filter(line -> !line.isEmpty() && !line.startsWith("#"))
			.collect(Collectors.toSet());

		Set<String> annotated = new ClassFileImporter()
			.withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
			.importPackages("dev.danvega.beansprout")
			.stream()
			.filter(type -> type.isAnnotatedWith(AutoConfiguration.class))
			.map(JavaClass::getName)
			.collect(Collectors.toSet());

		assertThat(registered).containsExactlyInAnyOrderElementsOf(annotated);
	}

}
