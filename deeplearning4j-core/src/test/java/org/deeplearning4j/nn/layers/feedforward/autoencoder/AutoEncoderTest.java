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

package org.deeplearning4j.nn.layers.feedforward.autoencoder;

import org.deeplearning4j.datasets.fetchers.MnistDataFetcher;
import org.deeplearning4j.nn.api.Layer;
import org.deeplearning4j.nn.api.LayerFactory;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.gradient.DefaultGradient;
import org.deeplearning4j.nn.gradient.Gradient;
import org.deeplearning4j.nn.layers.factory.LayerFactories;
import org.deeplearning4j.nn.params.DefaultParamInitializer;
import org.deeplearning4j.optimize.api.IterationListener;
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.junit.Test;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.lossfunctions.LossFunctions;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

public class AutoEncoderTest {

    @Test
    public void testAutoEncoderBiasInit() {
        org.deeplearning4j.nn.conf.layers.AutoEncoder build = new org.deeplearning4j.nn.conf.layers.AutoEncoder.Builder()
                .nIn(1)
                .nOut(3)
                .biasInit(1)
                .build();

        NeuralNetConfiguration conf = new NeuralNetConfiguration.Builder()
                .layer(build)
                .build();

        int numParams = LayerFactories.getFactory(conf).initializer().numParams(conf,true);
        INDArray params = Nd4j.create(1, numParams);
        Layer layer =  LayerFactories.getFactory(conf).create(conf, null, 0, params, true);

        assertEquals(1, layer.getParam("b").size(0));
    }


    @Test
    public void testAutoEncoder() throws Exception {

        MnistDataFetcher fetcher = new MnistDataFetcher(true);
        NeuralNetConfiguration conf = new NeuralNetConfiguration.Builder().momentum(0.9f)
                .optimizationAlgo(OptimizationAlgorithm.LINE_GRADIENT_DESCENT)
                .iterations(1)
                .learningRate(1e-1f)
                .layer(new org.deeplearning4j.nn.conf.layers.AutoEncoder.Builder()
                        .nIn(784).nOut(600)
                        .corruptionLevel(0.6)
                        .lossFunction(LossFunctions.LossFunction.RECONSTRUCTION_CROSSENTROPY).build())
                .build();


        fetcher.fetch(100);
        DataSet d2 = fetcher.next();

        INDArray input = d2.getFeatureMatrix();
        int numParams = LayerFactories.getFactory(conf).initializer().numParams(conf,true);
        INDArray params = Nd4j.create(1, numParams);
        AutoEncoder da = LayerFactories.getFactory(conf.getLayer()).create(conf, Arrays.<IterationListener>asList(new ScoreIterationListener(1)),0, params, true);
        assertEquals(da.params(),da.params());
        assertEquals(471784,da.params().length());
        da.setParams(da.params());
        da.fit(input);
    }





    @Test
    public void testBackProp() throws Exception {
        MnistDataFetcher fetcher = new MnistDataFetcher(true);
        LayerFactory layerFactory = LayerFactories.getFactory(new org.deeplearning4j.nn.conf.layers.AutoEncoder());
        NeuralNetConfiguration conf = new NeuralNetConfiguration.Builder().momentum(0.9f)
                .optimizationAlgo(OptimizationAlgorithm.LINE_GRADIENT_DESCENT)
                .iterations(100)
                .learningRate(1e-1f)
                .layer(new org.deeplearning4j.nn.conf.layers.AutoEncoder.Builder()
                        .nIn(784).nOut(600)
                        .corruptionLevel(0.6)
                        .lossFunction(LossFunctions.LossFunction.RECONSTRUCTION_CROSSENTROPY).build())
                .build();

        fetcher.fetch(100);
        DataSet d2 = fetcher.next();

        INDArray input = d2.getFeatureMatrix();
        int numParams = LayerFactories.getFactory(conf).initializer().numParams(conf,true);
        INDArray params = Nd4j.create(1, numParams);
        AutoEncoder da = layerFactory.create(conf,null,0,params, true);
        Gradient g = new DefaultGradient();
        g.gradientForVariable().put(DefaultParamInitializer.WEIGHT_KEY, da.decode(da.activate(input)).sub(input));

    }



}
