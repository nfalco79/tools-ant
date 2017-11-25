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
package com.github.nfalco79.tools.ant.taskdefs.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

public final class StringUtils {

	private StringUtils() {
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static String join(Collection<String> elements, String conjunction) {
		return toString((Collection) elements, conjunction);
	}

	public static String toString(Collection<Object> elements, String conjunction) {
		StringBuilder sb = new StringBuilder();
		Iterator<Object> it = elements.iterator();
		while (it.hasNext()) {
			sb.append(it.next());
			if (it.hasNext()) {
				sb.append(conjunction);
			}
		}
		return sb.toString();
	}

	public static String toString(Object[] arguments, String conjunction) {
		return toString(Arrays.asList(arguments), conjunction);
	}

	public static boolean isBlank(String value) {
		return value == null || "".equals(value.trim());
	}
}
