/*
 * Copyright 2018 Nikolas Falco
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

import static org.mockito.Mockito.*;

import java.lang.reflect.Field;

import javax.management.ObjectName;

import org.apache.tools.ant.Project;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.internal.util.collections.Sets;
import org.mockito.junit.MockitoJUnitRunner;

import com.github.nfalco79.tools.ant.taskdefs.AntUtil;
import com.github.nfalco79.tools.ant.types.jmx.ExistMBean;
import com.j256.simplejmx.client.JmxClient;

@RunWith(MockitoJUnitRunner.class)
public class JMXTaskTest {

    @Mock
    private JmxClient client;

	@Test
	public void exist_MBean() throws Exception {
	    Project project = AntUtil.createEmptyProject();

		ExistMBean operation = new ExistMBean();
        operation.setDomain("com.acme");
		operation.setFields("type=one");

		JMXTask task = mockJMXTask();
		ObjectName mbean1 = mock(ObjectName.class);
		when(mbean1.getKeyProperty("type")).thenReturn("one");
        ObjectName mbean2 = mock(ObjectName.class);
        when(client.getBeanNames(operation.getDomain())).thenReturn(Sets.newSet(mbean1, mbean2));

		task.setProject(project);
		task.setUrl("jmx:foo");
        task.add(operation);

		task.execute();

		Assert.assertThat(operation.getValue(), CoreMatchers.instanceOf(Boolean.class));
		Assert.assertThat((Boolean) operation.getValue(), CoreMatchers.equalTo(true));
	}

    private JMXTask mockJMXTask() throws IllegalAccessException, NoSuchFieldException {
        JMXTask task = spy(new JMXTask());
		Field declaredField = JMXTask.class.getDeclaredField("client");
		declaredField.setAccessible(true);
        declaredField.set(task, client);
		doNothing().when(task).setupClient();
        return task;
    }

}