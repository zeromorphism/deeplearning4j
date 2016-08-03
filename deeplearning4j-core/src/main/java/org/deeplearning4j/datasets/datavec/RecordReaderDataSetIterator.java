/*
 *
 *  * Copyright 2015 Skymind,Inc.
 *  *
 *  *    Licensed under the Apache License, Version 2.0 (the "License");
 *  *    you may not use this file except in compliance with the License.
 *  *    You may obtain a copy of the License at
 *  *
 *  *        http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  *    Unless required by applicable law or agreed to in writing, software
 *  *    distributed under the License is distributed on an "AS IS" BASIS,
 *  *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  *    See the License for the specific language governing permissions and
 *  *    limitations under the License.
 *
 */

package org.deeplearning4j.datasets.datavec;

import lombok.Getter;
import org.datavec.api.io.WritableConverter;
import org.datavec.api.io.converters.SelfWritableConverter;
import org.datavec.api.io.converters.WritableConverterException;
import org.datavec.api.records.reader.RecordReader;
import org.datavec.api.records.reader.SequenceRecordReader;
import org.datavec.api.writable.Writable;
import org.datavec.common.data.NDArrayWritable;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.DataSetPreProcessor;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.util.FeatureUtil;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


/**
 * Record reader dataset iterator
 *
 * @author Adam Gibson
 */
public class RecordReaderDataSetIterator implements DataSetIterator {
    protected RecordReader recordReader;
    protected WritableConverter converter;
    protected int batchSize = 10;
    protected int maxNumBatches = -1;
    protected int batchNum = 0;
    protected int labelIndex = -1;
    protected int labelIndexTo = -1;
    protected int numPossibleLabels = -1;
    protected Iterator<List<Writable>> sequenceIter;
    protected DataSet last;
    protected boolean useCurrent = false;
    protected boolean regression = false;
    @Getter protected DataSetPreProcessor preProcessor;



    @Deprecated
    public RecordReaderDataSetIterator(RecordReader recordReader, int labelIndex, int numPossibleLabels) {
        this(recordReader, new SelfWritableConverter(), 10, labelIndex, numPossibleLabels);
    }
    @Deprecated
    public RecordReaderDataSetIterator(RecordReader recordReader, WritableConverter converter) {
        this(recordReader, converter, 10, -1, -1);
    }
    @Deprecated
    public RecordReaderDataSetIterator(RecordReader recordReader, WritableConverter converter, int labelIndex, int numPossibleLabels) {
        this(recordReader, converter, 10, labelIndex, numPossibleLabels);
    }
    @Deprecated
    public RecordReaderDataSetIterator(RecordReader recordReader) {
        this(recordReader, new SelfWritableConverter());
    }

    public RecordReaderDataSetIterator(RecordReader recordReader, WritableConverter converter, int batchSize) {
        this(recordReader, converter, batchSize, -1,
                recordReader.getLabels() == null? -1 : recordReader.getLabels().size());
    }

    public RecordReaderDataSetIterator(RecordReader recordReader, int batchSize) {
        this(recordReader, new SelfWritableConverter(), batchSize, -1,
                recordReader.getLabels() == null? -1 : recordReader.getLabels().size());
    }

    public RecordReaderDataSetIterator(RecordReader recordReader, int batchSize, int labelIndex, int numPossibleLabels) {
        this(recordReader, new SelfWritableConverter(), batchSize, labelIndex, numPossibleLabels);
    }

    public RecordReaderDataSetIterator(RecordReader recordReader, WritableConverter converter, int batchSize, int labelIndex, int numPossibleLabels, boolean regression) {
        this(recordReader, converter, batchSize, labelIndex, numPossibleLabels, -1, regression);
    }

    public RecordReaderDataSetIterator(RecordReader recordReader, WritableConverter converter, int batchSize, int labelIndex, int numPossibleLabels) {
        this(recordReader, converter, batchSize, labelIndex, numPossibleLabels, -1, false);
    }

