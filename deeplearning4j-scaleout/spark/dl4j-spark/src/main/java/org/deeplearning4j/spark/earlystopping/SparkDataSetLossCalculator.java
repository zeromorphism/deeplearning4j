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

package org.deeplearning4j.spark.earlystopping;

import org.apache.spark.SparkContext;
import org.apache.spark.api.java.JavaRDD;
import org.deeplearning4j.earlystopping.scorecalc.ScoreCalculator;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.spark.impl.multilayer.SparkDl4jMultiLayer;
import org.nd4j.linalg.dataset.DataSet;

/** Score calculator to calculate the total loss for the {@link MultiLayerNetwork} on that data set (data set
 * as a {@link JavaRDD<DataSet>}), using Spark.
 * Typically used to calculate the loss on a test set.
 */
public class SparkDataSetLossCalculator implements ScoreCalculator<MultiLayerNetwork> {


    private JavaRDD<DataSet> data;
    private boolean average;
    private SparkContext sc;

    /**Calculate the score (loss function value) on a given data set (usually a test set)
     *
     * @param data Data set to calculate the score for
     * @param average Whether to return the average (sum of loss / N) or just (sum of loss)
     */
    public SparkDataSetLossCalculator(JavaRDD<DataSet> data, boolean average, SparkContext sc) {
        this.data = data;
        this.average = average;
        this.sc = sc;
    }

    @Override
    public double calculateScore(MultiLayerNetwork network) {
        SparkDl4jMultiLayer net = new SparkDl4jMultiLayer(sc,network,null);
        return net.calculateScore(data,average);
    }

}
