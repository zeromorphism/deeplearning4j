package org.deeplearning4j.spark.util;

import org.apache.commons.io.IOUtils;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.spark.SparkContext;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.storage.StorageLevel;
import org.deeplearning4j.spark.api.Repartition;
import org.deeplearning4j.spark.api.RepartitionStrategy;
import org.deeplearning4j.spark.impl.common.CountPartitionsFunction;
import org.deeplearning4j.spark.impl.common.SplitPartitionsFunction;
import org.deeplearning4j.spark.impl.common.SplitPartitionsFunction2;
import org.deeplearning4j.spark.impl.common.repartition.AssignIndexFunction;
import org.deeplearning4j.spark.impl.common.repartition.BalancedPartitioner;
import org.deeplearning4j.spark.impl.common.repartition.MapTupleToPairFlatMap;
import org.slf4j.Logger;
import scala.Tuple2;

import java.io.*;
import java.lang.reflect.Array;
import java.util.List;
import java.util.Random;

/**
 * Various utilities for Spark
 *
 * @author Alex Black
 */
public class SparkUtils {

    private SparkUtils() {
    }

    /**
     * Check the spark configuration for incorrect Kryo configuration, logging a warning message if necessary
     *
     * @param javaSparkContext Spark context
     * @param log              Logger to log messages to
     * @return True if ok (no kryo, or correct kryo setup)
     */
    public static boolean checkKryoConfiguration(JavaSparkContext javaSparkContext, Logger log) {
        //Check if kryo configuration is correct:
        String serializer = javaSparkContext.getConf().get("spark.serializer", null);
        if (serializer != null && serializer.equals("org.apache.spark.serializer.KryoSerializer")) {
            //conf.set("spark.kryo.registrator", "org.nd4j.Nd4jRegistrator");
            String kryoRegistrator = javaSparkContext.getConf().get("spark.kryo.registrator", null);
            if (kryoRegistrator == null || !kryoRegistrator.equals("org.nd4j.Nd4jRegistrator")) {
                log.warn("***** Kryo serialization detected without Nd4j Registrator *****");
                log.warn("***** ND4J Kryo registrator is required to avoid serialization (NullPointerException) issues on NDArrays *****");
                log.warn("***** Use nd4j-kryo_2.10 or _2.11 artifact, with sparkConf.set(\"spark.kryo.registrator\", \"org.nd4j.Nd4jRegistrator\"); *****");
                return false;
            }
        }
        return true;
    }

    /**
     * Write a String to a file (on HDFS or local) in UTF-8 format
     *
     * @param path    Path to write to
     * @param toWrite String to write
     * @param sc      Spark context
     */
    public static void writeStringToFile(String path, String toWrite, JavaSparkContext sc) throws IOException {
        writeStringToFile(path, toWrite, sc.sc());
    }

    /**
     * Write a String to a file (on HDFS or local) in UTF-8 format
     *
     * @param path    Path to write to
     * @param toWrite String to write
     * @param sc      Spark context
     */
    public static void writeStringToFile(String path, String toWrite, SparkContext sc) throws IOException {
        FileSystem fileSystem = FileSystem.get(sc.hadoopConfiguration());
        try (BufferedOutputStream bos = new BufferedOutputStream(fileSystem.create(new Path(path)))) {
            bos.write(toWrite.getBytes("UTF-8"));
        }
    }

    /**
     * Read a UTF-8 format String from HDFS (or local)
     *
     * @param path Path to write the string
     * @param sc   Spark context
     */
    public static String readStringFromFile(String path, JavaSparkContext sc) throws IOException {
        return readStringFromFile(path, sc.sc());
    }

    /**
     * Read a UTF-8 format String from HDFS (or local)
     *
     * @param path Path to write the string
     * @param sc   Spark context
     */
    public static String readStringFromFile(String path, SparkContext sc) throws IOException {
        FileSystem fileSystem = FileSystem.get(sc.hadoopConfiguration());
        try (BufferedInputStream bis = new BufferedInputStream(fileSystem.open(new Path(path)))) {
            byte[] asBytes = IOUtils.toByteArray(bis);
            return new String(asBytes, "UTF-8");
        }
    }

    /**
     * Write an object to HDFS (or local) using default Java object serialization
     *
     * @param path    Path to write the object to
     * @param toWrite Object to write
     * @param sc      Spark context
     */
    public static void writeObjectToFile(String path, Object toWrite, JavaSparkContext sc) throws IOException {
        writeObjectToFile(path, toWrite, sc.sc());
    }

    /**
     * Write an object to HDFS (or local) using default Java object serialization
     *
     * @param path    Path to write the object to
     * @param toWrite Object to write
     * @param sc      Spark context
     */
    public static void writeObjectToFile(String path, Object toWrite, SparkContext sc) throws IOException {
        FileSystem fileSystem = FileSystem.get(sc.hadoopConfiguration());
        try (BufferedOutputStream bos = new BufferedOutputStream(fileSystem.create(new Path(path)))) {
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(toWrite);
        }
    }

