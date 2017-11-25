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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.DataType;

public abstract class AbstractMBeanType extends DataType {

	private String domain;
	private String fields;
	private String beanName;
	private Object value;

	public Object getValue() {
		return value;
	}

	public void setValue(Object value) {
		this.value = value;
	}

	public String getDomain() {
		return domain;
	}

	public void setDomain(String domain) {
		this.domain = domain;
	}

	public String getBeanName() {
		return beanName;
	}

	public void setBeanName(String beanName) {
		this.beanName = beanName;
	}

	public String getFields() {
		return fields;
	}

	public void setFields(String fields) {
		this.fields = fields;
	}

	public Map<String, String> getDomainAttributes() {
		if (fields == null || "".equals(fields.trim())) {
			return Collections.emptyMap();
		}

		Map<String, String> domainAttributes = new LinkedHashMap<String, String>();
		final StringTokenizer pair = new StringTokenizer(fields, ",");

		while (pair.hasMoreTokens()) {
			final StringTokenizer property = new StringTokenizer(pair.nextToken(), "=");

			final String attrKey = property.nextToken();
			final String attrValue = property.hasMoreTokens() ? property.nextToken() : null;
			domainAttributes.put(attrKey, attrValue);
		}
		return domainAttributes;
	}

	public void validate() {
		if (getDomain() == null) {
			throw new BuildException("domain is required for any JMX operation");
		}
	}

}