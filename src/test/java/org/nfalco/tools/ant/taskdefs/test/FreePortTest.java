package org.nfalco.tools.ant.taskdefs.test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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
