package org.nfalco.tools.ant.taskdefs.test;

import static org.junit.Assert.*;

import java.io.File;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.ZipFileSet;
import org.junit.Test;
import org.nfalco.tools.ant.taskdefs.Esa;

public class EsaTest {

	@Test
	public void esa() throws Exception {
		File esaFile = File.createTempFile("test", ".zip");
//		esaFile.deleteOnExit();
		assertTrue(esaFile.delete());

		Project project = new Project();

		Esa task = new Esa();
		task.setProject(project);
		task.setDestFile(esaFile);
		task.setSymbolicName("org.nfalco.sample");
		task.setVersion("1.0.0.100");

		FileSet fileSet = new FileSet();
		fileSet.setProject(project);
		fileSet.setDir(new File("/Users/nikolasfalco/git/tools-ant/src/test"));
		fileSet.setIncludes("**/*.jar");
		ZipFileSet zipFileSet = new ZipFileSet();
		zipFileSet.setProject(project);
		zipFileSet.setSrc(new File("/Users/nikolasfalco/git/tools-ant/src/test/resources/jul-to-slf4j-1.7.10.jar"));
		zipFileSet.setIncludes("**/*.class");

		task.add(fileSet);
		task.execute();

//		assertTrue(esaFile.exists());
//		assertTrue(esaFile.length() > 0);
	}
}