package org.nfalco.tools.ant.taskdefs.test;

import java.io.File;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DefaultLogger;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;

final class AntUtil {

	/**
	 * To execute the specified target in the Ant build.xml file.
	 *
	 * @param buildFile
	 *            the full path of the ant file.
	 * @param target
	 *            the target to execute.
	 * @return the executed project.
	 */
	public static Project executeAntTarget(File buildFile, String target) {
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
	
	public static Project createEmptyProject() {
		DefaultLogger consoleLogger = getConsoleLogger();

		// Prepare Ant project
		Project project = new Project();
		project.addBuildListener(consoleLogger);

		// Capture event for Ant script build start / stop / failure
		try {
			project.fireBuildStarted();
			project.init();
			ProjectHelper projectHelper = ProjectHelper.getProjectHelper();
			project.addReference("ant.projectHelper", projectHelper);

			project.fireBuildFinished(null);
		} catch (BuildException buildException) {
			project.fireBuildFinished(buildException);
			throw buildException;
		}

		return project;
	}
}
