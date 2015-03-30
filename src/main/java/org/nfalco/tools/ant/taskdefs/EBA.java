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

public class EBA extends Esa {
	/** The application file name. */
	private static final String APPLICATION_NAME = "META-INF/APPLICATION.MF";

	private FileSet content;
	private Collection<BundleInfo> wab = new ArrayList<BundleInfo>();

	public void addContent(FileSet fileSet) {
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
						ContentInfo contentInfo = parseManifest(resource);
						if (contentInfo.getType() == ContentType.bundle) {
							BundleInfo bundleInfo = (BundleInfo) contentInfo;
							if (bundleInfo.getContext() != null) {
								wab.add(bundleInfo);
							}
						}
					} catch (IOException e) {
						log("Could not parse resource " + resource.getName(), Project.MSG_WARN);
					}
				}

				Collection<ContentInfo> content = new ArrayList<ContentInfo>(bundles);
				content.addAll(wab);
				if (!content.isEmpty()) {
					Collection<String> bundles = new ArrayList<String>();
					for (ContentInfo contentInfo : content) {
						if (contentInfo.type == ContentType.bundle) {
							BundleInfo bundleInfo = (BundleInfo) contentInfo;
							MessageFormat.format("{0};version=\"{1}\"", bundleInfo.getName(), bundleInfo.getVersion());
						}
					}
					manifest.addConfiguredAttribute(new Attribute(APPLICATION_CONTENT, join(bundles, ", ")));
				}
			}
		} catch (ManifestException e) {
			log("Manifest is invalid: " + e.getMessage(), Project.MSG_ERR);
			throw new BuildException("Invalid Manifest", e, getLocation());
		}

		return manifest;
	}
}
