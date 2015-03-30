package org.nfalco.tools.ant.taskdefs;

import static org.nfalco.tools.ant.taskdefs.BundleConstants.*;
import static org.nfalco.tools.ant.taskdefs.IBMSubsystemConstants.*;
import static org.nfalco.tools.ant.taskdefs.SubsystemConstants.*;

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
	protected Collection<ContentInfo> bundles = new ArrayList<ContentInfo>();

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

	class ContentInfo {
		protected String name;
		protected ContentType type;

		public String getName() {
			return name;
		}

		public ContentType getType() {
			return type;
		}
	}

	class BundleInfo extends ContentInfo {
		private String version;
		private String[] exportPackage;
		private String context;

		public String getVersion() {
			return version;
		}

		public String[] getExportPackage() {
			return exportPackage;
		}

		public String getContext() {
			return context;
		}
	}

	enum ContentType {
		bundle, feature, jar, file
	}

	protected static String join(Collection<String> list, String conjunction) {
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
		for (ResourceCollection rc : rcs) {
			for (@SuppressWarnings("unchecked") Iterator<Resource> iterator = rc.iterator(); iterator.hasNext();) {
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

	protected ContentInfo parseManifest(Resource resource) throws IOException {
		ContentInfo contentInfo = new ContentInfo();

		contentInfo.name = new File(resource.getName()).getName();
		if (contentInfo.getName().lastIndexOf('.') != -1) {
			// remove extension from file
			contentInfo.name = contentInfo.getName().substring(0, contentInfo.getName().lastIndexOf('.'));
		}

		if (contentInfo.getName().endsWith(".jar")) {
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
						String manifestVersion = manifest.getMainSection().getAttributeValue(BUNDLE_MANIFEST_VERSION);
						if (!isBlank(manifestVersion) && Integer.valueOf(manifestVersion) >= 2) {
							BundleInfo bundleInfo = new BundleInfo();
							bundleInfo.type = ContentType.bundle;
							bundleInfo.name = manifest.getMainSection().getAttributeValue(BUNDLE_SYMBOLIC_NAME);
							bundleInfo.version = manifest.getMainSection().getAttributeValue(BUNDLE_VERSION);
							bundleInfo.context = manifest.getMainSection().getAttributeValue(BUNDLE_CONTEXT_PATH);
							Attribute exportPackage = manifest.getMainSection().getAttribute(BUNDLE_EXPORT_PACKAGE);
							if (exportPackage != null) {
								bundleInfo.exportPackage = exportPackage.getValue().split(",\\s+");
							}
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
		return contentInfo;
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
			zipFile(bais, zOut, getManifestPath(), System.currentTimeMillis(), null, ZipFileSet.DEFAULT_FILE_MODE);
		} finally {
			// not really required
			FileUtils.close(bais);
		}
	}

	protected String getManifestPath() {
		return SUBSYTEM_NAME;
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
				symbolicName.append(";visibility:=").append(visibility.getValue());
			}
			if (singleton) {
				symbolicName.append(";singleton:=").append(singleton);
			}
			manifest.addConfiguredAttribute(new Attribute(SUBSYSTEM_SYMBOLIC_NAME, symbolicName.toString()));

			if (!bundles.isEmpty()) {
				Collection<String> exportPackages = new ArrayList<String>();
				for (ContentInfo contentInfo : bundles) {
					if (contentInfo.type == ContentType.bundle && ((BundleInfo) contentInfo).getExportPackage() != null) {
						BundleInfo bundleInfo = (BundleInfo) contentInfo;
						for (int i = 0; i < bundleInfo.getExportPackage().length; i++) {
							if (bundleInfo.getExportPackage()[i].startsWith("javax.")) {
								bundleInfo.getExportPackage()[i] += ";type=\"spec\"";
							}
						}
						exportPackages.addAll(Arrays.asList(bundleInfo.getExportPackage()));
					}
				}
				if (!exportPackages.isEmpty()) {
					manifest.addConfiguredAttribute(new Attribute(IBM_API_PACKAGE, join(exportPackages, ", ")));
				}

				Collection<String> content = new ArrayList<String>(bundles.size());
				for (ContentInfo contentInfo : bundles) {
					switch (contentInfo.getType()) {
					case bundle:
						BundleInfo bundleInfo = (BundleInfo) contentInfo;
						content.add(MessageFormat.format("{0};version=\"{1}\"", bundleInfo.getName(), bundleInfo.getVersion()));
						break;
					default:
						content.add(MessageFormat.format("{0};type=\"{1}\"", contentInfo.getName(), contentInfo.getType().name()));
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
