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
package com.github.nfalco79.tools.ant.taskdefs;

import static com.github.nfalco79.tools.ant.taskdefs.BundleConstants.*;
import static com.github.nfalco79.tools.ant.taskdefs.IBMSubsystemConstants.*;
import static com.github.nfalco79.tools.ant.taskdefs.SubsystemConstants.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Manifest;
import org.apache.tools.ant.taskdefs.Manifest.Attribute;
import org.apache.tools.ant.taskdefs.ManifestException;
import org.apache.tools.ant.taskdefs.Zip;
import org.apache.tools.ant.types.EnumeratedAttribute;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.ResourceCollection;
import org.apache.tools.ant.types.ZipFileSet;
import org.apache.tools.ant.util.FileUtils;
import org.apache.tools.zip.JarMarker;
import org.apache.tools.zip.ZipExtraField;
import org.apache.tools.zip.ZipOutputStream;

import com.github.nfalco79.tools.ant.taskdefs.BundleInfo.ContentType;
import com.github.nfalco79.tools.ant.taskdefs.util.StringUtils;

public class ESA extends Zip {

	/** The manifest file name. */
	private static final String MANIFEST_NAME = "META-INF/MANIFEST.MF";
	private static final String OSGI_INF = "OSGI-INF/";
	/** The subsystem file name. */
	private static final String SUBSYTEM_NAME = OSGI_INF + "SUBSYSTEM.MF";

	/**
	 * Extra fields needed to make Solaris recognize the archive as a jar file.
	 */
	private static final ZipExtraField[] JAR_MARKER = new ZipExtraField[] { JarMarker.getInstance() };

	private String name;
	private String symbolicName;
	private String version;
	private String license;
	private Visibility visibility = Visibility.PRIVATE;
	private boolean singleton;
	protected Collection<BundleInfo> bundles = new ArrayList<BundleInfo>();

	/**
	 * EnumeratedAttribute covering the visibility types to be checked for,
	 * either public, protected or private.
	 */
	public static class Visibility extends EnumeratedAttribute {

		private static final String[] VALUES = { "public", "protected", "private" };

		/** Private Visibility. */
		public static final Visibility PRIVATE = new Visibility("private");

		/** Protected Visibility. */
		public static final Visibility PROTECTED = new Visibility("protected");

		/** Public Visibility. */
		public static final Visibility PUBLIC = new Visibility("public");

		/**
		 * Default constructor.
		 */
		public Visibility() {
		}

		/**
		 * Construct a new Comparison with the specified value.
		 *
		 * @param value
		 *            the EnumeratedAttribute value.
		 */
		public Visibility(String value) {
			setValue(value);
		}

		/**
		 * {@inheritDoc}.
		 *
		 * @see EnumeratedAttribute#getValues
		 */
		@Override
		public String[] getValues() {
			return Arrays.copyOf(VALUES, VALUES.length);
		}

		/**
		 * Indicate if the visibility is public.
		 *
		 * @return true if the visibility specified is public.
		 */
		public boolean isPublic() {
			return "public".equalsIgnoreCase(getValue());
		}

		/**
		 * Indicate if the visibility is protected.
		 *
		 * @return true if the visibility specified is protected.
		 */
		public boolean isProtected() {
			return "protected".equalsIgnoreCase(getValue());
		}

		/**
		 * Indicate if the visibility is private.
		 *
		 * @return true if the visibility specified is private.
		 */
		public boolean isPrivate() {
			return "private".equalsIgnoreCase(getValue());
		}

	}

	@Override
	protected ArchiveState getResourcesToAdd(ResourceCollection[] rcs, File zipFile, boolean needsUpdate)
			throws BuildException {
		for (ResourceCollection rc : rcs) {
			for (@SuppressWarnings("unchecked")
			Iterator<Resource> iterator = rc.iterator(); iterator.hasNext();) {
				Resource resource = iterator.next();
				try {
					bundles.add(parseManifest(resource));
				} catch (IOException e) {
					log("Could not parse resource " + resource.getName(), Project.MSG_WARN);
				}
			}
		}
		return super.getResourcesToAdd(rcs, zipFile, needsUpdate);
	}

