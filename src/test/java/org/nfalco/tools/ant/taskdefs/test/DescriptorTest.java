package org.nfalco.tools.ant.taskdefs.test;

import static org.junit.Assert.*;

import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.junit.Test;
import org.nfalco.tools.ant.taskdefs.Esa;
import org.nfalco.tools.ant.taskdefs.FreePort;
import org.nfalco.tools.ant.taskdefs.ManifestReader;

public class DescriptorTest {

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
		
		assertTrue("Missing task definition freeport", props.containsKey("freeport"));
		assertEquals(FreePort.class.getName(), (String) props.getProperty("freeport"));

		assertTrue("Missing task definition manifestreader", props.containsKey("manifestreader"));
		assertEquals(ManifestReader.class.getName(), (String) props.getProperty("manifestreader"));
	}

	@Test
	public void verify_xml_file() throws Exception {
		URL resource = DescriptorTest.class.getResource("/org/nfalco/tools/ant/tools.xml");
		assertNotNull("Missing tools.properties taskdef file", resource);
		
		Map<String, String> taskDekMap = new HashMap<String, String>(2);

		SAXBuilder builder = new SAXBuilder();
		Document document = (Document) builder.build(resource);
		Element rootNode = document.getRootElement();
		List<Element> taskDefs = rootNode.getChildren("typedef");
		for (Element taskDef : taskDefs) {
			Attribute attrName = taskDef.getAttribute("name");
			Attribute attrClass = taskDef.getAttribute("classname");
			if (attrName != null) { 
				taskDekMap.put(attrName.getValue(), attrClass.getValue());
			}
		}
		
		assertEquals(3,  taskDefs.size());
		
		assertTrue("Missing task definition freeport", taskDekMap.containsKey("freeport"));
		assertEquals(FreePort.class.getName(), (String) taskDekMap.get("freeport"));
		
		assertTrue("Missing task definition manifestreader", taskDekMap.containsKey("manifestreader"));
		assertEquals(ManifestReader.class.getName(), (String) taskDekMap.get("manifestreader"));

		assertTrue("Missing task definition esa", taskDekMap.containsKey("esa"));
		assertEquals(Esa.class.getName(), (String) taskDekMap.get("esa"));
	}
}
