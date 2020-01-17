/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
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

package com.gengoai.jcrfsuite.example;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Test the example.
 * 
 * @author Justin Harris (github.com/juharris)
 */
public class TrainTagExampleTest {

	private static final Path TRAINING_FOLDER = Paths.get(System.getProperty("user.dir"))
			.resolve("src")
			.resolve("test")
			.resolve("resources")
			.resolve("com")
			.resolve("gengoai")
			.resolve("jcrfsuite")
			.resolve("trainer");

	private static final Path MODEL_PATH = TRAINING_FOLDER.resolve("twitter-pos.model");

	@Before
	public void setUpTest() throws IOException {
		// Delete the model to make sure we make a new one.
		Files.deleteIfExists(MODEL_PATH);
	}

	@After
	public void teardownTest() throws IOException {
		// Delete the model to make sure we make a new one next time and to make extra sure it's not commited.
		Files.deleteIfExists(MODEL_PATH);
	}

	@Test
	public void testMain() throws IOException {
		Train.main(new String[] {
				"data/tweet-pos/train-oct27.txt",
				MODEL_PATH.toString()
		});
		Tag.main(new String[] {
				MODEL_PATH.toString(),
				"data/tweet-pos/test-daily547.txt"
		});
	}
}
