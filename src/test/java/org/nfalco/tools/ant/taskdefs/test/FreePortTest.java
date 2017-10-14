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
package org.nfalco.tools.ant.taskdefs.test;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

import org.apache.tools.ant.Project;
import org.junit.Test;
import org.nfalco.tools.ant.taskdefs.FreePort;

public class FreePortTest {

	@Test
	public void get_free_port() throws Exception {
		String fpPropName = "freeport";

		Project project = AntUtil.createEmptyProject();

		FreePort task = new FreePort();
		task.setProject(project);

		task.setProperty(fpPropName);
		task.execute();

		String fpPropValue = project.getProperty(fpPropName);
		assertNotNull(fpPropValue);

		Integer port = Integer.valueOf(fpPropValue);
		assertTrue(port > 0);

		Socket socket = new Socket();
		try {
			socket.bind(new InetSocketAddress(port));
		} catch (IOException e) {
			fail(e.getMessage());
		} finally {
			socket.close();
		}
	}
}
