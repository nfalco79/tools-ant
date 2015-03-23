package org.nfalco.tools.ant.taskdefs.test;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.PrintWriter;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Manifest;
import org.apache.tools.ant.util.FileUtils;
import org.junit.Test;
import org.nfalco.tools.ant.taskdefs.ManifestReader;
import org.nfalco.tools.ant.taskdefs.ManifestReader.Attribute;
import org.nfalco.tools.ant.taskdefs.ManifestReader.Section;

public class ManifestReaderTest {

	private File createManifest() throws Exception {
		Manifest mf = new Manifest();

		mf.addConfiguredAttribute(new org.apache.tools.ant.taskdefs.Manifest.Attribute("Bundle-Version", "1.0.0.1"));
		mf.addConfiguredAttribute(new org.apache.tools.ant.taskdefs.Manifest.Attribute("Bundle-SymbolicName", "com.example.bundle"));

		org.apache.tools.ant.taskdefs.Manifest.Section section1 = new org.apache.tools.ant.taskdefs.Manifest.Section();
		section1.setName("Section1");
		section1.addConfiguredAttribute(new org.apache.tools.ant.taskdefs.Manifest.Attribute("Nested-Attribute", "nested.section1"));
		section1.addConfiguredAttribute(new org.apache.tools.ant.taskdefs.Manifest.Attribute("Other-Nested-Attribute", "other.nested.section1"));
		mf.addConfiguredSection(section1);

		org.apache.tools.ant.taskdefs.Manifest.Section section2 = new org.apache.tools.ant.taskdefs.Manifest.Section();
		section2.setName("Section2");
		section2.addConfiguredAttribute(new org.apache.tools.ant.taskdefs.Manifest.Attribute("Nested-Attribute", "nested.section2"));
		mf.addConfiguredSection(section2);

		File mfFile = File.createTempFile("MANIFEST", ".MF");
		PrintWriter writer = new PrintWriter(mfFile);
		mf.write(writer);
		FileUtils.close(writer);

		return mfFile;
	}

	@Test
	public void manifestreader_with_prefix() throws Exception {
		File mfFile = createManifest();

		ManifestReader mfReaderTask = new ManifestReader();
		Project project = AntUtil.createEmptyProject();
		mfReaderTask.setProject(project);

		mfReaderTask.setFile(mfFile);
		mfReaderTask.setPrefix("mf.");
		mfReaderTask.addConfiguredAttribute(new Attribute("Bundle-SymbolicName"));

		Section section1 = new Section("Section1");
		section1.addConfiguredAttribute(new Attribute("Nested-Attribute"));
		section1.addConfiguredAttribute(new Attribute("Other-Nested-Attribute", "my.own.property"));
		mfReaderTask.addConfiguredSection(section1);

		Section section2 = new Section("Section2");
		section2.addConfiguredAttribute(new Attribute("Nested-Attribute"));
		mfReaderTask.addConfiguredSection(section2);

		mfReaderTask.execute();

		assertEquals("Wrong value for property mf.Bundle-SymbolicName", "com.example.bundle", project.getProperty("mf.Bundle-SymbolicName"));
		assertEquals("Wrong value for property mf.Section1.Nested-Attribute", "nested.section1", project.getProperty("mf.Section1.Nested-Attribute"));
		assertEquals("Wrong value for property my.own.property", "other.nested.section1", project.getProperty("my.own.property"));
		assertEquals("Wrong value for property mf.Section2.Nested-Attribute", "nested.section2", project.getProperty("mf.Section2.Nested-Attribute"));
	}

	@Test
	public void manifestreader_with_default_mapping_and_prefix() throws Exception {
		File mfFile = createManifest();

		Project project = AntUtil.createEmptyProject();

		ManifestReader mfReaderTask = new ManifestReader();
		mfReaderTask.setProject(project);
		mfReaderTask.setFile(mfFile);
		mfReaderTask.setPrefix("mf.");

		mfReaderTask.execute();

		assertEquals("Wrong value for property mf.Bundle-SymbolicName", "com.example.bundle", project.getProperty("mf.Bundle-SymbolicName"));
		assertEquals("Wrong value for property mf.Section1.Other-Nested-Attribute", "other.nested.section1", project.getProperty("mf.Section1.Other-Nested-Attribute"));
		assertEquals("Wrong value for property mf.Section2.Nested-Attribute", "nested.section2", project.getProperty("mf.Section2.Nested-Attribute"));
	}
}
