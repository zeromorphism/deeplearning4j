package org.deeplearning4j.gradientcheck;

import org.deeplearning4j.datasets.iterator.impl.IrisDataSetIterator;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.ComputationGraphConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.Updater;
import org.deeplearning4j.nn.conf.distribution.NormalDistribution;
import org.deeplearning4j.nn.conf.distribution.UniformDistribution;
import org.deeplearning4j.nn.conf.graph.ElementWiseVertex;
import org.deeplearning4j.nn.conf.graph.MergeVertex;
import org.deeplearning4j.nn.conf.graph.SubsetVertex;
import org.deeplearning4j.nn.conf.graph.rnn.DuplicateToTimeSeriesVertex;
import org.deeplearning4j.nn.conf.graph.rnn.LastTimeStepVertex;
import org.deeplearning4j.nn.conf.layers.*;
import org.deeplearning4j.nn.conf.preprocessor.CnnToFeedForwardPreProcessor;
import org.deeplearning4j.nn.conf.preprocessor.FeedForwardToRnnPreProcessor;
import org.deeplearning4j.nn.conf.preprocessor.RnnToFeedForwardPreProcessor;
import org.deeplearning4j.nn.graph.ComputationGraph;
import org.deeplearning4j.nn.weights.WeightInit;
import org.junit.Test;
import org.nd4j.linalg.api.buffer.DataBuffer;
import org.nd4j.linalg.api.buffer.util.DataTypeUtil;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.lossfunctions.LossFunctions;

