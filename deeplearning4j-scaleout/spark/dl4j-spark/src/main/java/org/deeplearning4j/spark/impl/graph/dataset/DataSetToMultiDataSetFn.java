/*
 *
 *  * Copyright 2016 Skymind,Inc.
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

package org.deeplearning4j.spark.impl.graph.dataset;

import org.apache.spark.api.java.function.Function;
import org.deeplearning4j.nn.graph.util.ComputationGraphUtil;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.MultiDataSet;

/**Convert a {@code JavaRDD<DataSet>} to a {@code JavaRDD<MultiDataSet>}
 */
public class DataSetToMultiDataSetFn implements Function<DataSet,MultiDataSet> {
    @Override
    public MultiDataSet call(DataSet d) throws Exception {
        return ComputationGraphUtil.toMultiDataSet(d);
    }
}
