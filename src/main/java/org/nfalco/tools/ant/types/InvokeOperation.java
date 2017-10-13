package org.nfalco.tools.ant.types;

import java.util.ArrayList;
import java.util.Collection;

public class InvokeOperation extends AbstractMBeanType {

	public static class Parameter {
		private String value;
		private String mBeanRefId;

		public String getValue() {
			return value;
		}

		public void setValue(String value) {
			this.value = value;
		}

		public String getmBeanRefId() {
			return mBeanRefId;
		}

		public void setmBeanRefId(String mBeanRefId) {
			this.mBeanRefId = mBeanRefId;
		}

	}

	private String operation;
	private Collection<Parameter> parameters = new ArrayList<Parameter>();

	public String getOperation() {
		return operation;
	}

	public void setOperation(String operation) {
		this.operation = operation;
	}

	public Collection<Parameter> getParameters() {
		return parameters;
	}

	public void setParameters(Collection<Parameter> parmeters) {
		this.parameters = parmeters;
	}

	public void addParameter(Parameter parmeter) {
		parameters.add(parmeter);
	}

}