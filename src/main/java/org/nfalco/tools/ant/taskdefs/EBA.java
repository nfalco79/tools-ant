package org.nfalco.tools.ant.taskdefs;

import static org.nfalco.tools.ant.taskdefs.ApplicationConstants.*;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Manifest;
import org.apache.tools.ant.taskdefs.Manifest.Attribute;
import org.apache.tools.ant.taskdefs.ManifestException;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Resource;
import org.nfalco.tools.ant.taskdefs.BundleInfo.ContentType;
import org.nfalco.tools.ant.taskdefs.util.StringUtils;

public class EBA extends ESA {
	/** The application file name. */
	private static final String APPLICATION_NAME = "META-INF/APPLICATION.MF";

	private FileSet content;
	private Collection<BundleInfo> wab = new ArrayList<BundleInfo>();

	public void addExtraFileSet(FileSet fileSet) {
		this.content = fileSet;
	}

	@Override
	protected String getManifestPath() {
		return APPLICATION_NAME;
	}

	// Use-Bundle: javax.jcr;version=2.0.0

	@Override
	protected Manifest createManifest() throws BuildException {
		Manifest manifest = Manifest.getDefaultManifest();
		try {
			manifest.addConfiguredAttribute(new Attribute(APPLICATION_MANIFEST_VERSION, "1.0"));
			manifest.addConfiguredAttribute(new Attribute(ApplicationConstants.APPLICATION_NAME, getName()));
			manifest.addConfiguredAttribute(new Attribute(APPLICATION_VERSION, getVersion()));
			manifest.addConfiguredAttribute(new Attribute(APPLICATION_SYMBOLIC_NAME, getSymbolicName()));
			if (content != null && content.size() > 0) {
				@SuppressWarnings("unchecked")
				Iterator<Resource> it = content.iterator();
				while (it.hasNext()) {
					Resource resource = it.next();
					try {
						BundleInfo bundleInfo = parseManifest(resource);
						if (bundleInfo.getType() == ContentType.bundle && bundleInfo.getContext() != null) {
							wab.add(bundleInfo);
						}
					} catch (IOException e) {
						log("Could not parse resource " + resource.getName(), Project.MSG_WARN);
					}
				}

				Collection<BundleInfo> applicationContent = new ArrayList<BundleInfo>(bundles);
				applicationContent.addAll(wab);

				if (!applicationContent.isEmpty()) {
					Collection<String> bundles = new ArrayList<String>();
					for (BundleInfo bundleInfo : applicationContent) {
						if (bundleInfo.getType() == ContentType.bundle) {
							bundles.add(MessageFormat.format("{0};version=\"{1}\"", bundleInfo.getName(), bundleInfo.getVersion()));
						}
					}
					manifest.addConfiguredAttribute(new Attribute(APPLICATION_CONTENT, StringUtils.join(bundles, ", ")));
				}
			}
		} catch (ManifestException e) {
			log("Manifest is invalid: " + e.getMessage(), Project.MSG_ERR);
			throw new BuildException("Invalid Manifest", e, getLocation());
		}

		return manifest;
	}

	public EBA() {
		super();
		archiveType = "eba";
	}
}
