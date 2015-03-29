package org.nfalco.tools.ant.taskdefs.test;

import static org.junit.Assert.*;
import static org.ops4j.pax.tinybundles.core.TinyBundles.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Manifest;
import org.apache.tools.ant.taskdefs.Manifest.Attribute;
import org.apache.tools.ant.types.FileSet;
import org.junit.Test;
import org.nfalco.tools.ant.taskdefs.Esa;
import org.nfalco.tools.ant.taskdefs.SubsystemConstants;

import aQute.bnd.osgi.Constants;

public class EsaTest {

	private static final String BUNDLE_SYMBOLICNAME = "esa.bundle.test";
	private static final String BUNDLE_VERSION = "1.0.0";

	public File createBundles() throws IOException {
		InputStream is = bundle() //
				.add(Esa.class) //
				.add(EsaTest.class) //
				.set(Constants.BUNDLE_MANIFESTVERSION, "2") //
				.set(Constants.BUNDLE_VERSION, BUNDLE_VERSION) //
				.set(Constants.BUNDLE_SYMBOLICNAME, BUNDLE_SYMBOLICNAME) //
				.set(Constants.EXPORT_PACKAGE, Esa.class.getPackage().getName()) //
				.build();

		File bundleFile = File.createTempFile("bundle", ".jar");
		bundleFile.deleteOnExit();
		FileUtils.copyInputStreamToFile(is, bundleFile);

		return bundleFile;
	}

	@Test
	public void esa() throws Exception {
		String esaSymbolicName = "org.nfalco.sample";
		String esaVersion = "1.0.0.100";

		File esaFile = File.createTempFile("test", ".esa");
		esaFile.deleteOnExit();
		assertTrue(esaFile.delete());

		Project project = new Project();

		Esa task = new Esa();
		task.setProject(project);
		task.setDestFile(esaFile);
		task.setSymbolicName(esaSymbolicName);
		task.setVersion(esaVersion);

		File bundle = createBundles();
		FileSet fileSet = new FileSet();
		fileSet.setProject(project);
		fileSet.setFile(bundle);

		task.add(fileSet);
		task.execute();

		assertTrue(esaFile.exists());
		assertTrue(esaFile.length() > 0);

		ZipFile zf = new ZipFile(esaFile);
		ZipEntry ze = zf.getEntry(bundle.getName());
		assertNotNull(ze);

		ze = zf.getEntry("OSGI-INF/SUBSYSTEM.MF");
		assertNotNull(ze);
		
		InputStreamReader reader = new InputStreamReader(zf.getInputStream(ze));
		Manifest mf = new Manifest(reader);
		reader.close();

		Attribute attribute = mf.getMainSection().getAttribute(SubsystemConstants.SUBSYSTEM_MANIFEST_VERSION);
		assertNotNull(attribute);
		assertEquals("1", attribute.getValue());
		
		attribute = mf.getMainSection().getAttribute(SubsystemConstants.SUBSYSTEM_SYMBOLIC_NAME);
		assertNotNull(attribute);
		assertEquals(esaSymbolicName, attribute.getValue());

		attribute = mf.getMainSection().getAttribute(SubsystemConstants.SUBSYSTEM_VERSION);
		assertNotNull(attribute);
		assertEquals(esaVersion, attribute.getValue());

		attribute = mf.getMainSection().getAttribute(SubsystemConstants.SUBSYSTEM_CONTENT);
		assertNotNull(attribute);
		assertTrue(attribute.getValue().startsWith(BUNDLE_SYMBOLICNAME));
		assertTrue(attribute.getValue().contains("version=\"" + BUNDLE_VERSION + "\""));
		
		IOUtils.closeQuietly(zf);
	}
}