package org.deeplearning4j.gradientcheck;

import org.deeplearning4j.datasets.iterator.impl.IrisDataSetIterator;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.Updater;
import org.deeplearning4j.nn.conf.distribution.NormalDistribution;
import org.deeplearning4j.nn.conf.layers.ConvolutionLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.conf.layers.SubsamplingLayer;
import org.deeplearning4j.nn.conf.layers.setup.ConvolutionLayerSetup;
import org.deeplearning4j.nn.conf.preprocessor.CnnToFeedForwardPreProcessor;
import org.deeplearning4j.nn.conf.preprocessor.FeedForwardToCnnPreProcessor;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.junit.Test;
import org.nd4j.linalg.api.buffer.DataBuffer;
import org.nd4j.linalg.api.buffer.util.DataTypeUtil;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.factory.NDArrayFactory;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.indexing.INDArrayIndex;
import org.nd4j.linalg.indexing.NDArrayIndex;
import org.nd4j.linalg.lossfunctions.LossFunctions;

import java.util.Random;

import static org.junit.Assert.*;

/**
 * Created by nyghtowl on 9/1/15.
 */
public class CNNGradientCheckTest {
    private static final boolean PRINT_RESULTS = true;
    private static final boolean RETURN_ON_FIRST_FAILURE = false;
    private static final double DEFAULT_EPS = 1e-6;
    private static final double DEFAULT_MAX_REL_ERROR = 1e-3;

    static {
        //Force Nd4j initialization, then set data type to double:
        Nd4j.zeros(1);
        DataTypeUtil.setDTypeForContext(DataBuffer.Type.DOUBLE);
    }

    @Test
    public void testGradientCNNMLN(){
        //Parameterized test, testing combinations of:
        // (a) activation function
        // (b) Whether to test at random initialization, or after some learning (i.e., 'characteristic mode of operation')
        // (c) Loss function (with specified output activations)
        String[] activFns = {"sigmoid","tanh"};
        boolean[] characteristic = {false,true};	//If true: run some backprop steps first

        LossFunctions.LossFunction[] lossFunctions = {LossFunctions.LossFunction.NEGATIVELOGLIKELIHOOD, LossFunctions.LossFunction.MSE};
        String[] outputActivations = {"softmax", "tanh"};	//i.e., lossFunctions[i] used with outputActivations[i] here

        DataSet ds = new IrisDataSetIterator(150,150).next();
        ds.normalizeZeroMeanZeroUnitVariance();
        INDArray input = ds.getFeatureMatrix();
        INDArray labels = ds.getLabels();

        for( String afn : activFns) {
            for(boolean doLearningFirst : characteristic) {
                for(int i = 0; i < lossFunctions.length; i++) {
                    LossFunctions.LossFunction lf = lossFunctions[i];
                    String outputActivation = outputActivations[i];

                    MultiLayerConfiguration.Builder builder = new NeuralNetConfiguration.Builder()
                            .regularization(false)
                            .optimizationAlgo(OptimizationAlgorithm.CONJUGATE_GRADIENT)
                            .learningRate(1e-1)
                            .seed(12345L)
                            .list()
                            .layer(0, new ConvolutionLayer.Builder(new int[]{1, 1})
                                    .nOut(6)
                                    .weightInit(WeightInit.XAVIER)
                                    .activation(afn)
                                    .updater(Updater.NONE)
                                    .build())
                            .layer(1, new OutputLayer.Builder(lf)
                                    .activation(outputActivation)
                                    .nOut(3)
                                    .weightInit(WeightInit.XAVIER)
                                    .updater(Updater.NONE)
                                    .build())
                            .cnnInputSize(2,2,1)
                            .pretrain(false).backprop(true);

                    MultiLayerConfiguration conf = builder.build();

                    MultiLayerNetwork mln = new MultiLayerNetwork(conf);
                    mln.init();
                    String name = new Object(){}.getClass().getEnclosingMethod().getName();

                    if(doLearningFirst) {
                        //Run a number of iterations of learning
                        mln.setInput(ds.getFeatures());
                        mln.setLabels(ds.getLabels());
                        mln.computeGradientAndScore();
                        double scoreBefore = mln.score();
                        for( int j = 0; j < 10; j++)
                            mln.fit(ds);
                        mln.computeGradientAndScore();
                        double scoreAfter = mln.score();
                        //Can't test in 'characteristic mode of operation' if not learning
                        String msg = name+" - score did not (sufficiently) decrease during learning - activationFn="
                                + afn +", lossFn="+lf+", outputActivation="+outputActivation+", doLearningFirst= " + doLearningFirst
                                +" (before="+scoreBefore +", scoreAfter="+scoreAfter+")";
                        assertTrue(msg,scoreAfter < 0.8 *scoreBefore);
                    }

                    if( PRINT_RESULTS ){
                        System.out.println(name+" - activationFn="+afn+", lossFn="+lf+", outputActivation="+outputActivation
                                +", doLearningFirst="+doLearningFirst );
                        for( int j = 0; j<mln.getnLayers(); j++)
                            System.out.println("Layer " + j + " # params: " + mln.getLayer(j).numParams());
                    }

                    boolean gradOK = GradientCheckUtil.checkGradients(mln, DEFAULT_EPS, DEFAULT_MAX_REL_ERROR,
                            PRINT_RESULTS, RETURN_ON_FIRST_FAILURE, input, labels, true);

                    assertTrue(gradOK);
                }
            }
        }
    }



