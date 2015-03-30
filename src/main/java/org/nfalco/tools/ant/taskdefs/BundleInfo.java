package org.nfalco.tools.ant.taskdefs;

class BundleInfo {
	enum ContentType {
		bundle, feature, jar, file
	}

	private String version;
	private String[] exportPackage;
	private String context;
	private String name;
	private ContentType type;

	public ContentType getType() {
		return type;
	}

	public void setType(ContentType type) {
		this.type = type;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String[] getExportPackage() {
		return exportPackage;
	}

	public void setExportPackage(String[] exportPackage) {
		this.exportPackage = exportPackage;
	}

	public String getContext() {
		return context;
	}

	public void setContext(String context) {
		this.context = context;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
