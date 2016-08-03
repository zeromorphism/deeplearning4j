package org.deeplearning4j.spark.util;

import org.apache.spark.api.java.JavaRDD;
import org.deeplearning4j.spark.BaseSparkTest;
import org.deeplearning4j.spark.api.Repartition;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * Created by Alex on 03/07/2016.
 */
public class TestRepartitioning extends BaseSparkTest{

    @Test
    public void testRepartitioning(){
        List<String> list = new ArrayList<>();
        for( int i=0; i<1000; i++ ){
            list.add(String.valueOf(i));
        }

        JavaRDD<String> rdd = sc.parallelize(list);
        rdd = rdd.repartition(200);

        JavaRDD<String> rdd2 = SparkUtils.repartitionBalanceIfRequired(rdd, Repartition.Always, 100, 10);
        assertFalse(rdd == rdd2);   //Should be different objects due to repartitioning

        assertEquals(10, rdd2.partitions().size());
        for( int i=0; i<10; i++ ){
            List<String> partition = rdd2.collectPartitions(new int[]{i})[0];
            System.out.println("Partition " + i + " size: " + partition.size());
            assertEquals(100, partition.size());    //Should be exactly 100, for the util method (but NOT spark .repartition)
        }
    }
}
