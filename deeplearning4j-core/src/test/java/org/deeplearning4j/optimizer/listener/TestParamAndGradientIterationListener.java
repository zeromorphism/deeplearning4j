package org.deeplearning4j.optimizer.listener;


import org.deeplearning4j.datasets.iterator.impl.IrisDataSetIterator;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.optimize.api.IterationListener;
import org.deeplearning4j.optimize.listeners.ParamAndGradientIterationListener;
import org.junit.Test;
import org.nd4j.linalg.lossfunctions.LossFunctions;

import java.io.File;

public class TestParamAndGradientIterationListener {

    @Test
    public void test(){

        IrisDataSetIterator iter = new IrisDataSetIterator(30,150);

        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
                .learningRate(1e-5)
                .iterations(1)
                .list()
                .layer(0, new DenseLayer.Builder().nIn(4).nOut(20).build())
                .layer(1, new DenseLayer.Builder().nIn(20).nOut(30).build())
                .layer(2, new OutputLayer.Builder(LossFunctions.LossFunction.MCXENT).activation("softmax").nIn(30).nOut(3).build())
                .pretrain(false).backprop(true)
                .build();

        MultiLayerNetwork net = new MultiLayerNetwork(conf);
        net.init();

        IterationListener listener = ParamAndGradientIterationListener.builder()
                .outputToFile(true)
                .file(new File(System.getProperty("java.io.tmpdir") + "/paramAndGradTest.txt"))
                .outputToConsole(true).outputToLogger(false)
                .iterations(2)
                .printHeader(true).printMean(false).printMinMax(false).printMeanAbsValue(true)
                .delimiter("\t")
                .build();
        net.setListeners(listener);

        for( int i=0; i<2; i++ ){
            net.fit(iter);
        }


    }

}
