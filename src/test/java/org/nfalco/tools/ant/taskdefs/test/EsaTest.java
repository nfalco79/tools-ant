package org.nfalco.tools.ant.taskdefs.test;

import java.io.File;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.FileSet;
import org.junit.Test;
import org.nfalco.tools.ant.taskdefs.Esa;

public class EsaTest {

	@Test
	public void esa() {
		File destFile = new File("D:\\git\\tools-ant\\pippo.esa");
		destFile.delete();

		Esa task = new Esa();
		task.setProject(new Project());
		task.setDestFile(destFile);

		FileSet fileset = new FileSet();
		fileset.setDir(new File("D:\\git\\platform.server\\builder\\tmp\\LoggingFeature2052732729"));
		fileset.setIncludes("*.jar");

		task.add(fileset);
		task.execute();
	}

}