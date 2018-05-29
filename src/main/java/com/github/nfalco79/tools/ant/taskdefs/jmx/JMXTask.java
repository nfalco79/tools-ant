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
package com.github.nfalco79.tools.ant.taskdefs.jmx;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.management.JMException;
import javax.management.ObjectName;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.condition.Condition;

import com.github.nfalco79.tools.ant.types.jmx.AbstractMBeanType;
import com.github.nfalco79.tools.ant.types.jmx.ExistMBean;
import com.github.nfalco79.tools.ant.types.jmx.GetAttribute;
import com.github.nfalco79.tools.ant.types.jmx.InvokeOperation;
import com.github.nfalco79.tools.ant.types.jmx.InvokeOperation.Parameter;
import com.j256.simplejmx.client.JmxClient;

public class JMXTask extends Task implements Condition {

	private String url;
	private String user;
	private String password;
	private Collection<AbstractMBeanType> operations = new ArrayList<AbstractMBeanType>();

	private JmxClient client;
	private boolean isCondition;

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

	@Override
	public void execute() {
		if (url == null || "".equals(url.trim())) {
			throw new BuildException("jmxURL is a required attribute");
		}
		if (operations == null || operations.isEmpty()) {
			throw new BuildException("at least an operation is a required");
		}
		for (AbstractMBeanType operation : operations) {
			operation.validate();
		}

		try {
			setupClient();

			log(operations.size() + " JMX operation to perform");
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

    private Object execute(AbstractMBeanType op) throws Exception {
        final Set<ObjectName> beans = client.getBeanNames(op.getDomain());
        final Iterator<ObjectName> beansIterator = beans.iterator();

        if (!beansIterator.hasNext()) {
            throw new BuildException("Domain " + op.getDomain() + " not found for " + op.toString());
        }

        while (beansIterator.hasNext()) {
            final ObjectName bean = beansIterator.next();
            boolean matches = matches(op.getDomainAttributes(), bean);
            if (matches) {
                if (op instanceof ExistMBean) {
                    op.setValue(true);
                    break;
                } else if (op instanceof InvokeOperation) {
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

	private boolean matches(Map<String, String> properties, final ObjectName bean) {
		for (Entry<String, String> property : properties.entrySet()) {
			if (!property.getValue().equals(bean.getKeyProperty(property.getKey()))) {
				return false;
			}
		}
		return true;
	}

	private void getAttribute(final ObjectName bean, GetAttribute op) throws Exception {
		if (!isCondition) {
			log("get attribute " + op.getAttribute());
		}

		op.setValue(client.getAttribute(bean, op.getAttribute()));
		log(op.toString(), Project.MSG_DEBUG);
	}

	private void invokeOperation(final ObjectName bean, InvokeOperation op) throws Exception {
		if (!isCondition) {
			log("invoke operation " + op.getOperation());
		}

		final Collection<Parameter> parameters = op.getParameters();
		Object[] arguments = new Object[parameters.size()];
		int i = 0;
		for (Parameter parameter : parameters) {
			if (parameter.getmBeanRefId() != null) {
				AbstractMBeanType mbean = (AbstractMBeanType) getProject().getReference(parameter.getmBeanRefId());
				if (mbean == null) {
					throw new BuildException("The referred MBean " + parameter.getmBeanRefId() + " does not exists");
				}
				arguments[i++] = mbean.getValue();
			} else {
				String value = parameter.getValue();
				if ("null".equals(value)) {
					value = null;
				}
				arguments[i++] = value;
			}
		}

		log(op.toString(), Project.MSG_DEBUG);
		op.setValue(client.invokeOperation(bean, op.getOperation(), arguments));
	}

	public void add(AbstractMBeanType operation) {
		operations.add(operation);
	}

	@Override
	public boolean eval() {
		if (url == null || "".equals(url.trim())) {
			throw new BuildException("jmxURL is a required attribute");
		}
		if (operations == null || operations.isEmpty() || operations.size() != 1) {
			throw new BuildException("at most an operation is permitted");
		}

		isCondition = true;
		try {
			setupClient();

			AbstractMBeanType op = operations.iterator().next();
			Object result = execute(op);
			if (result instanceof Boolean) {
				return ((Boolean) result).booleanValue();
			} else if (result instanceof String) {
				return Boolean.valueOf((String) result);
			} else {
				throw new BuildException("the result of " + op.toString() + " is not a boolean");
			}
		} catch (Exception e) {
			throw new BuildException(e.getMessage(), e);
		} finally {
			if (client != null) {
				client.close();
			}
		}
	}

	void setupClient() {
		log("setup jmx client", Project.MSG_DEBUG);

		try {
			log("connecting to " + url + " with user " + user + " and password " + password, Project.MSG_VERBOSE);
			client = new JmxClient(url, user, password);
		} catch (JMException e) {
			throw new BuildException("Could not connect to " + url, e);
		}
	}

}