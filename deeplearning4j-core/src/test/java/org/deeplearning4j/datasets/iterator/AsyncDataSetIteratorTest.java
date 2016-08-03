package org.deeplearning4j.datasets.iterator;

import org.deeplearning4j.util.TestDataSetConsumer;
import org.junit.Before;
import org.junit.Test;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.factory.Nd4j;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * @author raver119@gmail.com
 */
public class AsyncDataSetIteratorTest {
    private ExistingDataSetIterator backIterator;
    private static final int TEST_SIZE = 100;
    private static final int ITERATIONS = 100;

    // time spent in consumer thread, milliseconds
    private static final long EXECUTION_TIME = 5;
    private static final long EXECUTION_SMALL = 1;

    @Before
    public void setUp() throws Exception {
        List<DataSet> iterable = new ArrayList<>();
        for (int i = 0; i < TEST_SIZE; i++) {
            iterable.add(new DataSet(Nd4j.create(new float[100]), Nd4j.create(new float[10])));
        }

        backIterator = new ExistingDataSetIterator(iterable);
    }

    @Test
    public void hasNext1() throws Exception {
        for (int iter = 0; iter < ITERATIONS; iter++) {
            for (int prefetchSize = 2; prefetchSize <= 8; prefetchSize++) {
                AsyncDataSetIterator iterator = new AsyncDataSetIterator(backIterator, prefetchSize);
                int cnt = 0;
                while (iterator.hasNext()) {
                    DataSet ds = iterator.next();

                    assertNotEquals(null, ds);
                    cnt++;
                }

                assertEquals("Failed on iteration: " + iter + ", prefetchSize: " + prefetchSize, TEST_SIZE, cnt);
            }
        }
    }

    @Test
    public void hasNextWithResetAndLoad() throws Exception {
        for (int iter = 0; iter < ITERATIONS; iter++) {
            for (int prefetchSize = 2; prefetchSize <= 8; prefetchSize++) {
                AsyncDataSetIterator iterator = new AsyncDataSetIterator(backIterator, prefetchSize);
                TestDataSetConsumer consumer = new TestDataSetConsumer(EXECUTION_SMALL);
                int cnt = 0;
                while (iterator.hasNext()) {
                    DataSet ds = iterator.next();
                    consumer.consumeOnce(ds, false);

                    cnt++;
                    if (cnt == TEST_SIZE / 2)
                        iterator.reset();
                }

                assertEquals(TEST_SIZE + (TEST_SIZE / 2), cnt);
            }
        }
    }


    @Test
    public void testWithLoad() {

        for (int iter = 0; iter < ITERATIONS; iter++) {
            AsyncDataSetIterator iterator = new AsyncDataSetIterator(backIterator, 8);
            TestDataSetConsumer consumer = new TestDataSetConsumer(iterator, EXECUTION_TIME);

            consumer.consumeWhileHasNext(true);

            assertEquals(TEST_SIZE, consumer.getCount());
        }
    }
}