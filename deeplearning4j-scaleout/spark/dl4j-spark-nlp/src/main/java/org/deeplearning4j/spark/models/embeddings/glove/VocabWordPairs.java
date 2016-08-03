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

package org.deeplearning4j.spark.models.embeddings.glove;

import org.apache.spark.api.java.function.Function;
import org.apache.spark.broadcast.Broadcast;
import org.deeplearning4j.berkeley.Triple;
import org.deeplearning4j.models.word2vec.VocabWord;
import org.deeplearning4j.models.word2vec.wordstore.VocabCache;

/**
 * Convert string to vocab words
 *
 * @author Adam Gibson
 */
public class VocabWordPairs implements Function<Triple<String,String,Double>,Triple<VocabWord,VocabWord,Double>> {
    private Broadcast<VocabCache<VocabWord>> vocab;

    public VocabWordPairs(Broadcast<VocabCache<VocabWord>> vocab) {
        this.vocab = vocab;
    }

    @Override
    public Triple<VocabWord, VocabWord, Double> call(Triple<String, String, Double> v1) throws Exception {
        return new Triple<>((VocabWord) vocab.getValue().wordFor(v1.getFirst()),(VocabWord) vocab.getValue().wordFor(v1.getSecond()),v1.getThird());
    }
}