import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class GradientCheckTestsComputationGraph {

    public static final boolean PRINT_RESULTS = true;
    private static final boolean RETURN_ON_FIRST_FAILURE = false;
    private static final double DEFAULT_EPS = 1e-6;
    private static final double DEFAULT_MAX_REL_ERROR = 1e-3;

    static {
        //Force Nd4j initialization, then set data type to double:
        Nd4j.zeros(1);
        DataTypeUtil.setDTypeForContext(DataBuffer.Type.DOUBLE);
    }

    @Test
    public void testBasicIris(){
        Nd4j.getRandom().setSeed(12345);
        ComputationGraphConfiguration conf = new NeuralNetConfiguration.Builder()
                .seed(12345)
                .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
                .weightInit(WeightInit.DISTRIBUTION).dist(new NormalDistribution(0, 1))
                .updater(Updater.NONE).learningRate(1.0)
                .graphBuilder()
                .addInputs("input")
                .addLayer("firstLayer", new DenseLayer.Builder().nIn(4).nOut(5).activation("tanh").build(), "input")
                .addLayer("outputLayer", new OutputLayer.Builder().lossFunction(LossFunctions.LossFunction.MCXENT)
                        .activation("softmax").nIn(5).nOut(3).build(), "firstLayer")
                .setOutputs("outputLayer")
                .pretrain(false).backprop(true)
                .build();

        ComputationGraph graph = new ComputationGraph(conf);
        graph.init();

        Nd4j.getRandom().setSeed(12345);
        int nParams = graph.numParams();
        INDArray newParams = Nd4j.rand(1,nParams);
        graph.setParams(newParams);

        DataSet ds = new IrisDataSetIterator(150,150).next();
        INDArray min = ds.getFeatureMatrix().min(0);
        INDArray max = ds.getFeatureMatrix().max(0);
        ds.getFeatureMatrix().subiRowVector(min).diviRowVector(max.sub(min));
        INDArray input = ds.getFeatureMatrix();
        INDArray labels = ds.getLabels();

        if( PRINT_RESULTS ){
            System.out.println("testBasicIris()" );
            for( int j=0; j<graph.getNumLayers(); j++ ) System.out.println("Layer " + j + " # params: " + graph.getLayer(j).numParams());
        }

        boolean gradOK = GradientCheckUtil.checkGradients(graph, DEFAULT_EPS, DEFAULT_MAX_REL_ERROR,
                PRINT_RESULTS, RETURN_ON_FIRST_FAILURE, new INDArray[]{input}, new INDArray[]{labels});

        String msg = "testBasicIris()";
        assertTrue(msg,gradOK);
    }

    @Test
    public void testBasicIrisWithMerging(){
        Nd4j.getRandom().setSeed(12345);
        ComputationGraphConfiguration conf = new NeuralNetConfiguration.Builder()
                .seed(12345)
                .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
                .weightInit(WeightInit.DISTRIBUTION).dist(new NormalDistribution(0, 1))
                .updater(Updater.NONE).learningRate(1.0)
                .graphBuilder()
                .addInputs("input")
                .addLayer("l1", new DenseLayer.Builder().nIn(4).nOut(5).activation("tanh").build(), "input")
                .addLayer("l2", new DenseLayer.Builder().nIn(4).nOut(5).activation("tanh").build(), "input")
                .addVertex("merge", new MergeVertex(), "l1", "l2")
                .addLayer("outputLayer", new OutputLayer.Builder().lossFunction(LossFunctions.LossFunction.MCXENT)
                        .activation("softmax").nIn(5+5).nOut(3).build(), "merge")
                .setOutputs("outputLayer")
                .pretrain(false).backprop(true)
                .build();

        ComputationGraph graph = new ComputationGraph(conf);
        graph.init();

        int numParams = (4*5+5) + (4*5+5) + (10*3+3);
        assertEquals(numParams, graph.numParams());

        Nd4j.getRandom().setSeed(12345);
        int nParams = graph.numParams();
        INDArray newParams = Nd4j.rand(1,nParams);
        graph.setParams(newParams);

        DataSet ds = new IrisDataSetIterator(150,150).next();
        INDArray min = ds.getFeatureMatrix().min(0);
        INDArray max = ds.getFeatureMatrix().max(0);
        ds.getFeatureMatrix().subiRowVector(min).diviRowVector(max.sub(min));
        INDArray input = ds.getFeatureMatrix();
        INDArray labels = ds.getLabels();

        if( PRINT_RESULTS ){
            System.out.println("testBasicIrisWithMerging()" );
            for( int j=0; j<graph.getNumLayers(); j++ ) System.out.println("Layer " + j + " # params: " + graph.getLayer(j).numParams());
        }

        boolean gradOK = GradientCheckUtil.checkGradients(graph, DEFAULT_EPS, DEFAULT_MAX_REL_ERROR,
                PRINT_RESULTS, RETURN_ON_FIRST_FAILURE, new INDArray[]{input}, new INDArray[]{labels});

        String msg = "testBasicIrisWithMerging()";
        assertTrue(msg,gradOK);
    }

    @Test
    public void testBasicIrisWithElementWiseNode(){

        ElementWiseVertex.Op[] ops = new ElementWiseVertex.Op[]{ElementWiseVertex.Op.Add, ElementWiseVertex.Op.Subtract};

        for( ElementWiseVertex.Op op : ops ) {

            Nd4j.getRandom().setSeed(12345);
            ComputationGraphConfiguration conf = new NeuralNetConfiguration.Builder()
                    .seed(12345)
                    .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
                    .weightInit(WeightInit.DISTRIBUTION).dist(new NormalDistribution(0, 1))
                    .updater(Updater.NONE).learningRate(1.0)
                    .graphBuilder()
                    .addInputs("input")
                    .addLayer("l1", new DenseLayer.Builder().nIn(4).nOut(5).activation("tanh").build(), "input")
                    .addLayer("l2", new DenseLayer.Builder().nIn(4).nOut(5).activation("sigmoid").build(), "input")
                    .addVertex("elementwise", new ElementWiseVertex(op), "l1", "l2")
                    .addLayer("outputLayer", new OutputLayer.Builder().lossFunction(LossFunctions.LossFunction.MCXENT)
                            .activation("softmax").nIn(5).nOut(3).build(), "elementwise")
                    .setOutputs("outputLayer")
                    .pretrain(false).backprop(true)
                    .build();

            ComputationGraph graph = new ComputationGraph(conf);
            graph.init();

            int numParams = (4 * 5 + 5) + (4 * 5 + 5) + (5 * 3 + 3);
            assertEquals(numParams, graph.numParams());

            Nd4j.getRandom().setSeed(12345);
            int nParams = graph.numParams();
            INDArray newParams = Nd4j.rand(1, nParams);
            graph.setParams(newParams);

            DataSet ds = new IrisDataSetIterator(150, 150).next();
            INDArray min = ds.getFeatureMatrix().min(0);
            INDArray max = ds.getFeatureMatrix().max(0);
            ds.getFeatureMatrix().subiRowVector(min).diviRowVector(max.sub(min));
            INDArray input = ds.getFeatureMatrix();
            INDArray labels = ds.getLabels();

            if (PRINT_RESULTS) {
                System.out.println("testBasicIrisWithElementWiseVertex(op=" + op + ")");
                for (int j = 0; j < graph.getNumLayers(); j++)
                    System.out.println("Layer " + j + " # params: " + graph.getLayer(j).numParams());
            }

            boolean gradOK = GradientCheckUtil.checkGradients(graph, DEFAULT_EPS, DEFAULT_MAX_REL_ERROR,
                    PRINT_RESULTS, RETURN_ON_FIRST_FAILURE, new INDArray[]{input}, new INDArray[]{labels});

            String msg = "testBasicIrisWithElementWiseVertex(op=" + op + ")";
            assertTrue(msg, gradOK);
        }
    }

    @Test
    public void testCnnDepthMerge(){

        Nd4j.getRandom().setSeed(12345);
        ComputationGraphConfiguration conf = new NeuralNetConfiguration.Builder()
                .seed(12345)
                .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
                .weightInit(WeightInit.DISTRIBUTION).dist(new NormalDistribution(0, 0.1))
                .updater(Updater.NONE).learningRate(1.0)
                .graphBuilder()
                .addInputs("input")
                .addLayer("l1", new ConvolutionLayer.Builder()
                        .kernelSize(2, 2).stride(1, 1).padding(0,0)
                        .nIn(2).nOut(2).activation("tanh").build(), "input")
                .addLayer("l2", new ConvolutionLayer.Builder()
                        .kernelSize(2, 2).stride(1, 1).padding(0,0)
                        .nIn(2).nOut(2).activation("tanh").build(), "input")
                .addVertex("merge", new MergeVertex(), "l1", "l2")
                .addLayer("outputLayer", new OutputLayer.Builder().lossFunction(LossFunctions.LossFunction.MCXENT)
                        .activation("softmax").nIn(5*5*(2+2)).nOut(3).build(), "merge")
                .setOutputs("outputLayer")
                .inputPreProcessor("outputLayer",new CnnToFeedForwardPreProcessor(5,5,4))
                .pretrain(false).backprop(true)
                .build();

        ComputationGraph graph = new ComputationGraph(conf);
        graph.init();

        Random r = new Random(12345);
        INDArray input = Nd4j.rand(new int[]{5,2,6,6}); //Order: examples, channels, height, width
        INDArray labels = Nd4j.zeros(5,3);
        for( int i=0; i<5; i++ ) labels.putScalar(new int[]{i,r.nextInt(3)},1.0);

        if (PRINT_RESULTS) {
            System.out.println("testCnnDepthMerge()");
            for (int j = 0; j < graph.getNumLayers(); j++)
                System.out.println("Layer " + j + " # params: " + graph.getLayer(j).numParams());
        }

        boolean gradOK = GradientCheckUtil.checkGradients(graph, DEFAULT_EPS, DEFAULT_MAX_REL_ERROR,
                PRINT_RESULTS, RETURN_ON_FIRST_FAILURE, new INDArray[]{input}, new INDArray[]{labels});

        String msg = "testCnnDepthMerge()";
        assertTrue(msg, gradOK);
    }

    @Test
    public void testLSTMWithMerging(){

        Nd4j.getRandom().setSeed(12345);
        ComputationGraphConfiguration conf = new NeuralNetConfiguration.Builder()
                .seed(12345)
                .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
                .weightInit(WeightInit.DISTRIBUTION).dist(new UniformDistribution(0.2, 0.6))
                .updater(Updater.NONE).learningRate(1.0)
                .graphBuilder()
                .addInputs("input")
                .setOutputs("out")
                .addLayer("lstm1", new GravesLSTM.Builder().nIn(3).nOut(4).activation("tanh").build(), "input")
                .addLayer("lstm2", new GravesLSTM.Builder().nIn(4).nOut(4).activation("tanh").build(), "lstm1")
                .addLayer("dense1", new DenseLayer.Builder().nIn(4).nOut(4).activation("sigmoid").build(), "lstm1")
                .addLayer("lstm3", new GravesLSTM.Builder().nIn(4).nOut(4).activation("tanh").build(), "dense1")
                .addVertex("merge", new MergeVertex(), "lstm2", "lstm3")
                .addLayer("out", new RnnOutputLayer.Builder().nIn(8).nOut(3).activation("softmax").lossFunction(LossFunctions.LossFunction.MCXENT).build(), "merge")
                .inputPreProcessor("dense1", new RnnToFeedForwardPreProcessor())
                .inputPreProcessor("lstm3", new FeedForwardToRnnPreProcessor())
                .pretrain(false).backprop(true).build();

        ComputationGraph graph = new ComputationGraph(conf);
        graph.init();

        Random r = new Random(12345);
        INDArray input = Nd4j.rand(new int[]{3,3,5});
        INDArray labels = Nd4j.zeros(3,3,5);
        for( int i=0; i<3; i++ ){
            for( int j=0; j<5; j++ ) {
                labels.putScalar(new int[]{i, r.nextInt(3), j}, 1.0);
            }
        }

        if (PRINT_RESULTS) {
            System.out.println("testLSTMWithMerging()");
            for (int j = 0; j < graph.getNumLayers(); j++)
                System.out.println("Layer " + j + " # params: " + graph.getLayer(j).numParams());
        }

        boolean gradOK = GradientCheckUtil.checkGradients(graph, DEFAULT_EPS, DEFAULT_MAX_REL_ERROR,
                PRINT_RESULTS, RETURN_ON_FIRST_FAILURE, new INDArray[]{input}, new INDArray[]{labels});

        String msg = "testLSTMWithMerging()";
        assertTrue(msg, gradOK);
    }

    @Test
    public void testLSTMWithSubset(){
        Nd4j.getRandom().setSeed(1234);
        ComputationGraphConfiguration conf = new NeuralNetConfiguration.Builder()
                .seed(1234)
                .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
                .weightInit(WeightInit.DISTRIBUTION).dist(new NormalDistribution(0, 1))
                .updater(Updater.NONE).learningRate(1.0)
                .graphBuilder()
                .addInputs("input")
                .setOutputs("out")
                .addLayer("lstm1", new GravesLSTM.Builder().nIn(3).nOut(8).activation("tanh").build(), "input")
                .addVertex("subset", new SubsetVertex(0, 3), "lstm1")
                .addLayer("out", new RnnOutputLayer.Builder().nIn(4).nOut(3).activation("softmax").lossFunction(LossFunctions.LossFunction.MCXENT).build(), "subset")
                .pretrain(false).backprop(true).build();

        ComputationGraph graph = new ComputationGraph(conf);
        graph.init();

        Random r = new Random(12345);
        INDArray input = Nd4j.rand(new int[]{3,3,5});
        INDArray labels = Nd4j.zeros(3,3,5);
        for( int i=0; i<3; i++ ){
            for( int j=0; j<5; j++ ) {
                labels.putScalar(new int[]{i, r.nextInt(3), j}, 1.0);
            }
        }

        if (PRINT_RESULTS) {
            System.out.println("testLSTMWithSubset()");
            for (int j = 0; j < graph.getNumLayers(); j++)
                System.out.println("Layer " + j + " # params: " + graph.getLayer(j).numParams());
        }

        boolean gradOK = GradientCheckUtil.checkGradients(graph, DEFAULT_EPS, DEFAULT_MAX_REL_ERROR,
                PRINT_RESULTS, RETURN_ON_FIRST_FAILURE, new INDArray[]{input}, new INDArray[]{labels});

        String msg = "testLSTMWithSubset()";
        assertTrue(msg, gradOK);
    }

    @Test
    public void testLSTMWithLastTimeStepVertex(){

        Nd4j.getRandom().setSeed(12345);
        ComputationGraphConfiguration conf = new NeuralNetConfiguration.Builder()
                .seed(12345)
                .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
                .weightInit(WeightInit.DISTRIBUTION).dist(new NormalDistribution(0, 1))
                .updater(Updater.NONE).learningRate(1.0)
                .graphBuilder()
                .addInputs("input")
                .setOutputs("out")
                .addLayer("lstm1", new GravesLSTM.Builder().nIn(3).nOut(4).activation("tanh").build(), "input")
                .addVertex("lastTS", new LastTimeStepVertex("input"), "lstm1")
                .addLayer("out", new OutputLayer.Builder().nIn(4).nOut(3).activation("softmax")
                        .lossFunction(LossFunctions.LossFunction.MCXENT).build(), "lastTS")
                .pretrain(false).backprop(true).build();

        ComputationGraph graph = new ComputationGraph(conf);
        graph.init();

        Random r = new Random(12345);
        INDArray input = Nd4j.rand(new int[]{3,3,5});
        INDArray labels = Nd4j.zeros(3,3);    //Here: labels are 2d (due to LastTimeStepVertex)
        for( int i=0; i<3; i++ ){
            labels.putScalar(new int[]{i, r.nextInt(3)}, 1.0);
        }

        if (PRINT_RESULTS) {
            System.out.println("testLSTMWithLastTimeStepVertex()");
            for (int j = 0; j < graph.getNumLayers(); j++)
                System.out.println("Layer " + j + " # params: " + graph.getLayer(j).numParams());
        }

        //First: test with no input mask array
        boolean gradOK = GradientCheckUtil.checkGradients(graph, DEFAULT_EPS, DEFAULT_MAX_REL_ERROR,
                PRINT_RESULTS, RETURN_ON_FIRST_FAILURE, new INDArray[]{input}, new INDArray[]{labels});

        String msg = "testLSTMWithLastTimeStepVertex()";
        assertTrue(msg, gradOK);

        //Second: test with input mask arrays.
        INDArray inMask = Nd4j.zeros(3,5);
        inMask.putRow(0,Nd4j.create(new double[]{1,1,1,0,0}));
        inMask.putRow(1,Nd4j.create(new double[]{1,1,1,1,0}));
        inMask.putRow(2,Nd4j.create(new double[]{1,1,1,1,1}));
        graph.setLayerMaskArrays(new INDArray[]{inMask}, null);
        gradOK = GradientCheckUtil.checkGradients(graph, DEFAULT_EPS, DEFAULT_MAX_REL_ERROR,
                PRINT_RESULTS, RETURN_ON_FIRST_FAILURE, new INDArray[]{input}, new INDArray[]{labels});

        assertTrue(msg, gradOK);
    }

    @Test
    public void testLSTMWithDuplicateToTimeSeries(){

        Nd4j.getRandom().setSeed(12345);
        ComputationGraphConfiguration conf = new NeuralNetConfiguration.Builder()
                .seed(12345)
                .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
                .weightInit(WeightInit.DISTRIBUTION).dist(new NormalDistribution(0, 1))
                .updater(Updater.NONE).learningRate(1.0)
                .graphBuilder()
                .addInputs("input1","input2")
                .setOutputs("out")
                .addLayer("lstm1", new GravesLSTM.Builder().nIn(3).nOut(4).activation("tanh").build(), "input1")
                .addLayer("lstm2", new GravesLSTM.Builder().nIn(4).nOut(5).activation("softsign").build(), "input2")
                .addVertex("lastTS", new LastTimeStepVertex("input2"), "lstm2")
                .addVertex("duplicate", new DuplicateToTimeSeriesVertex("input2"), "lastTS")
                .addLayer("out", new RnnOutputLayer.Builder().nIn(5+4).nOut(3).activation("softmax")
                        .lossFunction(LossFunctions.LossFunction.MCXENT).build(), "lstm1","duplicate")
                .pretrain(false).backprop(true).build();

        ComputationGraph graph = new ComputationGraph(conf);
        graph.init();

        Random r = new Random(12345);
        INDArray input1 = Nd4j.rand(new int[]{3,3,5});
        INDArray input2 = Nd4j.rand(new int[]{3,4,5});
        INDArray labels = Nd4j.zeros(3,3,5);
        for( int i=0; i<3; i++ ){
            for( int j=0; j<5; j++ ) {
                labels.putScalar(new int[]{i, r.nextInt(3), j}, 1.0);
            }
        }

        if (PRINT_RESULTS) {
            System.out.println("testLSTMWithDuplicateToTimeSeries()");
            for (int j = 0; j < graph.getNumLayers(); j++)
                System.out.println("Layer " + j + " # params: " + graph.getLayer(j).numParams());
        }

        boolean gradOK = GradientCheckUtil.checkGradients(graph, DEFAULT_EPS, DEFAULT_MAX_REL_ERROR,
                PRINT_RESULTS, RETURN_ON_FIRST_FAILURE, new INDArray[]{input1,input2}, new INDArray[]{labels});

        String msg = "testLSTMWithDuplicateToTimeSeries()";
        assertTrue(msg, gradOK);


    }

}
