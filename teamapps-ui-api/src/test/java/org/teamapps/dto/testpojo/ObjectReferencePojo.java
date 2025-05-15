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
package org.teamapps.dto.testpojo;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.teamapps.dto.ObjectDeserializer;
import org.teamapps.dto.ObjectSerializer;

public class ObjectReferencePojo {
	@JsonSerialize(using = ObjectSerializer.class)
	@JsonDeserialize(using = ObjectDeserializer.class)
	private Object o;

	public ObjectReferencePojo() {
	}

	public ObjectReferencePojo(Object o) {
		this.o = o;
	}

	public Object getO() {
		return o;
	}

	public void setO(Object o) {
		this.o = o;
	}
}