    @Test
    public void testGradientCNNL1L2MLN(){
        //Parameterized test, testing combinations of:
        // (a) activation function
        // (b) Whether to test at random initialization, or after some learning (i.e., 'characteristic mode of operation')
        // (c) Loss function (with specified output activations)
        String[] activFns = {"sigmoid","tanh"};
        boolean[] characteristic = {false,true};	//If true: run some backprop steps first

        LossFunctions.LossFunction[] lossFunctions = {LossFunctions.LossFunction.NEGATIVELOGLIKELIHOOD, LossFunctions.LossFunction.MSE};
        String[] outputActivations = {"softmax", "tanh"};	//i.e., lossFunctions[i] used with outputActivations[i] here

        DataSet ds = new IrisDataSetIterator(150,150).next();
        ds.normalizeZeroMeanZeroUnitVariance();
        INDArray input = ds.getFeatureMatrix();
        INDArray labels = ds.getLabels();

        double[] l2vals = {0.4, 0.0, 0.4};
        double[] l1vals = {0.0, 0.0, 0.5};	//i.e., use l2vals[i] with l1vals[i]

        for( String afn : activFns ){
            for( boolean doLearningFirst : characteristic ){
                for( int i=0; i < lossFunctions.length; i++ ) {
                    for (int k = 0; k < l2vals.length; k++) {
                        LossFunctions.LossFunction lf = lossFunctions[i];
                        String outputActivation = outputActivations[i];
                        double l2 = l2vals[k];
                        double l1 = l1vals[k];

                        MultiLayerConfiguration.Builder builder = new NeuralNetConfiguration.Builder()
                                .regularization(true)
                                .l2(l2).l1(l1)
                                .optimizationAlgo(OptimizationAlgorithm.CONJUGATE_GRADIENT)
                                .seed(12345L)
                                .list()
                                .layer(0, new ConvolutionLayer.Builder(new int[]{1, 1})
                                        .nIn(1).nOut(6)
                                        .weightInit(WeightInit.XAVIER)
                                        .activation(afn)
                                        .updater(Updater.NONE)
                                        .build())
                                .layer(1, new OutputLayer.Builder(lf)
                                        .activation(outputActivation)
                                        .nIn(6).nOut(3)
                                        .weightInit(WeightInit.XAVIER)
                                        .updater(Updater.NONE)
                                        .build())
                                .pretrain(false).backprop(true)
                                .cnnInputSize(2,2,1);   //Equivalent to: new ConvolutionLayerSetup(builder,2,2,1);

                        MultiLayerNetwork mln = new MultiLayerNetwork(builder.build());
                        mln.init();
                        String testName = new Object() {
                        }.getClass().getEnclosingMethod().getName();

                        if (doLearningFirst) {
                            //Run a number of iterations of learning
                            mln.setInput(ds.getFeatures());
                            mln.setLabels(ds.getLabels());
                            mln.computeGradientAndScore();
                            double scoreBefore = mln.score();
                            for (int j = 0; j < 10; j++) mln.fit(ds);
                            mln.computeGradientAndScore();
                            double scoreAfter = mln.score();
                            //Can't test in 'characteristic mode of operation' if not learning
                            String msg = testName + "- score did not (sufficiently) decrease during learning - activationFn="
                                    + afn + ", lossFn=" + lf + ", outputActivation=" + outputActivation + ", doLearningFirst=" + doLearningFirst
                                    + " (before=" + scoreBefore + ", scoreAfter=" + scoreAfter + ")";
                            assertTrue(msg, scoreAfter < 0.8 * scoreBefore);
                        }

                        if (PRINT_RESULTS) {
                            System.out.println(testName + "- activationFn=" + afn + ", lossFn=" + lf + ", outputActivation=" + outputActivation
                                    + ", doLearningFirst=" + doLearningFirst);
                            for (int j = 0; j < mln.getnLayers(); j++)
                                System.out.println("Layer " + j + " # params: " + mln.getLayer(j).numParams());
                        }

                        boolean gradOK = GradientCheckUtil.checkGradients(mln, DEFAULT_EPS, DEFAULT_MAX_REL_ERROR,
                                PRINT_RESULTS, RETURN_ON_FIRST_FAILURE, input, labels, true);

                        assertTrue(gradOK);
                    }
                }
            }
        }
    }


