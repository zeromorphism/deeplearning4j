package org.deeplearning4j.spark.impl.multilayer.scoring;

import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.broadcast.Broadcast;
import org.deeplearning4j.datasets.iterator.IteratorDataSetIterator;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.Tuple2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class ScoreFlatMapFunction implements FlatMapFunction<Iterator<DataSet>,Tuple2<Integer,Double>> {
    private static final Logger log = LoggerFactory.getLogger(ScoreFlatMapFunction.class);

    private String json;
    private Broadcast<INDArray> params;
    private int minibatchSize;

    public ScoreFlatMapFunction(String json, Broadcast<INDArray> params, int minibatchSize){
        this.json = json;
        this.params = params;
        this.minibatchSize = minibatchSize;
    }

    @Override
    public Iterable<Tuple2<Integer,Double>> call(Iterator<DataSet> dataSetIterator) throws Exception {
        if(!dataSetIterator.hasNext()) {
            return Collections.singletonList(new Tuple2<>(0,0.0));
        }

        DataSetIterator iter = new IteratorDataSetIterator(dataSetIterator, minibatchSize); //Does batching where appropriate

        MultiLayerNetwork network = new MultiLayerNetwork(MultiLayerConfiguration.fromJson(json));
        network.init();
        INDArray val = params.value();  //.value() object will be shared by all executors on each machine -> OK, as params are not modified by score function
        if(val.length() != network.numParams(false))
            throw new IllegalStateException("Network did not have same number of parameters as the broadcasted set parameters");
        network.setParameters(val);

        List<Tuple2<Integer,Double>> out = new ArrayList<>();
        while(iter.hasNext()){
            DataSet ds = iter.next();
            double score = network.score(ds,false);
            int numExamples = ds.getFeatureMatrix().size(0);
            out.add(new Tuple2<>(numExamples, score * numExamples));
        }

        return out;
    }
}
