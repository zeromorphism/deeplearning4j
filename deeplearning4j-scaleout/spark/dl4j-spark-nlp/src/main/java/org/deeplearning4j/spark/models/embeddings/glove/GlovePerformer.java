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
import org.deeplearning4j.berkeley.Triple;
import org.deeplearning4j.models.glove.GloveWeightLookupTable;
import org.deeplearning4j.models.word2vec.VocabWord;


/**
 * Base line glove performer
 *
 * @author Adam Gibson
 */
public class GlovePerformer implements Function<Triple<VocabWord,VocabWord,Double>,GloveChange> {


    public final static String NAME_SPACE = "org.deeplearning4j.scaleout.perform.models.glove";
    public final static String VECTOR_LENGTH = NAME_SPACE + ".length";
    public final static String ALPHA = NAME_SPACE + ".alpha";
    public final static String X_MAX = NAME_SPACE + ".xmax";
    public final static String MAX_COUNT = NAME_SPACE + ".maxcount";
    private GloveWeightLookupTable table;

    public GlovePerformer(GloveWeightLookupTable table) {
        this.table = table;
    }

    @Override
    public GloveChange call(Triple<VocabWord, VocabWord,Double> pair) throws Exception {
        return null;
    }
}
