package org.deeplearning4j.spark.iterator;

import org.apache.spark.input.PortableDataStream;
import org.nd4j.linalg.dataset.api.MultiDataSet;
import org.nd4j.linalg.dataset.api.MultiDataSetPreProcessor;
import org.nd4j.linalg.dataset.api.iterator.MultiDataSetIterator;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Iterator;

/**
 * A DataSetIterator that loads serialized MultiDataSet objects (saved with {@link MultiDataSet#save(OutputStream)}) from
 * a {@link PortableDataStream}, usually obtained from SparkContext.binaryFiles()
 *
 * @author Alex Black
 */
public class PortableDataStreamMultiDataSetIterator implements MultiDataSetIterator {

    private final Collection<PortableDataStream> dataSetStreams;
    private MultiDataSetPreProcessor preprocessor;
    private Iterator<PortableDataStream> iter;

    public PortableDataStreamMultiDataSetIterator(Iterator<PortableDataStream> iter){
        this.dataSetStreams = null;
        this.iter = iter;
    }

    public PortableDataStreamMultiDataSetIterator(Collection<PortableDataStream> dataSetStreams){
        this.dataSetStreams = dataSetStreams;
        iter = dataSetStreams.iterator();
    }

    @Override
    public MultiDataSet next(int num) {
        return next();
    }

    @Override
    public boolean resetSupported(){
        return dataSetStreams != null;
    }

    @Override
    public void reset() {
        if(dataSetStreams == null) throw new IllegalStateException("Cannot reset iterator constructed with an iterator");
        iter = dataSetStreams.iterator();
    }

    @Override
    public void setPreProcessor(MultiDataSetPreProcessor preProcessor) {
        this.preprocessor = preProcessor;
    }

    @Override
    public boolean hasNext() {
        return iter.hasNext();
    }

    @Override
    public MultiDataSet next() {
        MultiDataSet ds = new org.nd4j.linalg.dataset.MultiDataSet();
        PortableDataStream pds = iter.next();
        try {
            ds.load(pds.open());
        } catch(IOException e){
            throw new RuntimeException(e);
        } finally {
            pds.close();
        }

        if(preprocessor != null) preprocessor.preProcess(ds);
        return ds;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }
}
