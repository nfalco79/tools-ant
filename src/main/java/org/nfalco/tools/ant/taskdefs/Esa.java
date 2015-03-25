package org.nfalco.tools.ant.taskdefs;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringReader;
import java.util.Collection;
import java.util.Collections;
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
	private Map<String, ContentInfo> contentMap = new HashMap<String, Esa.ContentInfo>();

	/**
	 * EnumeratedAttribute covering the file types to be checked for, either
	 * file or dir.
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
		 * @param value the EnumeratedAttribute value.
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
		 * Indicate if the value specifies a directory.
		 *
		 * @return true if the value specifies a directory.
		 */
		public boolean isPublic() {
			return "public".equalsIgnoreCase(getValue());
		}

		/**
		 * Indicate if the value specifies a file.
		 *
		 * @return true if the value specifies a file.
		 */
		public boolean isProtected() {
			return "protected".equalsIgnoreCase(getValue());
		}

		/**
		 * Indicate if the value specifies a file.
		 *
		 * @return true if the value specifies a file.
		 */
		public boolean isPrivate() {
			return "private".equalsIgnoreCase(getValue());
		}

	}

	private class ContentInfo {
		protected ContentType type;
	}

	private class BundleInfo extends ContentInfo {
		private String bundleSymbolicName;
		private String bundleVersion;
		private Collection<String> exportPackage;
		private Collection<String> importPackage;
	}

	private enum ContentType {
		bundle, feature, jar, file
	}

	static byte[] collect(InputStream in) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		copy(in, out);
		return out.toByteArray();
	}

	static void copy(InputStream in, OutputStream out) throws IOException {
		int available = in.available();
		if (available <= 10000)
			available = 64000;
		byte[] buffer = new byte[available];
		int size;
		while ((size = in.read(buffer)) > 0) {
			out.write(buffer, 0, size);
		}
	}

	@Override
	protected void zipFile(InputStream in, ZipOutputStream zOut, String vPath, long lastModified, File fromArchive, int mode) throws IOException {
		ContentInfo contentInfo = new ContentInfo();
		if (vPath.endsWith(".jar")) {
			contentInfo.type = ContentType.jar;

			ZipInputStream zip = new ZipInputStream(in);
			ZipEntry zipEntry;
			while ((zipEntry = zip.getNextEntry()) != null) {
				if (!zipEntry.isDirectory() && MANIFEST_NAME.equals(zipEntry.getName())) {
					byte data[] = collect(zip);
					StringReader reader = new StringReader(new String(data));
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
					if (manifestVersion != null && Integer.valueOf(manifestVersion) >= 2) {
						BundleInfo bundleInfo = new BundleInfo();
						bundleInfo.type = ContentType.bundle;
						bundleInfo.bundleSymbolicName = manifest.getMainSection().getAttributeValue("Bundle-SymbolicName");
						bundleInfo.bundleVersion = manifest.getMainSection().getAttributeValue("Bundle-Version");
						Attribute exportPackage = manifest.getMainSection().getAttribute("Export-Package");
						bundleInfo.importPackage = Collections.list(exportPackage.getValues());
						Attribute importPackage = manifest.getMainSection().getAttribute("Import-Package");
						bundleInfo.exportPackage = Collections.list(importPackage.getValues());
						contentInfo = bundleInfo;
					}
					break;
				}
			}
		}
		else {
			contentInfo.type = ContentType.file;
		}
		contentMap.put(vPath, contentInfo);
		super.zipFile(in, zOut, vPath, lastModified, fromArchive, mode);
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
		for (Enumeration e = manifest.getWarnings(); e.hasMoreElements();) {
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
			manifest.addConfiguredAttribute(new Attribute("IBM-Feature-Version", "2"));
			if (name != null && !"".equals(name)) {
				manifest.addConfiguredAttribute(new Attribute("IBM-ShortName", name));
				manifest.addConfiguredAttribute(new Attribute("Subsystem-Name", name));
			}
			if (version != null && !"".equals(version)) {
				manifest.addConfiguredAttribute(new Attribute("Subsystem-Version", version));
			}
			manifest.addConfiguredAttribute(new Attribute("Subsystem-ManifestVersion", "1"));
			manifest.addConfiguredAttribute(new Attribute("Subsystem-License", getLicense()));
			StringBuilder symbolicName = new StringBuilder();
			symbolicName.append(this.symbolicName);
			if (!visibility.isPrivate()) {
				symbolicName.append("; visibility:=").append(visibility.getValue());
			}
			if (singleton) {
				symbolicName.append("; singleton:=").append(singleton);
			}
			manifest.addConfiguredAttribute(new Attribute("Subsystem-SymbolicName", symbolicName.toString()));

			if (!contentMap.isEmpty()) {
				Attribute api = new Attribute();
				api.setName("IBM-API-Package");
				for (Entry<String, ContentInfo> entry : contentMap.entrySet()) {
					switch (entry.getValue().type) {
					case bundle:
						BundleInfo bundleInfo = (BundleInfo) entry.getValue();
						for (String pkg : bundleInfo.exportPackage) {
							api.addContinuation(pkg);
						}
						break;
					default:
						break;
					}
				}
				manifest.addConfiguredAttribute(api);

				Attribute content = new Attribute();
				content.setName("Subsystem-Content");
				for (Entry<String, ContentInfo> entry : contentMap.entrySet()) {
					StringBuilder sb = new StringBuilder();
					switch (entry.getValue().type) {
					case bundle:
						BundleInfo bundleInfo = (BundleInfo) entry.getValue();
						sb.append(bundleInfo.bundleSymbolicName);
						sb.append("; version=\"").append(bundleInfo.bundleVersion).append('"');
						content.addValue(sb.toString());
						break;
					default:
						sb.append("type=\"").append(entry.getValue().type.name()).append('"');
						content.addValue(sb.toString());
						break;
					}
				}
				manifest.addConfiguredAttribute(content);
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
		emptyBehavior = "create";
		setEncoding("UTF8");
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		// check version format
		this.version = version;
	}

	public String getSymbolicName() {
		return symbolicName;
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

	public boolean isSingleton() {
		return singleton;
	}

	public void setSingleton(boolean singleton) {
		this.singleton = singleton;
	}

	public String getLicense() {
		return license;
	}

	public void setLicense(String license) {
		this.license = license;
	}
}
