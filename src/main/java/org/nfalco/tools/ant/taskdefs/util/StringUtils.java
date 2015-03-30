package org.nfalco.tools.ant.taskdefs.util;

import java.util.Collection;

public final class StringUtils {

	private StringUtils() {
	}
	
	public static String join(Collection<String> list, String conjunction) {
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (String item : list) {
			if (first) {
				first = false;
			} else {
				sb.append(conjunction);
			}
			sb.append(item);
		}
		return sb.toString();
	}

	public static boolean isBlank(String value) {
		return value == null || "".equals(value.trim());
	}
}
