/*
 *
 *  * Copyright 2015 Skymind,Inc.
 *  *
 *  *    Licensed under the Apache License, Version 2.0 (the "License");
 *  *    you may not use this file except in compliance with the License.
 *  *    You may obtain a copy of the License at
 *  *
 *  *        http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  *    Unless required by applicable law or agreed to in writing, software
 *  *    distributed under the License is distributed on an "AS IS" BASIS,
 *  *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  *    See the License for the specific language governing permissions and
 *  *    limitations under the License.
 *
 */

package org.deeplearning4j.datasets.iterator.impl;


import org.datavec.api.io.labels.ParentPathLabelGenerator;
import org.datavec.api.io.labels.PathLabelGenerator;
import org.datavec.image.loader.LFWLoader;
import org.datavec.image.transform.ImageTransform;
import org.deeplearning4j.datasets.datavec.RecordReaderDataSetIterator;

import java.util.Random;


public class LFWDataSetIterator extends RecordReaderDataSetIterator {

	/** Loads subset of images with given imgDim returned by the generator. */
	public LFWDataSetIterator(int[] imgDim) {
		this(LFWLoader.SUB_NUM_IMAGES, LFWLoader.SUB_NUM_IMAGES, imgDim, LFWLoader.SUB_NUM_LABELS, false, new ParentPathLabelGenerator(), true, 1, null, 0, new Random(System.currentTimeMillis()));
	}

	/** Loads images with given  batchSize, numExamples returned by the generator. */
	public LFWDataSetIterator(int batchSize, int numExamples) {
        this(batchSize, numExamples, new int[] {LFWLoader.HEIGHT, LFWLoader.WIDTH, LFWLoader.CHANNELS}, LFWLoader.NUM_LABELS, false, LFWLoader.LABEL_PATTERN, true, 1, null, 0, new Random(System.currentTimeMillis()));
	}

	/** Loads images with given  batchSize, numExamples, imgDim returned by the generator. */
	public LFWDataSetIterator(int batchSize, int numExamples, int[] imgDim) {
        this(batchSize, numExamples, imgDim, LFWLoader.NUM_LABELS, false, LFWLoader.LABEL_PATTERN, true, 1, null, 0, new Random(System.currentTimeMillis()));
	}

    /** Loads images with given  batchSize, imgDim, useSubset, returned by the generator. */
    public LFWDataSetIterator(int batchSize, int[] imgDim, boolean useSubset)  {
        this(batchSize, useSubset ? LFWLoader.SUB_NUM_IMAGES :LFWLoader.NUM_IMAGES, imgDim, useSubset ? LFWLoader.SUB_NUM_LABELS : LFWLoader.NUM_LABELS, useSubset, LFWLoader.LABEL_PATTERN, true, 1, null, 0, new Random(System.currentTimeMillis()));
    }

    /** Loads images with given  batchSize, numExamples, imgDim, train, & splitTrainTest returned by the generator. */
	public LFWDataSetIterator(int batchSize, int numExamples, int[] imgDim, boolean train, double splitTrainTest) {
        this(batchSize, numExamples, imgDim, LFWLoader.NUM_LABELS, false, LFWLoader.LABEL_PATTERN, train, splitTrainTest, null, 0, new Random(System.currentTimeMillis()));
	}

	/** Loads images with given  batchSize, numExamples, numLabels, train, & splitTrainTest returned by the generator. */
	public LFWDataSetIterator(int batchSize, int numExamples, int numLabels, boolean train, double splitTrainTest) {
        this(batchSize, numExamples, new int[] {LFWLoader.HEIGHT, LFWLoader.WIDTH, LFWLoader.CHANNELS}, numLabels, false, null, train, splitTrainTest, null, 0, new Random(System.currentTimeMillis()));
	}

	/** Loads images with given  batchSize, numExamples, imgDim, numLabels, useSubset, train, splitTrainTest & Random returned by the generator. */
    public LFWDataSetIterator(int batchSize, int numExamples, int[] imgDim, int numLabels, boolean useSubset, boolean train,  double splitTrainTest,  Random rng) {
        this(batchSize, numExamples, imgDim, numLabels, useSubset, LFWLoader.LABEL_PATTERN, train, splitTrainTest, null, 0, rng);
    }

    /** Loads images with given  batchSize, numExamples, imgDim, numLabels, useSubset, train, splitTrainTest & Random returned by the generator. */
    public LFWDataSetIterator(int batchSize, int numExamples, int[] imgDim, int numLabels, boolean useSubset, PathLabelGenerator labelGenerator, boolean train, double splitTrainTest, Random rng) {
        this(batchSize, numExamples, imgDim, numLabels, useSubset, labelGenerator, train, splitTrainTest, null, 0, rng);
    }

	/**
	 * Create LFW data specific iterator
	 * @param batchSize the batch size of the examples
     * @param numExamples the overall number of examples
	 * @param imgDim an array of height, width and channels
	 * @param numLabels the overall number of examples
     * @param useSubset use a subset of the LFWDataSet
     * @param labelGenerator path label generator to use
     * @param train true if use train value
     * @param splitTrainTest the percentage to split data for train and remainder goes to test
     * @param imageTransform how to transform the image
     * @param normalizeValue value to divide pixels by to normalize
     * @param rng random number to lock in batch shuffling
	 * */
	public LFWDataSetIterator(int batchSize, int numExamples, int[] imgDim, int numLabels, boolean useSubset, PathLabelGenerator labelGenerator, boolean train, double splitTrainTest, ImageTransform imageTransform, int normalizeValue, Random rng) {
		super(new LFWLoader(imgDim, imageTransform, normalizeValue, useSubset).getRecordReader(batchSize, numExamples, imgDim, numLabels, labelGenerator, train, splitTrainTest, rng), batchSize, 1, numLabels);
	}

}
