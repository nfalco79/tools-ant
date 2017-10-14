package org.nfalco.tools.ant.taskdefs.test;

import static org.junit.Assert.*;

import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.junit.Test;
import org.nfalco.tools.ant.taskdefs.EBA;
import org.nfalco.tools.ant.taskdefs.ESA;
import org.nfalco.tools.ant.taskdefs.FreePort;
import org.nfalco.tools.ant.taskdefs.ManifestReader;
import org.nfalco.tools.ant.taskdefs.jmx.JMXTask;
import org.nfalco.tools.ant.types.GetAttribute;
import org.nfalco.tools.ant.types.InvokeOperation;

public class DescriptorTest {

	private Map<String, Class<?>> tasks = new HashMap<String, Class<?>>();

	public DescriptorTest() {
		tasks.put("freeport", FreePort.class);
		tasks.put("manifestreader", ManifestReader.class);
		tasks.put("esa", ESA.class);
		tasks.put("eba", EBA.class);
		tasks.put("invokeOperation", InvokeOperation.class);
		tasks.put("getAttribute", GetAttribute.class);
		tasks.put("jmx", JMXTask.class);
	}

	@Test
	public void verify_property_file() throws Exception {
		URL resource = DescriptorTest.class.getResource("/org/nfalco/tools/ant/tools.properties");
		assertNotNull("Missing tools.properties taskdef file", resource);

		Properties props = new Properties();
		InputStream is = null;
		try {
			is = resource.openStream();
			props.load(is);
		} finally {
			if (is != null) {
				is.close();
			}
		}

		assertEquals(tasks.size(),  props.size());

		for (Entry<String, Class<?>> task : tasks.entrySet()) {
			String taskName = task.getKey();
			assertTrue("Missing task definition " +  taskName, props.containsKey(taskName));
			assertEquals(task.getValue().getName(), props.getProperty(taskName));
		}
	}

	@Test
	public void verify_xml_file() throws Exception {
		URL resource = DescriptorTest.class.getResource("/org/nfalco/tools/ant/tools.xml");
		assertNotNull("Missing tools.properties taskdef file", resource);

		Map<String, String> taskDekMap = new HashMap<String, String>(2);

		SAXBuilder builder = new SAXBuilder();
		Document document = builder.build(resource);
		Element rootNode = document.getRootElement();
		List<Element> taskDefs = rootNode.getChildren("typedef");
		for (Element taskDef : taskDefs) {
			Attribute attrName = taskDef.getAttribute("name");
			Attribute attrClass = taskDef.getAttribute("classname");
			if (attrName != null) {
				taskDekMap.put(attrName.getValue(), attrClass.getValue());
			}
		}

		assertEquals(tasks.size(), taskDekMap.size());

		for (Entry<String, Class<?>> task : tasks.entrySet()) {
			String taskName = task.getKey();
			assertTrue("Missing task definition " +  taskName, taskDekMap.containsKey(taskName));
			assertEquals(task.getValue().getName(), taskDekMap.get(taskName));
		}
	}
}
