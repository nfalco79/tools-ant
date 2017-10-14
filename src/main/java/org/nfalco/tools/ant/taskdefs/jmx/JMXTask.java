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
package org.nfalco.tools.ant.taskdefs.jmx;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.management.JMException;
import javax.management.ObjectName;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.condition.Condition;
import org.nfalco.tools.ant.types.AbstractMBeanType;
import org.nfalco.tools.ant.types.GetAttribute;
import org.nfalco.tools.ant.types.InvokeOperation;
import org.nfalco.tools.ant.types.InvokeOperation.Parameter;

import com.j256.simplejmx.client.JmxClient;

public class JMXTask extends Task implements Condition {

	private static final String JAVAX_NET_SSL_TRUST_STORE_PASSWORD = "javax.net.ssl.trustStorePassword";
	private static final String JAVAX_NET_SSL_TRUST_STORE = "javax.net.ssl.trustStore";

	private String url;
	private String user;
	private String password;
	private File trustStore;
	private String trustStorePassword;
	private File reportDir;
	private Collection<AbstractMBeanType> operations = new ArrayList<AbstractMBeanType>();

	private JmxClient client;

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public File getReportDir() {
		return reportDir;
	}

	public void setReportDir(File reportDir) {
		this.reportDir = reportDir;
	}

	@Override
	public void execute() throws BuildException {
		if (url == null || "".equals(url.trim())) {
			throw new BuildException("jmxURL is a required attribute");
		}
		if (operations == null || operations.isEmpty()) {
			throw new BuildException("at least an operation is a required");
		}

		try {
			setupClient();

			for (AbstractMBeanType op : operations) {
				execute(op);
			}
		} catch (Exception e) {
			throw new BuildException(e.getMessage(), e);
		} finally {
			if (client != null) {
				client.close();
			}
		}
	}

	private Object execute(AbstractMBeanType op) throws JMException, Exception {
		final Set<ObjectName> beans = client.getBeanNames(op.getDomain());
		final Iterator<ObjectName> beansIterator = beans.iterator();

		while (beansIterator.hasNext()) {
			final ObjectName bean = beansIterator.next();
			if (accept(op.getDomainAttributes(), bean)) {
				if (op instanceof InvokeOperation) {
					invokeOperation(bean, (InvokeOperation) op);
				} else if (op instanceof GetAttribute) {
					getAttribute(bean, (GetAttribute) op);
				} else {
					throw new IllegalArgumentException("operation not supported");
				}
			}
		}
		return op.getValue();
	}

	private boolean accept(Map<String, String> properties, final ObjectName bean) {
		for (Entry<String, String> property : properties.entrySet()) {
			if (!property.getValue().equals(bean.getKeyProperty(property.getKey()))) {
				return false;
			}
		}
		return true;
	}

	private void getAttribute(final ObjectName bean, GetAttribute op) throws Exception {
		op.setValue(client.getAttribute(bean, op.getAttribute()));
	}

	private void invokeOperation(final ObjectName bean, InvokeOperation op) throws Exception {
		final Collection<Parameter> parameters = op.getParameters();
		Object[] arguments = new Object[parameters.size()];
		int i = 0;
		for (Parameter parameter : parameters) {
			if (parameter.getmBeanRefId() != null) {
				AbstractMBeanType mbean = (AbstractMBeanType) getProject().getReference(parameter.getmBeanRefId());
				arguments[i++] = mbean.getValue();
			} else {
				arguments[i++] = parameter.getValue();
			}
		}
		op.setValue(client.invokeOperation(bean, op.getOperation(), arguments));
	}

	public void add(AbstractMBeanType operation) {
		operations.add(operation);
	}

	@Override
	public boolean eval() throws BuildException {
		if (url == null || "".equals(url.trim())) {
			throw new BuildException("jmxURL is a required attribute");
		}
		if (operations == null || operations.isEmpty() || operations.size() != 1) {
			throw new BuildException("at most an operation is permitted");
		}

		try {
			setupClient();

			AbstractMBeanType op = operations.iterator().next();
			Object result = execute(op);
			if (result instanceof Boolean) {
				return ((Boolean) result).booleanValue();
			} else if (result instanceof String) {
				return Boolean.valueOf((String) result);
			} else {
				throw new BuildException("the result of operation " + op.toString() + " is not a boolean");
			}
		} catch (Exception e) {
			throw new BuildException(e.getMessage(), e);
		} finally {
			if (client != null) {
				client.close();
			}
		}
	}

	private void setupClient() {
		String currentTS = null;
		String currentTSP = null;

		try {
			// very very dirty approach, no other working solution found for to
			// connect client
			if (trustStore != null) {
				currentTS = System.setProperty(JAVAX_NET_SSL_TRUST_STORE, trustStore.getAbsolutePath());
			}
			if (trustStorePassword != null) {
				currentTSP = System.setProperty(JAVAX_NET_SSL_TRUST_STORE_PASSWORD, trustStorePassword);
			}

			client = new JmxClient(url, user, password);
		} catch (JMException e) {
			throw new BuildException("Could not connect to " + url, e);
		} finally {
			if (trustStore != null) {
				if (currentTS == null) {
					System.clearProperty(JAVAX_NET_SSL_TRUST_STORE);
				} else {
					System.setProperty(JAVAX_NET_SSL_TRUST_STORE, currentTS);
				}
			}
			if (trustStorePassword != null) {
				if (currentTSP == null) {
					System.clearProperty(JAVAX_NET_SSL_TRUST_STORE_PASSWORD);
				} else {
					System.setProperty(JAVAX_NET_SSL_TRUST_STORE_PASSWORD, currentTSP);
				}
			}
		}
	}

	public File getTrustStore() {
		return trustStore;
	}

	public void setTrustStore(File trustStore) {
		this.trustStore = trustStore;
	}

	public String getTrustStorePassword() {
		return trustStorePassword;
	}

	public void setTrustStorePassword(String trustStorePassword) {
		this.trustStorePassword = trustStorePassword;
	}

}