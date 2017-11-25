/*
 * Copyright 2017 Nikolas Falco
 * Licensed under the Apache License, Version 2.0 (the
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
package com.github.nfalco79.tools.ant.types.jmx;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.apache.tools.ant.BuildException;

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

		@Override
		public String toString() {
			if (value != null) {
				return value;
			} else if (mBeanRefId != null) {
				return "ref:" + mBeanRefId;
			}
			return "null";
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

	@Override
	public void validate() {
		super.validate();

		if (getOperation() == null) {
			throw new BuildException("operation is required for JMX invokeOperation");
		}

		for (Parameter parameter : parameters) {
			if (parameter.getValue() == null && parameter.getmBeanRefId() == null) {
				throw new BuildException("parameter value is a required attribute for JMX invoke operation. To pass null value use \"null\" string.");
			}
		}
	}

	@Override
	public String toString() {
		return toString(true);
	}

	public String toString(boolean showParameters) {
		StringBuilder sb = new StringBuilder();
		sb.append(super.toString()) //
				.append('[') //
				.append(operation);

		if (showParameters) {
			sb.append('(');
			Iterator<Parameter> pIt = parameters.iterator();
			while (pIt.hasNext()) {
				sb.append(pIt.next().toString());
				if (pIt.hasNext()) {
					sb.append(',');
				}
			}
			sb.append(')');
		} else {
			sb.append("(..)");
		}
		sb.append(']');
		return sb.toString();
	}
}