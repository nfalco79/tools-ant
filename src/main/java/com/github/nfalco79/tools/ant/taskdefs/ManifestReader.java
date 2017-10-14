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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.PropertyHelper;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Manifest;
import org.apache.tools.ant.taskdefs.ManifestException;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.ResourceCollection;
import org.apache.tools.ant.types.resources.FileResource;
import org.apache.tools.ant.types.resources.Resources;
import org.apache.tools.ant.util.FileUtils;

/**
 * Parse the data of a jar manifest.
 *
 * <p>
 * Manifests are processed according to the
 * <a href="http://java.sun.com/j2se/1.5.0/docs/guide/jar/jar.html">Jar file
 * specification</a> . Specifically, a manifest element consists of a set of
 * attributes and sections. These sections in turn may contain attributes.
 * <p>
 * Each read attribute is mapped to an ant property composed with the
 * {@code <section name>.<attribute name>} except for attributes in the main
 * section.
 */
public class ManifestReader extends Task {
	/**
	 * An attribute for the manifest. Those attributes that are not nested into
	 * a section will be added to the "Main" section.
	 */
	public static class Attribute {
		private String name;
		private String property;

		public Attribute(String name) {
			this.name = name;
		}

		public Attribute(String name, String property) {
			this.name = name;
			this.property = property;
		}

		/**
		 * Get the Attribute's name.
		 *
		 * @return the attribute's name.
		 */
		public String getName() {
			return name;
		}

		/**
		 * Set the Attribute's name; required.
		 *
		 * @param name
		 *            the attribute's name
		 */
		public void setName(String name) {
			this.name = name;
		}

		/**
		 * Gets the Ant property's name where is mapped this attribute.
		 *
		 * @return the property's name.
		 */
		public String getProperty() {
			return property;
		}

		/**
		 * Sets the Ant property's name where map this attribute.
		 *
		 * @param property
		 *            name.
		 */
		public void setProperty(String property) {
			this.property = property;
		}
	}

	/**
	 * A manifest section - you can nest attribute elements into sections. A
	 * section consists of a set of attribute values, separated from other
	 * sections by a blank line.
	 */
	public static class Section {

		/**
		 * The section's name if any. The main section in a manifest is unnamed.
		 */
		private String name;

		/**
		 * The section's attributes.
		 */
		private Set<Attribute> attributes = new HashSet<ManifestReader.Attribute>(3);

		public Section() {
		}

		public Section(String name) {
			this.name = name;
		}

		/**
		 * Get the Section's name.
		 *
		 * @return the section's name.
		 */
		public String getName() {
			return name;
		}

		/**
		 * The name of the section; optional -default is the main section.
		 *
		 * @param name
		 *            the section's name
		 */
		public void setName(String name) {
			this.name = name;
		}

		/**
		 * Add an attribute to the section.
		 *
		 * @param attribute
		 *            the attribute to be read from the section
		 */
		public void addConfiguredAttribute(Attribute attribute) {
			String attributeName = attribute.getName();
			if (attributeName == null) {
				throw new BuildException("Attributes must have a name");
			}
			attributes.add(attribute);
		}
	}

	private Section mainSection = new Section();
	private Set<Section> sections = new HashSet<Section>();
	private Resources resources = new Resources();

	public void addConfiguredAttribute(Attribute attribute) throws ManifestException {
		mainSection.addConfiguredAttribute(attribute);
	}

	public void add(ResourceCollection resources) throws ManifestException {
		this.resources.add(resources);
	}

	/**
	 * Adds a section to the manifest.
	 *
	 * @param section
	 *            the manifest section to be added.
	 */
	public void addConfiguredSection(Section section) {
		String sectionName = section.getName();
		if (sectionName == null) {
			throw new BuildException("Sections must have a name");
		}
		sections.add(section);
	}

	/**
	 * The file to which the manifest should be read when used as a task
	 */
	private File manifestFile;

	/**
	 * The encoding of the manifest file
	 */
	private String encoding;

	/**
	 * Prefix for created properties.
	 */
	private String prefix;

	/**
	 * The name of the manifest file to read. Required if used as a task.
	 *
	 * @param f
	 *            the Manifest file to be written
	 */
	public void setFile(File f) {
		manifestFile = f;
	}

