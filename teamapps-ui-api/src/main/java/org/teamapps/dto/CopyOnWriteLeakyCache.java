/*-
 * ========================LICENSE_START=================================
 * TeamApps
 * ---
 * Copyright (C) 2014 - 2025 TeamApps.org
 * ---
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =========================LICENSE_END==================================
 */
package org.teamapps.dto;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class CopyOnWriteLeakyCache<K, V> {

	private Map<K, V> map = Collections.emptyMap(); // invariant: this map is never directly modified, only copied!

	public V computeIfAbsent(K key, Function<K, V> computeFunction) {
		V value = map.get(key);
		if (value != null) {
			return value;
		} else {
			HashMap<K, V> mapCopy = new HashMap<>(map);
			value = computeFunction.apply(key);
			mapCopy.put(key, value);
			map = mapCopy;
			return value;
		}
	}

}
