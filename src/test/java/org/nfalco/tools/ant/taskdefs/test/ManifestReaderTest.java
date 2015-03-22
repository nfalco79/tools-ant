package org.nfalco.tools.ant.taskdefs.test;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Properties;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DefaultLogger;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;
import org.junit.Test;

public class ManifestReaderTest {

	/**
	 * To execute a target specified in the Ant build.xml file
	 *
	 * @param buildFile
	 *            the full path of the ant file.
	 * @param target
	 *            the target to execute.
	 * @return the executed project.
	 */
	public static Project executeAntTask(File buildFile, String target) {
		if (!buildFile.exists() || !buildFile.isFile()) {
			throw new RuntimeException(buildFile.getPath() + " does not exists or is not a file");
		}
		DefaultLogger consoleLogger = getConsoleLogger();

		// Prepare Ant project
		Project project = new Project();
		project.setUserProperty("ant.file", buildFile.getAbsolutePath());
		project.addBuildListener(consoleLogger);

		// Capture event for Ant script build start / stop / failure
		try {
			project.fireBuildStarted();
			project.init();
			ProjectHelper projectHelper = ProjectHelper.getProjectHelper();
			project.addReference("ant.projectHelper", projectHelper);
			projectHelper.parse(project, buildFile);

			// If no target specified then default target will be executed.
			String targetToExecute = (target != null && target.trim().length() > 0) ? target.trim() : project.getDefaultTarget();
			project.executeTarget(targetToExecute);
			project.fireBuildFinished(null);
		} catch (BuildException buildException) {
			project.fireBuildFinished(buildException);
			throw buildException;
		}

		return project;
	}

	/**
	 * Logger to log output generated while executing ant script in console.
	 *
	 * @return a configured Ant logger.
	 */
	private static DefaultLogger getConsoleLogger() {
		DefaultLogger consoleLogger = new DefaultLogger();
		consoleLogger.setErrorPrintStream(System.err);
		consoleLogger.setOutputPrintStream(System.out);
		consoleLogger.setMessageOutputLevel(Project.MSG_INFO);

		return consoleLogger;
	}

	@Test
	public void manifestreader_with_prefix() throws Exception {
		URL resource = ManifestReaderTest.class.getResource("manifestreader.xml");
		assertNotNull("build file not found", resource);

		File buildFile = new File(resource.toURI());
		Project project = executeAntTask(buildFile, "manifestreader");

		assertEquals("Wrong value for property mf.Bundle-SymbolicName", "com.example.bundle", project.getProperty("mf.Bundle-SymbolicName"));
		assertEquals("Wrong value for property mf.Section1.Nested-Attribute", "nested.section1", project.getProperty("mf.Section1.Nested-Attribute"));
		assertEquals("Wrong value for property my.own.property", "other.nested.section1", project.getProperty("my.own.property"));
		assertEquals("Wrong value for property mf.Section2.Nested-Attribute", "nested.section2", project.getProperty("mf.Section2.Nested-Attribute"));
	}

	@Test
	public void manifestreader_with_default_mapping_and_prefix() throws Exception {
		URL resource = ManifestReaderTest.class.getResource("manifestreader.xml");
		assertNotNull("build file not found", resource);

		File buildFile = new File(resource.toURI());
		Project project = executeAntTask(buildFile, "manifestreaderMapAllAttributes");

		assertEquals("Wrong value for property mf.Bundle-SymbolicName", "com.example.bundle", project.getProperty("mf.Bundle-SymbolicName"));
		assertEquals("Wrong value for property mf.Section1.Other-Nested-Attribute", "other.nested.section1",
				project.getProperty("mf.Section1.Other-Nested-Attribute"));
		assertEquals("Wrong value for property mf.Section2.Nested-Attribute", "nested.section2", project.getProperty("mf.Section2.Nested-Attribute"));
	}
}