	protected BundleInfo parseManifest(Resource resource) throws IOException {
		BundleInfo BundleInfo = new BundleInfo();

		BundleInfo.setName(new File(resource.getName()).getName());
		if (BundleInfo.getName().endsWith(".jar")) {
			BundleInfo.setType(ContentType.jar);
			if (BundleInfo.getName().lastIndexOf('.') != -1) {
				// remove extension from file
				BundleInfo.setName(BundleInfo.getName().substring(0, BundleInfo.getName().lastIndexOf('.')));
			}

			ZipInputStream zip = null;
			try {
				zip = new ZipInputStream(resource.getInputStream());
				ZipEntry zipEntry;
				while ((zipEntry = zip.getNextEntry()) != null) {
					if (!zipEntry.isDirectory() && MANIFEST_NAME.equals(zipEntry.getName())) {
						Reader reader = new InputStreamReader(zip, Charset.defaultCharset());
						Manifest manifest;
						try {
							manifest = new Manifest(reader);
						} catch (ManifestException e) {
							log("Manifest is invalid: " + e.getMessage(), Project.MSG_ERR);
							throw new BuildException("Invalid Manifest", e, getLocation());
						} finally {
							reader.close();
						}
						String manifestVersion = manifest.getMainSection().getAttributeValue(BUNDLE_MANIFEST_VERSION);
						if (!StringUtils.isBlank(manifestVersion) && Integer.parseInt(manifestVersion) >= 2) {
							BundleInfo bundleInfo = new BundleInfo();
							bundleInfo.setType(ContentType.bundle);
							bundleInfo.setName(manifest.getMainSection().getAttributeValue(BUNDLE_SYMBOLIC_NAME));
							bundleInfo.setVersion(manifest.getMainSection().getAttributeValue(BUNDLE_VERSION));
							bundleInfo.setContext(manifest.getMainSection().getAttributeValue(BUNDLE_CONTEXT_PATH));
							Attribute exportPackage = manifest.getMainSection().getAttribute(BUNDLE_EXPORT_PACKAGE);
							if (exportPackage != null) {
								bundleInfo.setExportPackage(exportPackage.getValue().split(",\\s+"));
							}
							BundleInfo = bundleInfo;
						}
						break;
					}
				}
			} finally {
				if (zip != null) {
					FileUtils.close(zip);
				}
			}
		} else {
			BundleInfo.setType(ContentType.file);
		}
		return BundleInfo;
	}

	@Override
	protected void finalizeZipOutputStream(ZipOutputStream zOut) throws IOException, BuildException {
		if (!skipWriting) {
			writeManifest(zOut, createManifest());
		}
		super.finalizeZipOutputStream(zOut);
	}

	private void writeManifest(ZipOutputStream zOut, Manifest manifest) throws IOException {
		for (@SuppressWarnings("unchecked")
		Enumeration<String> e = manifest.getWarnings(); e.hasMoreElements();) {
			log("Manifest warning: " + e.nextElement(), Project.MSG_WARN);
		}

		zipDir((Resource) null, zOut, getManifestPath(), ZipFileSet.DEFAULT_DIR_MODE, JAR_MARKER);
		// time to write the manifest
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		OutputStreamWriter osw = new OutputStreamWriter(baos, Manifest.JAR_ENCODING);
		PrintWriter writer = new PrintWriter(osw);
		manifest.write(writer);
		if (writer.checkError()) {
			throw new IOException("Encountered an error writing the manifest");
		}
		writer.close();

		ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
		try {
			zipFile(bais, zOut, getManifestFilePath(), System.currentTimeMillis(), null, ZipFileSet.DEFAULT_FILE_MODE);
		} finally {
			// not really required
			FileUtils.close(bais);
		}
	}

	protected String getManifestPath() {
		return OSGI_INF;
	}

	protected String getManifestFilePath() {
		return SUBSYTEM_NAME;
	}

