package dev.danvega.beansprout.export;

import java.net.URI;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Runs the export once the app is ready, then stops the app. A failed export is thrown from here,
 * so the app fails to start and exits with a status other than 0.
 */
public final class ExportOnReady implements ApplicationListener<ApplicationReadyEvent> {

	private static final Log LOG = LogFactory.getLog(ExportOnReady.class);

	private final StaticExport export;

	public ExportOnReady(StaticExport export) {
		this.export = export;
	}

	@Override
	public void onApplicationEvent(ApplicationReadyEvent event) {
		ConfigurableApplicationContext context = event.getApplicationContext();
		String port = context.getEnvironment().getProperty("local.server.port");
		if (port == null) {
			throw new ExportException("The static export fetches the blog from the app's own web server, and this app "
					+ "has none. Run it as a servlet web app, without spring.main.web-application-type=none.");
		}
		int written = export.run(URI.create("http://127.0.0.1:" + port));
		LOG.info("Exported " + written + " files to " + export.output());
		SpringApplication.exit(context);
	}

}
