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

package com.gengoai.jcrfsuite;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.gengoai.jcrfsuite.util.CrfSuiteLoader;
import com.gengoai.jcrfsuite.util.Pair;
import third_party.org.chokkan.crfsuite.ItemSequence;
import third_party.org.chokkan.crfsuite.StringList;
import third_party.org.chokkan.crfsuite.Tagger;

/**
 * An instance of a tagger using CRFsuite.
 */
public class CrfTagger {
	
	static {
		try {
			CrfSuiteLoader.load();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private final Tagger tagger = new Tagger();

	/**
	 * Create a tagger using a model file.
	 * 
	 * @param modelFile The file containing the model for this tagger.
	 */
	public CrfTagger(String modelFile){
		tagger.open(modelFile);
	}

	/**
	 * Tag an item sequence. This method is synchronized so that you do not try to label multiple sequences at the same
	 * time.
	 * 
	 * @param xseq
	 *			The input sequence.
	 * @return For each item in the sequence, a {@link Pair} for each label with the score for the label.
	 */
	public synchronized List<Pair<String, Double>> tag(ItemSequence xseq) {
		List<Pair<String, Double>> predicted = 
				new ArrayList<Pair<String, Double>>();
		
		tagger.set(xseq);
		StringList labels = tagger.viterbi();
		for (int i = 0; i < labels.size(); i++) {
			String label = labels.get(i);
			predicted.add(new Pair<String, Double>(
					label, tagger.marginal(label, i)));
		}
		
		return predicted;
	}
	
	/**
	 * Tag text in file. This calls a synchronized method so that you do not try to label multiple sequences at the same
	 * time.
	 * 
	 * @param fileName
	 *			The name of the file containing sequences to label.
	 * @return For each sequence in the file, for each item in the sequence, a {@link Pair} for each label with the
	 *		 score for the label
	 * @throws IOException
	 *			 If there is a problem using the file.
	 */
	public List<List<Pair<String, Double>>> tag(String fileName) throws IOException {
		
		List<List<Pair<String, Double>>> taggedSentences = 
				new ArrayList<List<Pair<String, Double>>>();

		Pair<List<ItemSequence>, List<StringList>> taggingSequences = CrfTrainer.loadTrainingInstances(fileName, CrfTrainer.DEFAULT_ENCODING);
		for (ItemSequence xseq: taggingSequences.getFirst()) {
			taggedSentences.add(tag(xseq));
		}
		
		return taggedSentences;
	}
	
	/**
	 * Tag text in file. This calls a synchronized method so that you do not try to label multiple sequences at the same
	 * time.
	 * 
	 * @param fileName
	 *			The name of the file containing sequences to label.
	 * @param encoding
	 * 			Encoding of fileName file
	 * @return For each sequence in the file, for each item in the sequence, a {@link Pair} for each label with the
	 *		 score for the label
	 * @throws IOException
	 *			 If there is a problem using the file.
	 */
	public List<List<Pair<String, Double>>> tag(String fileName, String encoding) throws IOException {
		
		List<List<Pair<String, Double>>> taggedSentences = 
				new ArrayList<List<Pair<String, Double>>>();

		Pair<List<ItemSequence>, List<StringList>> taggingSequences = CrfTrainer.loadTrainingInstances(fileName, encoding);
		for (ItemSequence xseq: taggingSequences.getFirst()) {
			taggedSentences.add(tag(xseq));
		}
		
		return taggedSentences;
	}

	/**
	 * @return The possible labels for this tagger.
	 */
	public List<String> getlabels() {
		StringList labels = tagger.labels();
		int numLabels = (int) labels.size();
		List<String> result = new ArrayList<>(numLabels);
		for (int labelIndex = 0; labelIndex < numLabels; ++labelIndex) {
			result.add(labels.get(labelIndex));
		}
		return result;
	}
}
