/*
 * Copyright 2017. the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package net.tzolov.geode.jta.narayana.application;

import org.assertj.core.api.Condition;
import org.junit.Rule;
import org.junit.Test;

import org.springframework.boot.test.rule.OutputCapture;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Basic integration tests for demo application.
 *
 * @author Christian Tzolov (christian.tzolov@gmail.com)
 */
public class SampleNarayanaApplicationTests {

	static {
		System.setProperty("gemfire.name", "server1");
		System.setProperty("gemfire.server.port", "40406");
		System.setProperty("gemfire.jmx-manager", "false");
		System.setProperty("gemfire.jmx-manager-start", "true");
		System.setProperty("gemfire.dir", "target");
	}


	@Rule
	public OutputCapture outputCapture = new OutputCapture();

	@Test
	public void testTransactionRollback() throws Exception {
		SampleNarayanaApplication.main(new String[] {});
		String output = this.outputCapture.toString();

		assertThat(output).has(substring(1, "-> New message: tzolov"));
		assertThat(output).has(substring(1, "JPA entry count is 1"));
		assertThat(output).has(substring(1, "Simulated error"));
		assertThat(output).has(substring(1, "JAP entry count is still 1"));
	}

	private Condition<String> substring(final int times, final String substring) {
		return new Condition<String>(
				"containing '" + substring + "' " + times + " times") {

			@Override
			public boolean matches(String value) {
				int i = 0;
				while (value.contains(substring)) {
					int beginIndex = value.indexOf(substring) + substring.length();
					value = value.substring(beginIndex);
					i++;
				}
				return i == times;
			}

		};
	}

}
