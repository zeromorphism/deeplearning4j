package org.deeplearning4j.spark.impl.common.repartition;

import org.apache.spark.api.java.function.Function2;
import scala.Tuple2;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A Function2 used to assign each element in a RDD an index (integer key). This is used later in the {@link BalancedPartitioner}
 * to enable partitioning to be done in a way that is more reliable (less random) than standard .repartition calls
 *
 * @author Alex Black
 */
public class AssignIndexFunction<T> implements Function2<Integer, Iterator<T>, Iterator<Tuple2<Integer,T>>> {
    private final int[] partitionElementStartIdxs;

    /**
     * @param partitionElementStartIdxs    These are the start indexes for elements in each partition (determined from the
     *                                     number of elements in each partition). Thus length of the array must be equal
     *                                     to the number of partitions
     */
    public AssignIndexFunction(int[] partitionElementStartIdxs) {
        this.partitionElementStartIdxs = partitionElementStartIdxs;
    }

    @Override
    public Iterator<Tuple2<Integer, T>> call(Integer partionNum, Iterator<T> v2) throws Exception {
        int currIdx = partitionElementStartIdxs[partionNum];
        List<Tuple2<Integer,T>> list = new ArrayList<>();
        while(v2.hasNext()){
            list.add(new Tuple2<>(currIdx++, v2.next()));
        }
        return list.iterator();
    }
}
