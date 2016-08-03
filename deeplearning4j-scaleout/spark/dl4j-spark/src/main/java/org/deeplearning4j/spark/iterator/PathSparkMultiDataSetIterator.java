package org.deeplearning4j.spark.iterator;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.nd4j.linalg.dataset.api.MultiDataSet;
import org.nd4j.linalg.dataset.api.MultiDataSetPreProcessor;
import org.nd4j.linalg.dataset.api.iterator.MultiDataSetIterator;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.Collection;
import java.util.Iterator;

/**
 * A DataSetIterator that loads serialized DataSet objects (saved with {@link MultiDataSet#save(OutputStream)}) from
 * a String that represents the path (for example, on HDFS)
 *
 * @author Alex Black
 */
public class PathSparkMultiDataSetIterator implements MultiDataSetIterator {

    public static final int BUFFER_SIZE = 4194304;  //4 MB

    private final Collection<String> dataSetStreams;
    private MultiDataSetPreProcessor preprocessor;
    private Iterator<String> iter;
    private FileSystem fileSystem;

    public PathSparkMultiDataSetIterator(Iterator<String> iter){
        this.dataSetStreams = null;
        this.iter = iter;
    }

    public PathSparkMultiDataSetIterator(Collection<String> dataSetStreams){
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
        MultiDataSet ds = load(iter.next());

        if(preprocessor != null) preprocessor.preProcess(ds);
        return ds;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }


    private synchronized MultiDataSet load(String path){
        if(fileSystem == null){
            try{
                fileSystem = FileSystem.get(new URI(path), new Configuration());
            }catch(Exception e){
                throw new RuntimeException(e);
            }
        }

        MultiDataSet ds = new org.nd4j.linalg.dataset.MultiDataSet();
        try(FSDataInputStream inputStream = fileSystem.open(new Path(path), BUFFER_SIZE)){
            ds.load(inputStream);
        }catch(IOException e){
            throw new RuntimeException(e);
        }

        return ds;
    }
}
