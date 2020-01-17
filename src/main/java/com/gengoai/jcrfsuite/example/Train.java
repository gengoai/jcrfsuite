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

import com.gengoai.jcrfsuite.CrfTrainer;

import java.io.IOException;

/**
 * This example shows how to use jcrfsuite to train a POS model
 */
public class Train {

	/**
	 * Train using the sequences in a file.
	 * 
	 * @param args
	 *			Training file, model file.
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		if (args.length != 2) {
			System.out.println("Usage: " + Train.class.getCanonicalName() + " <train file> <model file>");
			System.exit(1);
		}
		
		String trainFile = args[0];
		String modelFile = args[1];
		CrfTrainer.train(trainFile, modelFile);
	}

}
