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

package org.deeplearning4j.text.movingwindow;

import org.deeplearning4j.models.word2vec.Word2Vec;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.indexing.INDArrayIndex;
import org.nd4j.linalg.indexing.NDArrayIndex;

import java.util.List;


/**
 * Util methods for converting windows to 
 * training examples
 * @author Adam Gibson    
 *
 */
public class WindowConverter {


    private WindowConverter() {
    }

    /**
     * Converts a window (each word in the window)
     *
     * in to a vector.
     *
     * Keep in mind each window is a multi word context.
     *
     * From there, each word uses the passed in model
     * as a lookup table to getFromOrigin what vectors are relevant
     * to the passed in windows
     * @param window the window to take in.
     * @param vec the model to use as a lookup table
     * @return a concacneated 1 row array
     * containing all of the numbers for each word in the window
     */
    public static INDArray asExampleArray(Window window,Word2Vec vec,boolean normalize) {
        int length = vec.lookupTable().layerSize();
        List<String> words = window.getWords();
        int windowSize = vec.getWindow();
        assert words.size() == vec.getWindow();
        INDArray ret = Nd4j.create(length * windowSize);



        for(int i = 0; i < words.size(); i++) {
            String word = words.get(i);
            INDArray n = normalize ? vec.getWordVectorMatrixNormalized(word) :  vec.getWordVectorMatrix(word);
            ret.put(new INDArrayIndex[]{NDArrayIndex.interval(i * vec.lookupTable().layerSize(),i * vec.lookupTable().layerSize() + vec.lookupTable().layerSize())},n);
        }

        return ret;
    }






    /**
     * Converts a window (each word in the window)
     *
     * in to a vector.
     *
     * Keep in mind each window is a multi word context.
     *
     * From there, each word uses the passed in model
     * as a lookup table to getFromOrigin what vectors are relevant
     * to the passed in windows
     * @param window the window to take in.
     * @param vec the model to use as a lookup table
     * @return a concatneated 1 row array
     * containing all of the numbers for each word in the window
     */
	public static INDArray asExampleMatrix(Window window,Word2Vec vec) {
        INDArray[] data = new INDArray[window.getWords().size()];
        for(int i = 0; i < data.length; i++) {
            data[i] = vec.getWordVectorMatrix(window.getWord(i));
        }
		return Nd4j.hstack(data);
	}

}