    public RecordReaderDataSetIterator(RecordReader recordReader, int batchSize, int labelIndex, int numPossibleLabels, int maxNumBatches) {
        this(recordReader, new SelfWritableConverter(), batchSize, labelIndex, numPossibleLabels, maxNumBatches, false);
    }

    /**
     * Main constructor for multi-label regression (i.e., regression with multiple outputs)
     *
     * @param recordReader      RecordReader to get data from
     * @param labelIndexFrom    Index of the first regression target
     * @param labelIndexTo      Index of the last regression target, inclusive
     * @param batchSize         Minibatch size
     * @param regression        Require regression = true. Mainly included to avoid clashing with other constructors previously defined :/
     */
    public RecordReaderDataSetIterator(RecordReader recordReader, int batchSize, int labelIndexFrom, int labelIndexTo, boolean regression ){
        this(recordReader, new SelfWritableConverter(), batchSize, labelIndexFrom, labelIndexTo, -1, -1, regression);
    }


    public RecordReaderDataSetIterator(RecordReader recordReader, WritableConverter converter, int batchSize, int labelIndex,
                                       int numPossibleLabels, int maxNumBatches, boolean regression) {
        this(recordReader, converter, batchSize, labelIndex, labelIndex, numPossibleLabels, maxNumBatches, regression);
    }


    /**
     * Main constructor
     *
     * @param recordReader      the recordreader to use
     * @param converter         the batch size
     * @param maxNumBatches     Maximum number of batches to return
     * @param labelIndexFrom    the index of the label (for classification), or the first index of the labels for multi-output regression
     * @param labelIndexTo      only used if regression == true. The last index _inclusive_ of the multi-output regression
     * @param numPossibleLabels the number of possible labels for classification. Not used if regression == true
     * @param regression        if true: regression. If false: classification (assume labelIndexFrom is a
     */
    public RecordReaderDataSetIterator(RecordReader recordReader, WritableConverter converter, int batchSize, int labelIndexFrom,
                                       int labelIndexTo, int numPossibleLabels, int maxNumBatches, boolean regression) {
        this.recordReader = recordReader;
        this.converter = converter;
        this.batchSize = batchSize;
        this.maxNumBatches = maxNumBatches;
        this.labelIndex = labelIndexFrom;
        this.labelIndexTo = labelIndexTo;
        this.numPossibleLabels = numPossibleLabels;
        this.regression = regression;
    }


    @Override
    public DataSet next(int num) {
        if (useCurrent) {
            useCurrent = false;
            if (preProcessor != null) preProcessor.preProcess(last);
            return last;
        }

        List<DataSet> dataSets = new ArrayList<>();
        for (int i = 0; i < num; i++) {
            if (!hasNext())
                break;
            if (recordReader instanceof SequenceRecordReader) {
                if (sequenceIter == null || !sequenceIter.hasNext()) {
                    List<List<Writable>> sequenceRecord = ((SequenceRecordReader) recordReader).sequenceRecord();
                    sequenceIter = sequenceRecord.iterator();
                }

                List<Writable> record = sequenceIter.next();
                dataSets.add(getDataSet(record));
            } else {
                List<Writable> record = recordReader.next();
                dataSets.add(getDataSet(record));
            }
        }
        batchNum++;

        if(dataSets.isEmpty())
            return new DataSet();

        DataSet ret = DataSet.merge(dataSets);
        last = ret;
        if (preProcessor != null) preProcessor.preProcess(ret);
        //Add label name values to dataset
        if (recordReader.getLabels() != null) ret.setLabelNames(recordReader.getLabels());
        return ret;
    }


