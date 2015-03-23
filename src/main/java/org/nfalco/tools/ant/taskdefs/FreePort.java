/* ==========================================================
 * ant-get-free-port
 * https://github.com/backstop/ant-get-free-port
 * ==========================================================
 * Copyright 2012 Backstop Solutions Group, LLC.
 * Authors: George Shakhnazaryan (gshakhnazaryan@backstopsolutions.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ========================================================== */

package org.nfalco.tools.ant.taskdefs;

import java.io.IOException;
import java.net.Socket;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.PropertyHelper;
import org.apache.tools.ant.Task;

/**
 * Sets a property by name with a first system ephemeral port number.
 * If system doesn't provide a free port, property will not be registered.
 *
 * @author Nikolas Falco
 *
 */
public class FreePort extends Task {
	private String property;

	/**
	 * The name of the property to set.
	 *
	 * @param name
	 *            property name
	 */
	public void setProperty(String property) {
		this.property = property;
	}

	/**
	 * Get the property name.
	 *
	 * @return the property name
	 */
	public String getProperty() {
		return this.property;
	}

	@Override
	public void execute() {
		if (getProject() == null) {
			throw new IllegalStateException("project has not been set");
		}

		if (property == null) {
			throw new BuildException("You must specify property attribute",
					getLocation());
		}

		Socket socket = new Socket();
		try {
			socket.bind(null);
			int port = socket.getLocalPort();
			if (port != -1) {
				addProperty(this.property, String.valueOf(port));
			}
		} catch (IOException e) {
			throw new BuildException(e);
		} finally {
			try {
				socket.close();
			} catch (IOException e) {
			}
		}
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
}