    @Test
    public void testCnnWithSubsampling(){
        int nOut = 4;

        int[] minibatchSizes = {1,3};
        int width = 5;
        int height = 5;
        int inputDepth = 1;

        int[] kernel = {2,2};
        int[] stride = {1,1};
        int[] padding = {0,0};

        String[] activations = {"sigmoid","tanh"};
        SubsamplingLayer.PoolingType[] poolingTypes = new SubsamplingLayer.PoolingType[]{SubsamplingLayer.PoolingType.MAX, SubsamplingLayer.PoolingType.AVG};

        for(String afn : activations) {
            for (SubsamplingLayer.PoolingType poolingType : poolingTypes) {
                for (int minibatchSize : minibatchSizes) {
                    INDArray input = Nd4j.rand(minibatchSize, width * height * inputDepth);
                    INDArray labels = Nd4j.zeros(minibatchSize, nOut);
                    for (int i = 0; i < minibatchSize; i++) {
                        labels.putScalar(new int[]{i, i % nOut}, 1.0);
                    }

                    MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                            .regularization(false)
                            .learningRate(1.0)
                            .updater(Updater.SGD)
                            .list()
                            .layer(0, new ConvolutionLayer.Builder(kernel, stride, padding)
                                    .nIn(inputDepth).nOut(3)
                                    .build())//output: (5-2+0)/1+1 = 4
                            .layer(1, new SubsamplingLayer.Builder(poolingType)
                                    .kernelSize(kernel)
                                    .stride(stride)
                                    .padding(padding)
                                    .build())   //output: (4-2+0)/1+1 =3 -> 3x3x3
                            .layer(2, new OutputLayer.Builder(LossFunctions.LossFunction.MCXENT).activation("softmax")
                                    .nIn(3 * 3 * 3)
                                    .nOut(4)
                                    .build())
                            .cnnInputSize(height, width, inputDepth)
                            .build();

                    MultiLayerNetwork net = new MultiLayerNetwork(conf);
                    net.init();

                    String msg = "PoolingType=" + poolingType + ", minibatch=" + minibatchSize + ", activationFn=" + afn;
                    System.out.println(msg);

                    boolean gradOK = GradientCheckUtil.checkGradients(net, DEFAULT_EPS, DEFAULT_MAX_REL_ERROR,
                            PRINT_RESULTS, RETURN_ON_FIRST_FAILURE, input, labels, true);

                    assertTrue(msg, gradOK);
                }
            }
        }
    }

