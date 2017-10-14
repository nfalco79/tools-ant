/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.github.nfalco79.tools.ant.taskdefs.test;

import static org.junit.Assert.*;
import static org.ops4j.pax.tinybundles.core.TinyBundles.bundle;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.io.FileUtils;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Manifest;
import org.apache.tools.ant.taskdefs.Manifest.Attribute;
import org.apache.tools.ant.types.FileSet;
import org.junit.Test;

import com.github.nfalco79.tools.ant.taskdefs.ESA;
import com.github.nfalco79.tools.ant.taskdefs.SubsystemConstants;

import aQute.bnd.osgi.Constants;

public class ESATest {

	private static final String BUNDLE_SYMBOLICNAME = "esa.bundle.test";
	private static final String BUNDLE_VERSION = "1.0.0";

	private File createBundles() throws IOException {
		InputStream is = bundle() //
		.add(ESA.class) //
		.add(ESATest.class) //
		.set(Constants.BUNDLE_MANIFESTVERSION, "2") //
		.set(Constants.BUNDLE_VERSION, BUNDLE_VERSION) //
		.set(Constants.BUNDLE_SYMBOLICNAME, BUNDLE_SYMBOLICNAME) //
		.set(Constants.EXPORT_PACKAGE, ESA.class.getPackage().getName()) //
		.build();

		File bundleFile = File.createTempFile("bundle", ".jar");
		FileUtils.copyInputStreamToFile(is, bundleFile);

		return bundleFile;
	}

	@Test
	public void esa() throws Exception {
		String esaSymbolicName = "org.nfalco.sample";
		String esaVersion = "1.0.0.100";

		File esaFile = File.createTempFile("test", ".esa");
		esaFile.delete();

		Project project = AntUtil.createEmptyProject();

		ESA task = new ESA();
		task.setProject(project);
		task.setDestFile(esaFile);
		task.setSymbolicName(esaSymbolicName);
		task.setVersion(esaVersion);

		File bundle = createBundles();
		FileSet fileSet = new FileSet();
		fileSet.setProject(project);
		fileSet.setFile(bundle);

		task.add(fileSet);
		try {
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
			assertEquals("1.0", attribute.getValue());

			attribute = mf.getMainSection().getAttribute(SubsystemConstants.SUBSYSTEM_SYMBOLIC_NAME);
			assertNotNull(attribute);
			assertEquals(esaSymbolicName, attribute.getValue());

			attribute = mf.getMainSection().getAttribute(SubsystemConstants.SUBSYSTEM_VERSION);
			assertNotNull(attribute);
			assertEquals(esaVersion, attribute.getValue());

			attribute = mf.getMainSection().getAttribute(SubsystemConstants.SUBSYSTEM_TYPE);
			assertNotNull(attribute);
			assertEquals("osgi.subsystem.feature", attribute.getValue());

			attribute = mf.getMainSection().getAttribute(SubsystemConstants.SUBSYSTEM_CONTENT);
			assertNotNull(attribute);
			assertTrue(attribute.getValue().startsWith(BUNDLE_SYMBOLICNAME));
			assertTrue(attribute.getValue().contains("version=\"" + BUNDLE_VERSION + "\""));

			zf.close();
		} finally {
			if (!esaFile.delete()) {
				esaFile.deleteOnExit();
			}
		}
	}
}