    private DataSet getDataSet(List<Writable> record) {
        List<Writable> currList;
        if (record instanceof List)
            currList = (List<Writable>) record;
        else
            currList = new ArrayList<>(record);

        //allow people to specify label index as -1 and infer the last possible label
        if (numPossibleLabels >= 1 && labelIndex < 0) {
            labelIndex = record.size() - 1;
        }

        INDArray label = null;
        INDArray featureVector = null;
        int featureCount = 0;
        int labelCount = 0;

        //no labels
        if(currList.size() == 2 && currList.get(1) instanceof NDArrayWritable && currList.get(0) instanceof NDArrayWritable && currList.get(0) == currList.get(1)) {
            NDArrayWritable writable = (NDArrayWritable)currList.get(0);
            return new DataSet(writable.get(),writable.get());
        }
        if(currList.size() == 2 && currList.get(0) instanceof NDArrayWritable) {
            if(!regression)
                label = FeatureUtil.toOutcomeVector((int) Double.parseDouble(currList.get(1).toString()),numPossibleLabels);
            else
                label = Nd4j.scalar(Double.parseDouble(currList.get(1).toString()));
            NDArrayWritable ndArrayWritable = (NDArrayWritable) currList.get(0);
            featureVector = ndArrayWritable.get();
            return new DataSet(featureVector,label);
        }

        for (int j = 0; j < currList.size(); j++) {
            Writable current = currList.get(j);
            //ndarray writable is an insane slow down herecd
            if (!(current instanceof  NDArrayWritable) && current.toString().isEmpty())
                continue;

            if (regression && j >= labelIndex && j <= labelIndexTo) {
                //This is the multi-label regression case
                if (label == null) label = Nd4j.create(1, (labelIndexTo - labelIndex + 1));
                label.putScalar(labelCount++, current.toDouble());
            } else if (labelIndex >= 0 && j == labelIndex) {
                //single label case (classification, etc)
                if (converter != null)
                    try {
                        current = converter.convert(current);
                    } catch (WritableConverterException e) {
                        e.printStackTrace();
                    }
                if (numPossibleLabels < 1)
                    throw new IllegalStateException("Number of possible labels invalid, must be >= 1");
                if (regression) {
                    label = Nd4j.scalar(current.toDouble());
                } else {
                    int curr = current.toInt();
                    if (curr >= numPossibleLabels)
                        curr--;
                    label = FeatureUtil.toOutcomeVector(curr, numPossibleLabels);
                }
            } else {
                try {
                    double value = current.toDouble();
                    if (featureVector == null) {
                        if(regression && labelIndex >= 0){
                            //Handle the possibly multi-label regression case here:
                            int nLabels = labelIndexTo - labelIndex + 1;
                            featureVector = Nd4j.create(1, currList.size() - nLabels);
                        } else {
                            //Classification case, and also no-labels case
                            featureVector = Nd4j.create(labelIndex >= 0 ? currList.size() - 1 : currList.size());
                        }
                    }
                    featureVector.putScalar(featureCount++, value);
                } catch (UnsupportedOperationException e) {
                    // This isn't a scalar, so check if we got an array already
                    if (current instanceof NDArrayWritable) {
                        assert featureVector == null;
                        featureVector = ((NDArrayWritable)current).get();
                    } else {
                        throw e;
                    }
                }
            }
        }

        return new DataSet(featureVector, labelIndex >= 0 ? label : featureVector);
    }

    @Override
    public int totalExamples() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int inputColumns() {
        if (last == null) {
            DataSet next = next();
            last = next;
            useCurrent = true;
            return next.numInputs();
        } else
            return last.numInputs();

    }

    @Override
    public int totalOutcomes() {
        if (last == null) {
            DataSet next = next();
            last = next;
            useCurrent = true;
            return next.numOutcomes();
        } else
            return last.numOutcomes();


    }

    @Override
    public boolean resetSupported(){
        return true;
    }

    @Override
    public void reset() {
        batchNum = 0;
        recordReader.reset();
    }

    @Override
    public int batch() {
        return batchSize;
    }

    @Override
    public int cursor() {
        throw new UnsupportedOperationException();

    }

    @Override
    public int numExamples() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setPreProcessor(org.nd4j.linalg.dataset.api.DataSetPreProcessor preProcessor) {
        this.preProcessor = preProcessor;
    }

    @Override
    public boolean hasNext() {
        return (recordReader.hasNext() && (maxNumBatches < 0 || batchNum < maxNumBatches));
    }

    @Override
    public DataSet next() {
        return next(batchSize);
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<String> getLabels() {
        return recordReader.getLabels();
    }

}
