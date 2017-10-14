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
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.ResourceCollection;
import org.apache.tools.ant.types.ZipFileSet;
import org.nfalco.tools.ant.taskdefs.BundleInfo.ContentType;
import org.nfalco.tools.ant.taskdefs.util.StringUtils;

public class EBA extends ESA {
	private static final String META_INF = "META-INF/";
	/** The application file name. */
	private static final String APPLICATION_NAME = META_INF + "APPLICATION.MF";

	private Collection<ResourceCollection> wabResources = new ArrayList<ResourceCollection>(1);

	/**
	 * Adds a set of files that can be read from an archive and be given a
	 * prefix/fullpath.
	 *
	 * @param zfs
	 *            the zip fileset to archive.
	 */
	public void addWAB(ZipFileSet zfs) {
		addWAB((ResourceCollection) zfs);
	}

	/**
	 * Adds a collection of resources to be archived.
	 *
	 * @param rc
	 *            the resources to archive.
	 */
	public void addWAB(ResourceCollection rc) {
		wabResources.add(rc);
	}

	@Override
	protected String getManifestPath() {
		return META_INF;
	}

	@Override
	protected String getManifestFilePath() {
		return APPLICATION_NAME;
	}

	// Use-Bundle: javax.jcr;version=2.0.0

	@Override
	protected Manifest createManifest() throws BuildException {
		Manifest manifest = Manifest.getDefaultManifest();
		try {
			manifest.addConfiguredAttribute(new Attribute(APPLICATION_MANIFEST_VERSION, "1.0"));
			if (getName() != null) {
				manifest.addConfiguredAttribute(new Attribute(ApplicationConstants.APPLICATION_NAME, getName()));
			}
			manifest.addConfiguredAttribute(new Attribute(APPLICATION_VERSION, getVersion()));
			manifest.addConfiguredAttribute(new Attribute(APPLICATION_SYMBOLIC_NAME, getSymbolicName()));

			Collection<BundleInfo> applicationContent = new ArrayList<BundleInfo>(bundles);
			for (ResourceCollection rc : wabResources) {
				@SuppressWarnings("unchecked")
				Iterator<Resource> it = rc.iterator();
				while (it.hasNext()) {
					Resource resource = it.next();
					try {
						BundleInfo bundleInfo = parseManifest(resource);
						if (bundleInfo.getType() == ContentType.bundle && bundleInfo.getContext() != null) {
							applicationContent.add(bundleInfo);
						}
					} catch (IOException e) {
						log("Could not parse resource " + resource.getName(), Project.MSG_WARN);
					}
				}
			}

			if (!applicationContent.isEmpty()) {
				Collection<String> bundles = new ArrayList<String>();
				for (BundleInfo bundleInfo : applicationContent) {
					if (bundleInfo.getType() == ContentType.bundle) {
						bundles.add(MessageFormat.format("{0};version=\"{1}\"", bundleInfo.getName(),
								bundleInfo.getVersion()));
					}
				}
				manifest.addConfiguredAttribute(new Attribute(APPLICATION_CONTENT, StringUtils.join(bundles, ", ")));
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