	protected Manifest createManifest() throws BuildException {
		Manifest manifest = Manifest.getDefaultManifest();
		try {
			manifest.addConfiguredAttribute(new Attribute(IBM_FEATURE_VERSION, "2"));
			if (!StringUtils.isBlank(name)) {
				manifest.addConfiguredAttribute(new Attribute(IBM_SHORT_NAME, name));
				manifest.addConfiguredAttribute(new Attribute(SUBSYSTEM_NAME, name));
			}
			if (!StringUtils.isBlank(version)) {
				manifest.addConfiguredAttribute(new Attribute(SUBSYSTEM_VERSION, version));
			}
			if (!StringUtils.isBlank(license)) {
				manifest.addConfiguredAttribute(new Attribute(SUBSYSTEM_LICENSE, license));
			}
			manifest.addConfiguredAttribute(new Attribute(SUBSYSTEM_MANIFEST_VERSION, "1.0"));

			StringBuilder symbolicName = new StringBuilder();
			symbolicName.append(this.symbolicName);
			if (!visibility.isPrivate()) {
				symbolicName.append(";visibility:=").append(visibility.getValue());
			}
			if (singleton) {
				symbolicName.append(";singleton:=").append(singleton);
			}
			manifest.addConfiguredAttribute(new Attribute(SUBSYSTEM_SYMBOLIC_NAME, symbolicName.toString()));
			manifest.addConfiguredAttribute(new Attribute(SUBSYSTEM_TYPE, "osgi.subsystem.feature"));

			if (!bundles.isEmpty()) {
				Collection<String> exportPackages = new ArrayList<String>();
				for (BundleInfo BundleInfo : bundles) {
					if (BundleInfo.getType() == ContentType.bundle && BundleInfo.getExportPackage() != null) {
						BundleInfo bundleInfo = BundleInfo;
						for (int i = 0; i < bundleInfo.getExportPackage().length; i++) {
							if (bundleInfo.getExportPackage()[i].startsWith("javax.")) {
								bundleInfo.getExportPackage()[i] += ";type=\"spec\"";
							}
						}
						exportPackages.addAll(Arrays.asList(bundleInfo.getExportPackage()));
					}
				}
				if (!exportPackages.isEmpty()) {
					manifest.addConfiguredAttribute(
							new Attribute(IBM_API_PACKAGE, StringUtils.join(exportPackages, ", ")));
				}

				Collection<String> content = new ArrayList<String>(bundles.size());
				for (BundleInfo BundleInfo : bundles) {
					switch (BundleInfo.getType()) {
					case bundle:
						BundleInfo bundleInfo = BundleInfo;
						content.add(MessageFormat.format("{0};version=\"{1}\"", bundleInfo.getName(),
								bundleInfo.getVersion()));
						break;
					default:
						content.add(MessageFormat.format("{0};type=\"{1}\"", BundleInfo.getName(),
								BundleInfo.getType().name()));
						break;
					}
				}
				manifest.addConfiguredAttribute(new Attribute(SUBSYSTEM_CONTENT, StringUtils.join(content, ", ")));
			}
		} catch (ManifestException e) {
			log("Manifest is invalid: " + e.getMessage(), Project.MSG_ERR);
			throw new BuildException("Invalid Manifest", e, getLocation());
		}
		return manifest;
	}

	/** constructor */
	public ESA() {
		super();
		archiveType = "esa";
		setEncoding("UTF8");
	}

	@Override
	public void execute() throws BuildException {
		if (symbolicName == null) {
			throw new BuildException("You must specify a symbolic name");
		}
		if (version == null) {
			throw new BuildException("You must specify a version");
		}
		super.execute();
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setVersion(String version) {
		// check version format
		this.version = version;
	}

	public String getVersion() {
		return version;
	}

	public void setSymbolicName(String symbolicName) {
		this.symbolicName = symbolicName;
	}

	public String getSymbolicName() {
		return symbolicName;
	}

	public Visibility getVisibility() {
		return visibility;
	}

	public void setVisibility(Visibility visibility) {
		this.visibility = visibility;
	}

	public void setSingleton(boolean singleton) {
		this.singleton = singleton;
	}

	public void setLicense(String license) {
		this.license = license;
	}
}
