package org.nfalco.tools.ant.taskdefs;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.PropertyHelper;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Manifest;
import org.apache.tools.ant.taskdefs.ManifestException;
import org.apache.tools.ant.util.FileUtils;

public class ManifestReaderTask extends Task {
	/**
	 * An attribute for the manifest. Those attributes that are not nested into
	 * a section will be added to the "Main" section.
	 */
	public static class Attribute {
		private String name;
		private String property;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getProperty() {
			return property;
		}

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
		private Set<Attribute> attributes = new HashSet<ManifestReaderTask.Attribute>(3);

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
		 * @param name the section's name
		 */
		public void setName(String name) {
			this.name = name;
		}

		/**
		 * Add an attribute to the section.
		 *
		 * @param attribute the attribute to be read from the section
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

	public void addConfiguredAttribute(Attribute attribute) throws ManifestException {
        mainSection.addConfiguredAttribute(attribute);
	}

	/**
	 * Add a section to the manifest
	 *
	 * @param section the manifest section to be added
	 */
	public void addConfiguredSection(Section section) throws ManifestException {
        String sectionName = section.getName();
        if (sectionName == null) {
            throw new BuildException("Sections must have a name");
        }
		sections.add(section);
	}

	/**
	 * The file to which the manifest should be written when used as a task
	 */
	private File manifestFile;

	/**
	 * The encoding of the manifest file
	 */
	private String encoding;

	/**
	 * The prefix for all created properties.
	 */
	private String prefix;

	/**
	 * The name of the manifest file to create/update. Required if used as a
	 * task.
	 *
	 * @param f the Manifest file to be written
	 */
	public void setFile(File f) {
		manifestFile = f;
	}

	/**
	 * The encoding to use for reading in an existing manifest file
	 *
	 * @param encoding the manifest file encoding.
	 */
	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}

	/**
	 * Add a name value pair to the project property set
	 *
	 * @param n name of property
	 * @param v value to set
	 * @since Ant 1.8
	 */
	protected void addProperty(String n, Object v) {
		PropertyHelper ph = PropertyHelper.getPropertyHelper(getProject());
		ph.setNewProperty(n, v);
	}

	/**
	 * Create or update the Manifest when used as a task.
	 *
	 * @throws BuildException if the manifest cannot be written.
	 */
	@Override
	public void execute() throws BuildException {
		if (manifestFile == null) {
			throw new BuildException("the file attribute is required");
		}
		if (!manifestFile.exists()) {
			throw new BuildException("Manifest does not exists: " + manifestFile);
		}

		if (prefix == null && sections.isEmpty() && mainSection.attributes.isEmpty()) {
            throw new BuildException("You must specify or a prefix or a nested attribute", getLocation());
		}

		FileInputStream fis = null;
		InputStreamReader isr = null;
		try {
			fis = new FileInputStream(manifestFile);
			if (encoding == null) {
				isr = new InputStreamReader(fis, "UTF-8");
			}
			else {
				isr = new InputStreamReader(fis, encoding);
			}
			Manifest current = new Manifest(isr);

			bindAttributes(mainSection, current.getMainSection());
			for (Section section : sections) {
				Manifest.Section readSection = current.getSection(section.getName());
				if (readSection != null) {
					bindAttributes(section, readSection);
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

	private void bindAttributes(Section section, Manifest.Section readSection) {
		for (Attribute attribute : section.attributes) {
			Manifest.Attribute readAttribute = readSection.getAttribute(attribute.getName());
			if (readAttribute != null) {
				addProperty(attribute.getProperty(), readAttribute.getValue());
			}
		}
	}

}
