package org.nfalco.tools.ant.types;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringTokenizer;

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

			final String key = property.nextToken();
			final String value = property.hasMoreTokens() ? property.nextToken() : null;
			domainAttributes.put(key, value);
		}
		return domainAttributes;
	}

}