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

import com.gengoai.jcrfsuite.CrfTagger;
import com.gengoai.jcrfsuite.util.Pair;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

/**
 * This example shows how to use jcrfsuite to do POS tagging
 * 
 * @author vinkhu
 *
 */
public class Tag {

	/**
	 * Tag the sequences in a file.
	 * 
	 * @param args
	 *			Model file, test file.
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		if (args.length != 2) {
			System.out.println("Usage: " + Tag.class.getCanonicalName() + " <model file> <test file>");
			System.exit(1);
		}
		String modelFile = args[0];
		String testFile = args[1];
		
		// POS tag
		CrfTagger crfTagger = new CrfTagger(modelFile);
		List<List<Pair<String, Double>>> tagProbLists = crfTagger.tag(testFile);
		
		// Compute accuracy
		int total = 0;
		int correct = 0;
		System.out.println("Gold\tPredict\tProbability");
		
		BufferedReader br = new BufferedReader(new FileReader(testFile));
		String line;
		for (List<Pair<String, Double>> tagProbs: tagProbLists) {
			for (Pair<String, Double> tagProb: tagProbs) {
				String prediction = tagProb.first;
				Double prob = tagProb.second;
				
				line = br.readLine();
				if (line.length() == 0) {
					// End of the sentence, will get word from the next sentence
					line = br.readLine(); 
				}
				String gold = line.split("\t")[0];
				
				System.out.format("%s\t%s\t%.2f\n", gold, prediction, prob);
				total++;
				if (gold.equals(prediction)) {
					correct++;
				}
			}
			System.out.println();
		}
		br.close();
		
		System.out.format("Accuracy = %.2f%%\n", 100. * correct / total);
	}
}
