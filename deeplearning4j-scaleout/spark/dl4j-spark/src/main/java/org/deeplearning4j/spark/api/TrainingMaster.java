package org.deeplearning4j.spark.api;

import org.apache.spark.annotation.Experimental;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.input.PortableDataStream;
import org.deeplearning4j.optimize.api.IterationListener;
import org.deeplearning4j.spark.api.stats.SparkTrainingStats;
import org.deeplearning4j.spark.impl.graph.SparkComputationGraph;
import org.deeplearning4j.spark.impl.multilayer.SparkDl4jMultiLayer;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.MultiDataSet;

import java.io.OutputStream;
import java.util.Collection;

/**
 * A TrainingMaster controls how distributed training is executed in practice<br>
 * In principle, a large number of different approches can be used in distributed training (synchronous vs. asynchronous,
 * parameter vs. gradient averaging, etc). Each of these different approaches would be implemented as a TrainingMaster;
 * this allows {@link SparkDl4jMultiLayer} and {@link SparkComputationGraph} to be used with different training methods.
 *
 * @author Alex Black
 */
public interface TrainingMaster<R extends TrainingResult, W extends TrainingWorker<R>> {

    /**
     * Get the worker instance for this training master
     *
     * @param network Current SparkDl4jMultiLayer
     * @return Worker instance
     */
    W getWorkerInstance(SparkDl4jMultiLayer network);

    /**
     * Get the worker instance for this training master
     *
     * @param graph Current SparkComputationGraph
     * @return Worker instance
     */
    W getWorkerInstance(SparkComputationGraph graph);

    /**
     * Train the SparkDl4jMultiLayer with the specified data set
     *
     * @param network      Current network state
     * @param trainingData Data to train on
     */
    void executeTraining(SparkDl4jMultiLayer network, JavaRDD<DataSet> trainingData);


    /**
     * Train the SparkDl4jMultiLayer with the specified <i>serialized DataSet objects</i>. The assumption
     * here is that the PortableDataStreams are for DataSet objects, one per file.
     *
     * @param network      Current network state
     * @param trainingData Data to train on
     */
    void executeTraining(SparkDl4jMultiLayer network, JavaPairRDD<String, PortableDataStream> trainingData);


    /**
     * <b>EXPERIMENTAL method, may be removed in a future release.</b><br>
     * Fit the network using a list of paths for serialized DataSet objects.
     *
     * @param network           Current network state
     * @param trainingDataPaths Data to train on
     */
    @Experimental
    void executeTrainingPaths(SparkDl4jMultiLayer network, JavaRDD<String> trainingDataPaths);

    /**
     * Train the SparkComputationGraph with the specified data set
     *
     * @param graph        Current network state
     * @param trainingData Data to train on
     */
    void executeTraining(SparkComputationGraph graph, JavaRDD<DataSet> trainingData);

    /**
     * Train the SparkComputationGraph with the specified <i>serialized DataSet objects</i>. The assumption
     * here is that the PortableDataStreams are for DataSet objects, one per file, and that these have been
     * serialized using {@link DataSet#save(OutputStream)}
     *
     * @param network      Current network state
     * @param trainingData Data to train on
     */
    void executeTraining(SparkComputationGraph network, JavaPairRDD<String, PortableDataStream> trainingData);

    /**
     * <b>EXPERIMENTAL method, may be removed in a future release.</b><br>
     * Fit the network using a list of paths for serialized DataSet objects.
     *
     * @param network           Current network state
     * @param trainingDataPaths Data to train on
     */
    @Experimental
    void executeTrainingPaths(SparkComputationGraph network, JavaRDD<String> trainingDataPaths);

    /**
     * <b>EXPERIMENTAL method, may be removed in a future release.</b><br>
     * Fit the network using a list of paths for serialized MultiDataSet objects.
     *
     * @param network           Current network state
     * @param trainingMultiDataSetPaths Data to train on
     */
    @Experimental
    void executeTrainingPathsMDS(SparkComputationGraph network, JavaRDD<String> trainingMultiDataSetPaths);

    /**
     * Train the SparkComputationGraph with the specified data set
     *
     * @param graph        Current network state
     * @param trainingData Data to train on
     */
    void executeTrainingMDS(SparkComputationGraph graph, JavaRDD<MultiDataSet> trainingData);

    /**
     * Train the SparkComputationGraph with the specified <i>serialized MultiDataSet objects</i>. The assumption
     * here is that the PortableDataStreams are for MultiDataSet objects, one per file.
     *
     * @param network      Current network state
     * @param trainingData Data to train on
     */
    void executeTrainingMDS(SparkComputationGraph network, JavaPairRDD<String, PortableDataStream> trainingData);

    /**
     * Set whether the training statistics should be collected. Training statistics may include things like per-epoch run times,
     * time spent waiting for data, etc.
     * <p>
     * These statistics are primarily used for debugging and optimization, in order to gain some insight into what aspects
     * of network training are taking the most time.
     *
     * @param collectTrainingStats If true: collecting training statistics will be
     */
    void setCollectTrainingStats(boolean collectTrainingStats);

    /**
     * Get the current setting for collectTrainingStats
     */
    boolean getIsCollectTrainingStats();

    /**
     * Return the training statistics. Note that this may return null, unless setCollectTrainingStats has been set first
     *
     * @return Training statistics
     */
    SparkTrainingStats getTrainingStats();

    /**
     * Set the iteration listeners. These should be called after every averaging (or similar) operation in the TrainingMaster,
     * though the exact behaviour may be dependent on each IterationListener
     *
     * @param listeners Listeners to set
     */
    void setListeners(Collection<IterationListener> listeners);
}
