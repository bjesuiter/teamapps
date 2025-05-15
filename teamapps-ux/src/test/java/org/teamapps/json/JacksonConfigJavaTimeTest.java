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
package org.teamapps.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;


public class JacksonConfigJavaTimeTest {

	private final ObjectMapper teamAppsObjectMapper = TeamAppsObjectMapperFactory.create();

	@Test
	public void instant() throws Exception {
		assertThat(teamAppsObjectMapper.writeValueAsString(Instant.ofEpochMilli(1528910356690L))).isEqualTo("1528910356690");
		assertThat(teamAppsObjectMapper.readValue("1528910356690", Instant.class)).isEqualTo(Instant.ofEpochMilli(1528910356690L));
	}

	@Test
	public void localDateTime() throws Exception {
		assertThat(teamAppsObjectMapper.writeValueAsString(LocalDate.of(2020, 1, 1))).isEqualTo("[2020,1,1]");
		assertThat(teamAppsObjectMapper.readValue("[2020,1,1]", LocalDate.class)).isEqualTo(LocalDate.of(2020, 1, 1));
	}

	@Test
	public void localDate() throws Exception {
		assertThat(teamAppsObjectMapper.writeValueAsString(LocalDateTime.of(2020, 1, 1, 1, 1))).isEqualTo("[2020,1,1,1,1]");
		assertThat(teamAppsObjectMapper.readValue("[2020,1,1,1,1]", LocalDateTime.class)).isEqualTo(LocalDateTime.of(2020, 1, 1, 1, 1));
	}

}