	/**
	 * Set the prefix to load these properties under.
	 *
	 * @param prefix
	 *            to set
	 */
	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}

	/**
	 * The encoding to use for reading in an existing manifest file
	 *
	 * @param encoding
	 *            the manifest file encoding.
	 */
	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}

	/**
	 * Add a name value pair to the project property set
	 *
	 * @param n
	 *            name of property
	 * @param v
	 *            value to set
	 */
	protected void addProperty(String n, Object v) {
		PropertyHelper ph = PropertyHelper.getPropertyHelper(getProject());
		ph.setNewProperty(n, v);
	}

	/**
	 * Read the Manifest when used as a task.
	 *
	 * @throws BuildException
	 *             if the manifest cannot be read.
	 */
	@Override
	public void execute() throws BuildException {
		if (manifestFile == null && resources.size() == 0) {
			throw new BuildException("the file attribute or a nested resource is required");
		}
		if (manifestFile != null && !manifestFile.exists()) {
			throw new BuildException("Manifest does not exists: " + manifestFile);
		}

		resources.add(new FileResource(manifestFile));
		if (resources.size() == 0) {
			throw new BuildException("nested resource is empty");
		}

		InputStream is = null;
		InputStreamReader isr = null;
		Manifest current = null;
		try {
			@SuppressWarnings("rawtypes")
			Iterator resIt = resources.iterator();
			while (resIt.hasNext()) {
				Object res = resIt.next();
				if (!(res instanceof Resource)) {
					continue;
				}
				Resource resource = (Resource) res;
				is = resource.getInputStream();
				if (encoding == null) {
					isr = new InputStreamReader(is, "UTF-8");
				} else {
					isr = new InputStreamReader(is, encoding);
				}
				if (current == null) {
					current = new Manifest(isr);
				} else {
					current.merge(new Manifest(isr));
				}
			}

			// if no sections was specified all attributes will be read, copy
			// all attributes from loaded manifest
			if (sections.isEmpty() && mainSection.attributes.isEmpty()) {
				mapAllAttributes(current);
			}

			if (current == null) {
				return;
			}

			bindAttributes(mainSection, current.getMainSection());
			for (Section section : sections) {
				Manifest.Section readSection = current.getSection(section.getName());
				if (readSection != null) {
					bindAttributes(section, readSection);
				} else {
					log("Unable to find attribute " + section.getName(), Project.MSG_INFO);
				}
			}
		} catch (ManifestException m) {
			throw new BuildException("Existing manifest " + manifestFile + " is invalid", m, getLocation());
		} catch (IOException e) {
			throw new BuildException("Failed to read " + manifestFile, e, getLocation());
		} finally {
			FileUtils.close(isr);
		}

	}

	private void mapAllAttributes(Manifest main) throws ManifestException {
		copyAttributes(main.getMainSection(), mainSection);

		@SuppressWarnings("rawtypes")
		Enumeration sectionNames = main.getSectionNames();
		while (sectionNames.hasMoreElements()) {
			org.apache.tools.ant.taskdefs.Manifest.Section readSection = main
					.getSection((String) sectionNames.nextElement());
			Section section = new Section(readSection.getName());
			copyAttributes(readSection, section);
			addConfiguredSection(section);
		}
	}

	private void copyAttributes(Manifest.Section sourceSection, Section destSection) {
		@SuppressWarnings("rawtypes")
		Enumeration attributeKeys = sourceSection.getAttributeKeys();

		while (attributeKeys.hasMoreElements()) {
			org.apache.tools.ant.taskdefs.Manifest.Attribute readAttribute = sourceSection
					.getAttribute((String) attributeKeys.nextElement());
			Attribute attribute = new Attribute(readAttribute.getName());
			destSection.addConfiguredAttribute(attribute);
		}
	}

	private void bindAttributes(Section section, Manifest.Section readSection) {
		for (Attribute attribute : section.attributes) {
			Manifest.Attribute readAttribute = readSection.getAttribute(attribute.getName());
			if (readAttribute != null) {
				String property = attribute.getProperty();
				if (property == null) {
					if (section == mainSection) {
						property = prefix + attribute.getName();
					} else {
						property = prefix + section.name + '.' + attribute.getName();
					}
				}
				addProperty(property, readAttribute.getValue());
			} else {
				log("Unable to find attribute " + attribute.getName(), Project.MSG_INFO);
			}
		}
	}

}