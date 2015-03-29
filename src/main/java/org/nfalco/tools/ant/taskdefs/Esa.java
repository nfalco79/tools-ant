package org.nfalco.tools.ant.taskdefs;

import static org.nfalco.tools.ant.taskdefs.SubsystemConstants.*;
import static org.nfalco.tools.ant.taskdefs.IBMSubsystemConstants.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
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
import org.apache.tools.ant.types.resources.FileResource;
import org.apache.tools.ant.util.FileUtils;
import org.apache.tools.zip.JarMarker;
import org.apache.tools.zip.ZipExtraField;
import org.apache.tools.zip.ZipOutputStream;

public class Esa extends Zip {

	/** The manifest file name. */
	private static final String MANIFEST_NAME = "META-INF/MANIFEST.MF";
	/** The subsystem file name. */
	private static final String SUBSYTEM_NAME = "OSGI-INF/SUBSYSTEM.MF";
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
	private Map<String, ContentInfo> contentMap = new HashMap<String, Esa.ContentInfo>();

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
		 * @see EnumeratedAttribute#getValues
		 */
		/** {@inheritDoc}. */
		@Override
		public String[] getValues() {
			return VALUES;
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

	private class ContentInfo {
		protected String name;
		protected ContentType type;
	}

	private class BundleInfo extends ContentInfo {
		private String version;
		private String[] exportPackage;
	}

	private enum ContentType {
		bundle, feature, jar, file
	}

	private static String join(Collection<String> list, String conjunction) {
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (String item : list) {
			if (first) {
				first = false;
			} else {
				sb.append(conjunction);
			}
			sb.append(item);
		}
		return sb.toString();
	}

	private static boolean isBlank(String value) {
		return value == null || "".equals(value.trim());
	}

	@Override
	protected ArchiveState getResourcesToAdd(ResourceCollection[] rcs, File zipFile, boolean needsUpdate) throws BuildException {
		return super.getResourcesToAdd(rcs, zipFile, needsUpdate);
	}

	@Override
	public void add(ResourceCollection rc) {
		// TODO parseManifest here
		super.add(rc);
	}

	@Override
	protected void zipFile(File file, ZipOutputStream zOut, String vPath, int mode) throws IOException {
		parseManifest(vPath, new FileResource(file));
		super.zipFile(file, zOut, vPath, mode);
	}

	private void parseManifest(String vPath, Resource resource) throws IOException {
		ContentInfo contentInfo = new ContentInfo();

		contentInfo.name = new File(vPath).getName();
		if (contentInfo.name.lastIndexOf('.') != -1) {
			// remove extension from file
			contentInfo.name = contentInfo.name.substring(0, contentInfo.name.lastIndexOf('.'));
		}

		if (vPath.endsWith(".jar")) {
			contentInfo.type = ContentType.jar;

			ZipInputStream zip = null;
			try {
				zip = new ZipInputStream(resource.getInputStream());
				ZipEntry zipEntry;
				while ((zipEntry = zip.getNextEntry()) != null) {
					if (!zipEntry.isDirectory() && MANIFEST_NAME.equals(zipEntry.getName())) {
						Reader reader = new InputStreamReader(zip);
						Manifest manifest;
						try {
							manifest = new Manifest(reader);
						} catch (ManifestException e) {
							log("Manifest is invalid: " + e.getMessage(), Project.MSG_ERR);
							throw new BuildException("Invalid Manifest", e, getLocation());
						} finally {
							reader.close();
						}
						String manifestVersion = manifest.getMainSection().getAttributeValue("Bundle-ManifestVersion");
						if (!isBlank(manifestVersion) && Integer.valueOf(manifestVersion) >= 2) {
							BundleInfo bundleInfo = new BundleInfo();
							bundleInfo.type = ContentType.bundle;
							bundleInfo.name = manifest.getMainSection().getAttributeValue("Bundle-SymbolicName");
							bundleInfo.version = manifest.getMainSection().getAttributeValue("Bundle-Version");
							Attribute exportPackage = manifest.getMainSection().getAttribute("Export-Package");
							bundleInfo.exportPackage = exportPackage.getValue().split(",\\s+");
							contentInfo = bundleInfo;
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
			contentInfo.type = ContentType.file;
		}
		contentMap.put(vPath, contentInfo);
	}

	@Override
	protected void finalizeZipOutputStream(ZipOutputStream zOut) throws IOException, BuildException {
		if (!skipWriting) {
			Manifest esaManifest = createManifest();
			writeManifest(zOut, esaManifest);
		}
		super.finalizeZipOutputStream(zOut);
	}

	private void writeManifest(ZipOutputStream zOut, Manifest manifest) throws IOException {
		for (@SuppressWarnings("unchecked")
		Enumeration<String> e = manifest.getWarnings(); e.hasMoreElements();) {
			log("Manifest warning: " + e.nextElement(), Project.MSG_WARN);
		}

		zipDir((Resource) null, zOut, "OSGI-INF/", ZipFileSet.DEFAULT_DIR_MODE, JAR_MARKER);
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
			super.zipFile(bais, zOut, SUBSYTEM_NAME, System.currentTimeMillis(), null, ZipFileSet.DEFAULT_FILE_MODE);
		} finally {
			// not really required
			FileUtils.close(bais);
		}
	}

	protected Manifest createManifest() throws BuildException {
		Manifest manifest = Manifest.getDefaultManifest();
		try {
			manifest.addConfiguredAttribute(new Attribute(IBM_FEATURE_VERSION, "2"));
			if (!isBlank(name)) {
				manifest.addConfiguredAttribute(new Attribute(IBM_SHORT_NAME, name));
				manifest.addConfiguredAttribute(new Attribute(SUBSYSTEM_NAME, name));
			}
			if (!isBlank(version)) {
				manifest.addConfiguredAttribute(new Attribute(SUBSYSTEM_VERSION, version));
			}
			if (!isBlank(license)) {
				manifest.addConfiguredAttribute(new Attribute(SUBSYSTEM_LICENSE, license));
			}
			manifest.addConfiguredAttribute(new Attribute(SUBSYSTEM_MANIFEST_VERSION, "1"));

			StringBuilder symbolicName = new StringBuilder();
			symbolicName.append(this.symbolicName);
			if (!visibility.isPrivate()) {
				symbolicName.append("; visibility:=").append(visibility.getValue());
			}
			if (singleton) {
				symbolicName.append("; singleton:=").append(singleton);
			}
			manifest.addConfiguredAttribute(new Attribute(SUBSYSTEM_SYMBOLIC_NAME, symbolicName.toString()));

			if (!contentMap.isEmpty()) {
				Collection<String> exportPackages = new ArrayList<String>();
				for (ContentInfo contentInfo : contentMap.values()) {
					if (contentInfo.type == ContentType.bundle) {
						BundleInfo bundleInfo = (BundleInfo) contentInfo;
						exportPackages.addAll(Arrays.asList(bundleInfo.exportPackage));
					}
				}
				if (!exportPackages.isEmpty()) {
					manifest.addConfiguredAttribute(new Attribute(IBM_API_PACKAGE, join(exportPackages, ", ")));
				}

				Collection<String> content = new ArrayList<String>(contentMap.size());
				for (Entry<String, ContentInfo> entry : contentMap.entrySet()) {
					ContentInfo contentInfo = entry.getValue();
					switch (contentInfo.type) {
					case bundle:
						BundleInfo bundleInfo = (BundleInfo) contentInfo;
						content.add(MessageFormat.format("{0}; version=\"{1}\"", bundleInfo.name, bundleInfo.version));
						break;
					default:
						content.add(MessageFormat.format("{0}; type=\"{1}\"", contentInfo.name, contentInfo.type.name()));
						break;
					}
				}
				manifest.addConfiguredAttribute(new Attribute(SUBSYSTEM_CONTENT, join(content, ", ")));
			}
		} catch (ManifestException e) {
			log("Manifest is invalid: " + e.getMessage(), Project.MSG_ERR);
			throw new BuildException("Invalid Manifest", e, getLocation());
		}
		return manifest;
	}

	/** constructor */
	public Esa() {
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

	public void setVersion(String version) {
		// check version format
		this.version = version;
	}

	public void setSymbolicName(String symbolicName) {
		this.symbolicName = symbolicName;
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
