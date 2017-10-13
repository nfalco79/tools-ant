package org.nfalco.tools.ant.types;

public class GetAttribute extends AbstractMBeanType {

	private String attribute;

	public String getAttribute() {
		return attribute;
	}

	public void setAttribute(String attribute) {
		this.attribute = attribute;
	}

}
