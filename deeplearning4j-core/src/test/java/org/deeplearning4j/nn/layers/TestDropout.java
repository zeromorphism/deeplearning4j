package org.deeplearning4j.nn.layers;

import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.Updater;
import org.deeplearning4j.nn.conf.distribution.UniformDistribution;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.junit.Test;
import org.nd4j.linalg.api.iter.NdIndexIterator;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.lossfunctions.LossFunctions;

import java.lang.reflect.Field;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class TestDropout {

    @Test
    public void testDropoutSimple() throws Exception {
        //Testing dropout with a single layer
        //Layer input: values should be set to either 0.0 or 2.0x original value

        int nIn = 8;
        int nOut = 4;

        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
                .updater(Updater.SGD).iterations(1)
                .regularization(true).dropOut(0.5)
                .list()
                .layer(0, new OutputLayer.Builder().lossFunction(LossFunctions.LossFunction.MSE)
                        .nIn(nIn).nOut(nOut).weightInit(WeightInit.XAVIER).build())
                .backprop(true).pretrain(false)
                .build();

        MultiLayerNetwork net = new MultiLayerNetwork(conf);
        net.init();

        int nTests = 15;

        Nd4j.getRandom().setSeed(12345);
        int noDropoutCount = 0;
        for( int i=0; i<nTests; i++ ){
            INDArray in = Nd4j.rand(1,nIn);
            INDArray out = Nd4j.rand(1,nOut);
            INDArray inCopy = in.dup();

            net.fit(new DataSet(in,out));

            INDArray l0Input = net.getLayer(0).input();
            //Dropout occurred. Expect inputs to be either scaled 2x original, or set to 0.0 (with dropout = 0.5)
            for( int j=0; j<inCopy.length(); j++ ){
                double origValue = inCopy.getDouble(j);
                double doValue = l0Input.getDouble(j);
                if(doValue > 0.0){
                    //Input was kept -> should be scaled by factor of (1.0/0.5 = 2)
                    assertEquals(origValue*2.0, doValue, 0.0001);
                }
            }

            //Do forward pass
            //(1) ensure dropout ISN'T being applied for forward pass at test time
            //(2) ensure dropout ISN'T being applied for test time scoring
            //If dropout is applied at test time: outputs + score will differ between passes
            INDArray in2 = Nd4j.rand(1,nIn);
            INDArray out2 = Nd4j.rand(1,nOut);
            INDArray outTest1 = net.output(in2, false);
            INDArray outTest2 = net.output(in2, false);
            INDArray outTest3 = net.output(in2, false);
            assertEquals(outTest1,outTest2);
            assertEquals(outTest1, outTest3);

            double score1 = net.score(new DataSet(in2,out2), false);
            double score2 = net.score(new DataSet(in2,out2), false);
            double score3 = net.score(new DataSet(in2,out2), false);
            assertEquals(score1,score2,0.0);
            assertEquals(score1,score3,0.0);
        }

        if(noDropoutCount >= nTests / 3){
            //at 0.5 dropout ratio and more than a few inputs, expect only a very small number of instances where
            //no dropout occurs, just due to random chance
            fail("Too many instances of dropout not being applied");
        }
    }


    @Test
    public void testDropoutMultiLayer() throws Exception {
        //Testing dropout with multiple layers
        //Layer input: values should be set to either 0.0 or 2.0x original value
        //However: we don't have access to 'original' activations easily
        //Instead: use sigmoid + weight initialization that saturates

        int nIn = 8;
        int layerSize = 10;
        int nOut = 4;

        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
                .updater(Updater.SGD).iterations(1)
                .regularization(true).dropOut(0.5)
                .learningRate(1e-9)
                .weightInit(WeightInit.DISTRIBUTION).dist(new UniformDistribution(10,11))   //Weight init to cause sigmoid saturation
                .list()
                .layer(0, new DenseLayer.Builder().activation("sigmoid").nIn(nIn).nOut(layerSize).build())
                .layer(1, new DenseLayer.Builder().activation("sigmoid").nIn(layerSize).nOut(layerSize).build())
                .layer(2, new OutputLayer.Builder().lossFunction(LossFunctions.LossFunction.MSE)
                        .nIn(layerSize).nOut(nOut).weightInit(WeightInit.XAVIER).build())
                .backprop(true).pretrain(false)
                .build();

        MultiLayerNetwork net = new MultiLayerNetwork(conf);
        net.init();

        Field dropoutMaskField = BaseLayer.class.getDeclaredField("dropoutMask");
        dropoutMaskField.setAccessible(true);

        int nTests = 15;

        Nd4j.getRandom().setSeed(12345);
        int noDropoutCount = 0;
        for( int i=0; i<nTests; i++ ){
            INDArray in = Nd4j.rand(1,nIn).dup('c');
            INDArray out = Nd4j.rand(1,nOut).dup('c');
            INDArray inCopy = in.dup('c');

            net.fit(new DataSet(in,out));

            INDArray l0Input = net.getLayer(0).input().dup('c');
            //Dropout occurred. Expect inputs to be either scaled 2x original, or set to 0.0 (with dropout = 0.5)
            NdIndexIterator iter = new NdIndexIterator(inCopy.shape());
            while(iter.hasNext()){
                int[] idx = iter.next();
                double origValue = inCopy.getDouble(idx);
                double doValue = l0Input.getDouble(idx);
                if(doValue > 0.0){
                    //Input was kept -> should be scaled by factor of (1.0/0.5 = 2)
                    assertEquals(origValue*2.0, doValue, 0.0001);
                }
            }

            //Check other layers. Don't know pre-dropout values in general, but using saturated sigmoids -> inputs should
            //all be ~1.0 before dropout -> either 0 or ~2.0 after dropout
            for( int j=1; j<3; j++ ){

                INDArray ljInput = net.getLayer(j).input();
                for( int k=0; k<ljInput.length(); k++ ){
                    double doValue = ljInput.getDouble(j);
                    if(doValue > 0.0){
                        //Input was kept -> should be scaled by factor of (1.0/0.5 = 2)
                        assertEquals(2.0, doValue, 0.1);    //Sigmoid is saturated -> inputs should be ~1.0 -> 2.0 after dropout
                    }
                }
            }

            //Do forward pass
            //(1) ensure dropout ISN'T being applied for forward pass at test time
            //(2) ensure dropout ISN'T being applied for test time scoring
            //If dropout is applied at test time: outputs + score will differ between passes
            INDArray in2 = Nd4j.rand(1,nIn);
            INDArray out2 = Nd4j.rand(1,nOut);
            INDArray outTest1 = net.output(in2, false);
            INDArray outTest2 = net.output(in2, false);
            INDArray outTest3 = net.output(in2, false);
            assertEquals(outTest1,outTest2);
            assertEquals(outTest1, outTest3);

            double score1 = net.score(new DataSet(in2,out2), false);
            double score2 = net.score(new DataSet(in2,out2), false);
            double score3 = net.score(new DataSet(in2,out2), false);
            assertEquals(score1,score2,0.0);
            assertEquals(score1,score3,0.0);
        }

        if(noDropoutCount >= nTests / 3){
            //at 0.5 dropout ratio and more than a few inputs, expect only a very small number of instances where
            //no dropout occurs, just due to random chance
            fail("Too many instances of dropout not being applied");
        }
    }



}