    /**
     * Read an object from HDFS (or local) using default Java object serialization
     *
     * @param path File to read
     * @param type Class of the object to read
     * @param sc   Spark context
     * @param <T>  Type of the object to read
     */
    public static <T> T readObjectFromFile(String path, Class<T> type, JavaSparkContext sc) throws IOException {
        return readObjectFromFile(path, type, sc.sc());
    }

    /**
     * Read an object from HDFS (or local) using default Java object serialization
     *
     * @param path File to read
     * @param type Class of the object to read
     * @param sc   Spark context
     * @param <T>  Type of the object to read
     */
    public static <T> T readObjectFromFile(String path, Class<T> type, SparkContext sc) throws IOException {
        FileSystem fileSystem = FileSystem.get(sc.hadoopConfiguration());
        try (ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(fileSystem.open(new Path(path))))) {
            Object o;
            try {
                o = ois.readObject();
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }

            return (T) o;
        }
    }

    /**
     * Repartition the specified RDD (or not) using the given {@link Repartition} and {@link RepartitionStrategy} settings
     *
     * @param rdd                 RDD to repartition
     * @param repartition         Setting for when repartiting is to be conducted
     * @param repartitionStrategy Setting for how repartitioning is to be conducted
     * @param objectsPerPartition Desired number of objects per partition
     * @param numPartitions       Total number of partitions
     * @param <T>                 Type of the RDD
     * @return Repartitioned RDD, or original RDD if no repartitioning was conducted
     */
    public static <T> JavaRDD<T> repartition(JavaRDD<T> rdd, Repartition repartition, RepartitionStrategy repartitionStrategy,
                                             int objectsPerPartition, int numPartitions) {
        if (repartition == Repartition.Never) return rdd;

        switch (repartitionStrategy) {
            case SparkDefault:
                if (repartition == Repartition.NumPartitionsWorkersDiffers && rdd.partitions().size() == numPartitions)
                    return rdd;

                //Either repartition always, or workers/num partitions differs
                return rdd.repartition(numPartitions);
            case Balanced:
                return repartitionBalanceIfRequired(rdd, repartition, objectsPerPartition, numPartitions);
            default:
                throw new RuntimeException("Unknown repartition strategy: " + repartitionStrategy);
        }
    }


    /**
     * Repartition a RDD (given the {@link Repartition} setting) such that we have approximately {@code numPartitions} partitions,
     * each of which has {@code objectsPerPartition} objects.
     *
     * @param rdd                 RDD to repartition
     * @param repartition         Repartitioning setting
     * @param objectsPerPartition Number of objects we want in each partition
     * @param numPartitions       Number of partitions to have
     * @param <T>                 Type of RDD
     * @return Repartitioned RDD, or the original RDD if no repartitioning was performed
     */
    public static <T> JavaRDD<T> repartitionBalanceIfRequired(JavaRDD<T> rdd, Repartition repartition, int objectsPerPartition, int numPartitions) {
        int origNumPartitions = rdd.partitions().size();
        switch (repartition) {
            case Never:
                return rdd;
            case NumPartitionsWorkersDiffers:
                if (origNumPartitions == numPartitions) return rdd;
            case Always:
                //Repartition: either always, or origNumPartitions != numWorkers

                //First: count number of elements in each partition. Need to know this so we can work out how to properly index each example,
                // so we can in turn create properly balanced partitions after repartitioning
                //Because the objects (DataSets etc) should be small, this should be OK
                rdd.persist(StorageLevel.MEMORY_ONLY());

                //Count each partition...
                List<Tuple2<Integer, Integer>> partitionCounts = rdd.mapPartitionsWithIndex(new CountPartitionsFunction<T>(), true).collect();
                int totalObjects = 0;
                int initialPartitions = partitionCounts.size();

                boolean allCorrectSize = true;
                int[] countPerPartition = new int[partitionCounts.size()];
                int x = 0;
                for (Tuple2<Integer, Integer> t2 : partitionCounts) {
                    int partitionSize = t2._2();
                    countPerPartition[x++] = partitionSize;
                    allCorrectSize &= (partitionSize == objectsPerPartition);
                    totalObjects += t2._2();
                }

                if (numPartitions * objectsPerPartition < totalObjects) {
                    int add = (totalObjects - numPartitions * objectsPerPartition) / numPartitions;
                    int mod = (totalObjects - numPartitions * objectsPerPartition) % numPartitions;
                    if (mod != 0) add++; //round up
                    objectsPerPartition += add;

                    allCorrectSize = true;
                    for (Tuple2<Integer, Integer> t2 : partitionCounts) {
                        allCorrectSize &= (t2._2() == objectsPerPartition);
                    }
                }
                if (numPartitions * objectsPerPartition < totalObjects) throw new RuntimeException();

                if (initialPartitions == numPartitions && allCorrectSize) {
                    //Don't need to do any repartitioning here - already in the format we want
                    return rdd;
                }

                //In each partition: work out the start offset (so we can work out the index of each element)
                int[] elementStartOffsetByPartitions = new int[countPerPartition.length];
                for (int i = 1; i < elementStartOffsetByPartitions.length; i++) {
                    elementStartOffsetByPartitions[i] = elementStartOffsetByPartitions[i - 1] + countPerPartition[i - 1];
                }

                //Index each element for repartitioning (can only do manual repartitioning on a JavaPairRDD)
                JavaRDD<Tuple2<Integer, T>> indexed = rdd.mapPartitionsWithIndex(new AssignIndexFunction<T>(elementStartOffsetByPartitions), true);
                JavaPairRDD<Integer, T> pairIndexed = indexed.mapPartitionsToPair(new MapTupleToPairFlatMap<Integer, T>(), true);

                int numStandardPartitions = totalObjects / objectsPerPartition;
                if (totalObjects % objectsPerPartition != 0) numStandardPartitions++; //Round up.

                pairIndexed = pairIndexed.partitionBy(new BalancedPartitioner(numPartitions, numStandardPartitions, objectsPerPartition));

                return pairIndexed.values();
            default:
                throw new RuntimeException("Unknown setting for repartition: " + repartition);
        }
    }

    /**
     * Random split the specified RDD into a number of RDDs, where each has {@code numObjectsPerSplit} in them.
     * <p>
     * This similar to how RDD.randomSplit works (i.e., split via filtering), but this should result in more
     * equal splits (instead of independent binomial sampling that is used there, based on weighting)
     * This balanced splitting approach is important when the number of DataSet objects we want in each split is small,
     * as random sampling variance of {@link JavaRDD#randomSplit(double[])} is quite large relative to the number of examples
     * in each split. Note however that this method doesn't <i>guarantee</i> that partitions will be balanced
     * <p>
     * Downside is we need total object count (whereas {@link JavaRDD#randomSplit(double[])} does not). However, randomSplit
     * requires a full pass of the data anyway (in order to do filtering upon it) so this should not add much overhead in practice
     *
     * @param totalObjectCount   Total number of objects in the RDD to split
     * @param numObjectsPerSplit Number of objects in each split
     * @param data               Data to split
     * @param <T>                Generic type for the RDD
     * @return The RDD split up (without replacetement) into a number of smaller RDDs
     */
    public static <T> JavaRDD<T>[] balancedRandomSplit(int totalObjectCount, int numObjectsPerSplit, JavaRDD<T> data) {
        return balancedRandomSplit(totalObjectCount, numObjectsPerSplit, data, new Random().nextLong());
    }

    /**
     * Equivalent to {@link #balancedRandomSplit(int, int, JavaRDD)} with control over the RNG seed
     */
    public static <T> JavaRDD<T>[] balancedRandomSplit(int totalObjectCount, int numObjectsPerSplit, JavaRDD<T> data, long rngSeed) {
        JavaRDD<T>[] splits;
        if (totalObjectCount <= numObjectsPerSplit) {
            splits = (JavaRDD<T>[]) Array.newInstance(JavaRDD.class, 1);
            splits[0] = data;
        } else {
            int numSplits = totalObjectCount / numObjectsPerSplit; //Intentional round down
            splits = (JavaRDD<T>[]) Array.newInstance(JavaRDD.class, numSplits);
            for (int i = 0; i < numSplits; i++) {
                splits[i] = data.mapPartitionsWithIndex(new SplitPartitionsFunction<T>(i, numSplits, rngSeed), true);
            }

        }
        return splits;
    }

    /**
     * Equivalent to {@link #balancedRandomSplit(int, int, JavaRDD)} but for Pair RDDs
     */
    public static <T, U> JavaPairRDD<T, U>[] balancedRandomSplit(int totalObjectCount, int numObjectsPerSplit, JavaPairRDD<T, U> data) {
        return balancedRandomSplit(totalObjectCount, numObjectsPerSplit, data, new Random().nextLong());
    }

    /**
     * Equivalent to {@link #balancedRandomSplit(int, int, JavaRDD)} but for pair RDDs, and with control over the RNG seed
     */
    public static <T, U> JavaPairRDD<T, U>[] balancedRandomSplit(int totalObjectCount, int numObjectsPerSplit, JavaPairRDD<T, U> data, long rngSeed) {
        JavaPairRDD<T, U>[] splits;
        if (totalObjectCount <= numObjectsPerSplit) {
            splits = (JavaPairRDD<T, U>[]) Array.newInstance(JavaPairRDD.class, 1);
            splits[0] = data;
        } else {
            int numSplits = totalObjectCount / numObjectsPerSplit; //Intentional round down

            splits = (JavaPairRDD<T, U>[]) Array.newInstance(JavaPairRDD.class, numSplits);
            for (int i = 0; i < numSplits; i++) {

                //What we really need is a .mapPartitionsToPairWithIndex function
                //but, of course Spark doesn't provide this
                //So we need to do a two-step process here...

                JavaRDD<Tuple2<T, U>> split = data.mapPartitionsWithIndex(new SplitPartitionsFunction2<T, U>(i, numSplits, rngSeed), true);
                splits[i] = split.mapPartitionsToPair(new MapTupleToPairFlatMap<T, U>(), true);
            }
        }
        return splits;
    }
}