    @Test
    public void testCnnWithSubsamplingV2(){
        int nOut = 4;

        int[] minibatchSizes = {1,3};
        int width = 5;
        int height = 5;
        int inputDepth = 1;

        int[] kernel = {2,2};
        int[] stride = {1,1};
        int[] padding = {0,0};

        String[] activations = {"sigmoid","tanh"};
        SubsamplingLayer.PoolingType[] poolingTypes = new SubsamplingLayer.PoolingType[]{SubsamplingLayer.PoolingType.MAX, SubsamplingLayer.PoolingType.AVG};

        for(String afn : activations) {
            for (SubsamplingLayer.PoolingType poolingType : poolingTypes) {
                for (int minibatchSize : minibatchSizes) {
                    INDArray input = Nd4j.rand(minibatchSize, width * height * inputDepth);
                    INDArray labels = Nd4j.zeros(minibatchSize, nOut);
                    for (int i = 0; i < minibatchSize; i++) {
                        labels.putScalar(new int[]{i, i % nOut}, 1.0);
                    }

                    MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                            .regularization(false)
                            .learningRate(1.0)
                            .updater(Updater.SGD)
                            .list()
                            .layer(0, new ConvolutionLayer.Builder(kernel, stride, padding)
                                    .nIn(inputDepth).nOut(3)
                                    .build())//output: (5-2+0)/1+1 = 4
                            .layer(1, new SubsamplingLayer.Builder(poolingType)
                                    .kernelSize(kernel)
                                    .stride(stride)
                                    .padding(padding)
                                    .build())   //output: (4-2+0)/1+1 =3 -> 3x3x3
                            .layer(2, new ConvolutionLayer.Builder(kernel, stride, padding)
                                    .nIn(3).nOut(2)
                                    .build())
                            .layer(3, new OutputLayer.Builder(LossFunctions.LossFunction.MCXENT).activation("softmax")
                                    .nIn(2 * 3 * 3)
                                    .nOut(4)
                                    .build())
                            .cnnInputSize(height, width, inputDepth)
                            .build();

                    MultiLayerNetwork net = new MultiLayerNetwork(conf);
                    net.init();

                    String msg = "PoolingType=" + poolingType + ", minibatch=" + minibatchSize + ", activationFn=" + afn;
                    System.out.println(msg);

                    boolean gradOK = GradientCheckUtil.checkGradients(net, DEFAULT_EPS, DEFAULT_MAX_REL_ERROR,
                            PRINT_RESULTS, RETURN_ON_FIRST_FAILURE, input, labels, true);

                    assertTrue(msg, gradOK);
                }
            }
        }
    }

    @Test
    public void testCnnMultiLayer(){
        int nOut = 2;

        int[] minibatchSizes = {1,2,5};
        int width = 5;
        int height = 5;
        int[] inputDepths = {1,2,4};

        String[] activations = {"sigmoid","tanh"};
        SubsamplingLayer.PoolingType[] poolingTypes = new SubsamplingLayer.PoolingType[]{SubsamplingLayer.PoolingType.MAX, SubsamplingLayer.PoolingType.AVG};

        Nd4j.getRandom().setSeed(12345);

        for(int inputDepth : inputDepths) {
            for (String afn : activations) {
                for (SubsamplingLayer.PoolingType poolingType : poolingTypes) {
                    for (int minibatchSize : minibatchSizes) {
                        INDArray input = Nd4j.rand(minibatchSize, width * height * inputDepth);
                        INDArray labels = Nd4j.zeros(minibatchSize, nOut);
                        for (int i = 0; i < minibatchSize; i++) {
                            labels.putScalar(new int[]{i, i % nOut}, 1.0);
                        }

                        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                                .seed(12345)
                                .regularization(false)
                                .learningRate(1.0)
                                .updater(Updater.SGD)
                                .activation(afn)
                                .list()
                                .layer(0, new ConvolutionLayer.Builder()
                                        .kernelSize(2, 2)
                                        .stride(1, 1)
                                        .padding(0, 0)
                                        .nIn(inputDepth).nOut(2)
                                        .build())//output: (5-2+0)/1+1 = 4
                                .layer(1, new ConvolutionLayer.Builder()
                                        .nIn(2).nOut(2)
                                        .kernelSize(2, 2)
                                        .stride(1, 1)
                                        .padding(0, 0)
                                        .build())   //(4-2+0)/1+1 = 3
                                .layer(2, new ConvolutionLayer.Builder()
                                        .nIn(2).nOut(2)
                                        .kernelSize(2, 2)
                                        .stride(1, 1)
                                        .padding(0, 0)
                                        .build())   //(3-2+0)/1+1 = 2
                                .layer(3, new OutputLayer.Builder(LossFunctions.LossFunction.MCXENT).activation("softmax")
                                        .nIn(2 * 2 * 2)
                                        .nOut(nOut)
                                        .build())
                                .cnnInputSize(height, width, inputDepth)
                                .build();

                        MultiLayerNetwork net = new MultiLayerNetwork(conf);
                        net.init();

                        for (int i = 0; i < 4; i++) {
                            System.out.println("nParams, layer " + i + ": " + net.getLayer(i).numParams());
                        }

                        String msg = "PoolingType=" + poolingType + ", minibatch=" + minibatchSize + ", activationFn=" + afn;
                        System.out.println(msg);

                        boolean gradOK = GradientCheckUtil.checkGradients(net, DEFAULT_EPS, DEFAULT_MAX_REL_ERROR,
                                PRINT_RESULTS, RETURN_ON_FIRST_FAILURE, input, labels, true);

                        assertTrue(msg, gradOK);
                    }
                }
            }
        }
    }
}
