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

package org.deeplearning4j.eval;

import org.deeplearning4j.berkeley.Counter;
import org.deeplearning4j.nn.api.Layer;
import org.deeplearning4j.nn.graph.ComputationGraph;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.api.shape.Shape;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.indexing.NDArrayIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.*;

/**
 * Evaluation metrics:
 * precision, recall, f1
 *
 * @author Adam Gibson
 */
public class Evaluation implements Serializable {

    protected Counter<Integer> truePositives = new Counter<>();
    protected Counter<Integer> falsePositives = new Counter<>();
    protected Counter<Integer> trueNegatives = new Counter<>();
    protected Counter<Integer> falseNegatives = new Counter<>();
    protected ConfusionMatrix<Integer> confusion;
    protected int numRowCounter = 0;
    protected List<String> labelsList = new ArrayList<>();
    protected static Logger log = LoggerFactory.getLogger(Evaluation.class);
    //What to output from the precision/recall function when we encounter an edge case
    protected static final double DEFAULT_EDGE_VALUE = 0.0;

    // Empty constructor
    public Evaluation() {
    }

    // Constructor that takes number of output classes

    /**
     * The number of classes to account
     * for in the evaluation
     * @param numClasses the number of classes to account for in the evaluation
     */
    public Evaluation(int numClasses) {
        this(createLabels(numClasses));
    }

    /**
     * The labels to include with the evaluation.
     * This constructor can be used for
     * generating labeled output rather than just
     * numbers for the labels
     * @param labels the labels to use
     *               for the output
     */
    public Evaluation(List<String> labels) {
        this.labelsList = labels;
        if(labels != null){
            createConfusion(labels.size());
        }

    }

    /**
     * Use a map to generate labels
     * Pass in a label index with the actual label
     * you want to use for output
     * @param labels a map of label index to label value
     */
    public Evaluation(Map<Integer, String> labels) {
        this(createLabelsFromMap(labels));
    }

    private static List<String> createLabels(int numClasses){
        if(numClasses == 1) numClasses = 2; //Binary (single output variable) case...
        List<String> list = new ArrayList<>(numClasses);
        for (int i = 0; i < numClasses; i++){
            list.add(String.valueOf(i));
        }
        return list;
    }

    private static List<String> createLabelsFromMap(Map<Integer,String> labels ){
        int size = labels.size();
        List<String> labelsList = new ArrayList<>(size);
        for( int i = 0; i < size; i++) {
            String str = labels.get(i);
            if(str == null) throw new IllegalArgumentException("Invalid labels map: missing key for class " + i + " (expect integers 0 to " + (size-1) + ")");
            labelsList.add(str);
        }
        return labelsList;
    }

    private void createConfusion(int nClasses) {
        List<Integer> classes = new ArrayList<>();
        for (int i = 0; i < nClasses; i++) {
            classes.add(i);
        }

        confusion = new ConfusionMatrix<>(classes);
    }


    /**
     * Evaluate the output
     * using the given true labels,
     * the input to the multi layer network
     * and the multi layer network to
     * use for evaluation
     * @param trueLabels the labels to ise
     * @param input the input to the network to use
     *              for evaluation
     * @param network the network to use for output
     */
    public void eval(INDArray trueLabels,INDArray input,ComputationGraph network) {
        eval(trueLabels,network.output(false,input)[0]);
    }


    /**
     * Evaluate the output
     * using the given true labels,
     * the input to the multi layer network
     * and the multi layer network to
     * use for evaluation
     * @param trueLabels the labels to ise
     * @param input the input to the network to use
     *              for evaluation
     * @param network the network to use for output
     */
    public void eval(INDArray trueLabels,INDArray input,MultiLayerNetwork network) {
        eval(trueLabels,network.output(input, Layer.TrainingMode.TEST));
    }


    /**
     * Collects statistics on the real outcomes vs the
     * guesses. This is for logistic outcome matrices.
     * <p>
     * Note that an IllegalArgumentException is thrown if the two passed in
     * matrices aren't the same length.
     *
     * @param realOutcomes the real outcomes (labels - usually binary)
     * @param guesses      the guesses/prediction (usually a probability vector)
     */
    public void eval(INDArray realOutcomes, INDArray guesses) {
        // Add the number of rows to numRowCounter
        numRowCounter += realOutcomes.shape()[0];

        // If confusion is null, then Evaluation was instantiated without providing the classes -> infer # classes from
        if (confusion == null) {
            int nClasses = realOutcomes.columns();
            if(nClasses == 1) nClasses = 2;     //Binary (single output variable) case
            labelsList = new ArrayList<>(nClasses);
            for( int i = 0; i<nClasses; i++ ) labelsList.add(String.valueOf(i));
            createConfusion(nClasses);
        }

        // Length of real labels must be same as length of predicted labels
        if (realOutcomes.length() != guesses.length())
            throw new IllegalArgumentException("Unable to evaluate. Outcome matrices not same length");

        // For each row get the most probable label (column) from prediction and assign as guessMax
        // For each row get the column of the true label and assign as currMax
        int nCols = realOutcomes.columns();
        for (int i = 0; i < realOutcomes.rows(); i++) {
            INDArray currRow = realOutcomes.getRow(i);
            INDArray guessRow = guesses.getRow(i);


            int currMax;
            int guessMax;
            if( nCols == 1){
                //Binary (single variable) case
                if(currRow.getDouble(i) == 0.0) currMax = 0;
                else currMax = 1;

                if(guessRow.getDouble(i) <= 0.5 ) guessMax = 0;
                else guessMax = 1;

            } else {
                //Normal case
                currMax = (int)Nd4j.argMax(currRow,1).getDouble(0);
                guessMax = (int)Nd4j.argMax(guessRow,1).getDouble(0);
            }

            // Add to the confusion matrix the real class of the row and
            // the predicted class of the row
            addToConfusion(currMax, guessMax);

            // If they are equal
            if (currMax == guessMax) {
                // Then add 1 to True Positive
                // (For a particular label)
                incrementTruePositives(guessMax);

                // And add 1 for each negative class that is accurately predicted (True Negative)
                //(For a particular label)
                for (Integer clazz : confusion.getClasses()) {
                    if (clazz != guessMax)
                        trueNegatives.incrementCount(clazz, 1.0);
                }
            } else {
                // Otherwise the real label is predicted as negative (False Negative)
                incrementFalseNegatives(currMax);
                // Otherwise the prediction is predicted as falsely positive (False Positive)
                incrementFalsePositives(guessMax);
                // Otherwise true negatives
                for (Integer clazz : confusion.getClasses()) {
                    if (clazz != guessMax && clazz != currMax)
                        trueNegatives.incrementCount(clazz, 1.0);

                }
            }
        }
    }

    /**
     * Convenience method for evaluation of time series.
     * Reshapes time series (3d) to 2d, then calls eval
     *
     * @see #eval(INDArray, INDArray)
     */
    public void evalTimeSeries(INDArray labels, INDArray predicted) {
        if (labels.rank() == 2 && predicted.rank() == 2) eval(labels, predicted);
        if (labels.rank() != 3)
            throw new IllegalArgumentException("Invalid input: labels are not rank 3 (rank=" + labels.rank() + ")");
        if (!Arrays.equals(labels.shape(), predicted.shape())) {
            throw new IllegalArgumentException("Labels and predicted have different shapes: labels="
                    + Arrays.toString(labels.shape()) + ", predicted=" + Arrays.toString(predicted.shape()));
        }

        if (labels.ordering() == 'f') labels = Shape.toOffsetZeroCopy(labels, 'c');
        if (predicted.ordering() == 'f') predicted = Shape.toOffsetZeroCopy(predicted, 'c');

        //Reshape, as per RnnToFeedForwardPreProcessor:
        int[] shape = labels.shape();
        labels = labels.permute(0, 2, 1);    //Permute, so we get correct order after reshaping
        labels = labels.reshape(shape[0] * shape[2], shape[1]);

        predicted = predicted.permute(0, 2, 1);
        predicted = predicted.reshape(shape[0] * shape[2], shape[1]);

        eval(labels, predicted);
    }

    /**
     * Evaluate a time series, whether the output is masked usind a masking array. That is,
     * the mask array specified whether the output at a given time step is actually present, or whether it
     * is just padding.<br>
     * For example, for N examples, nOut output size, and T time series length:
     * labels and predicted will have shape [N,nOut,T], and outputMask will have shape [N,T].
     *
     * @see #evalTimeSeries(INDArray, INDArray)
     */
    public void evalTimeSeries(INDArray labels, INDArray predicted, INDArray outputMask) {

        int totalOutputExamples = outputMask.sumNumber().intValue();
        int outSize = labels.size(1);

        INDArray labels2d = Nd4j.create(totalOutputExamples, outSize);
        INDArray predicted2d = Nd4j.create(totalOutputExamples, outSize);

        int rowCount = 0;
        for (int ex = 0; ex < outputMask.size(0); ex++) {
            for (int t = 0; t < outputMask.size(1); t++) {
                if (outputMask.getDouble(ex, t) == 0.0) continue;

                labels2d.putRow(rowCount, labels.get(NDArrayIndex.point(ex), NDArrayIndex.all(), NDArrayIndex.point(t)));
                predicted2d.putRow(rowCount, predicted.get(NDArrayIndex.point(ex), NDArrayIndex.all(), NDArrayIndex.point(t)));

                rowCount++;
            }
        }
        eval(labels2d, predicted2d);
    }

    /**
     * Evaluate a single prediction (one prediction at a time)
     *
     * @param predictedIdx Index of class predicted by the network
     * @param actualIdx    Index of actual class
     */
    public void eval(int predictedIdx, int actualIdx) {
        // Add the number of rows to numRowCounter
        numRowCounter++;

        // If confusion is null, then Evaluation is instantiated without providing the classes
        if (confusion == null) {
            throw new UnsupportedOperationException("Cannot evaluate single example without initializing confusion matrix first");
        }

        addToConfusion(predictedIdx, actualIdx);

        // If they are equal
        if (predictedIdx == actualIdx) {
            // Then add 1 to True Positive
            // (For a particular label)
            incrementTruePositives(predictedIdx);

            // And add 1 for each negative class that is accurately predicted (True Negative)
            //(For a particular label)
            for (Integer clazz : confusion.getClasses()) {
                if (clazz != predictedIdx)
                    trueNegatives.incrementCount(clazz, 1.0);
            }
        } else {
            // Otherwise the real label is predicted as negative (False Negative)
            incrementFalseNegatives(actualIdx);
            // Otherwise the prediction is predicted as falsely positive (False Positive)
            incrementFalsePositives(predictedIdx);
            // Otherwise true negatives
            for (Integer clazz : confusion.getClasses()) {
                if (clazz != predictedIdx && clazz != actualIdx)
                    trueNegatives.incrementCount(clazz, 1.0);

            }
        }
    }

    public String stats() {
        return stats(false);
    }

    /**
     * Method to obtain the classification report as a String
     *
     * @param suppressWarnings whether or not to output warnings related to the evaluation results
     * @return A (multi-line) String with accuracy, precision, recall, f1 score etc
     */
    public String stats(boolean suppressWarnings) {
        String actual, expected;
        StringBuilder builder = new StringBuilder().append("\n");
        StringBuilder warnings = new StringBuilder();
        List<Integer> classes = confusion.getClasses();
        for (Integer clazz : classes) {
            actual = resolveLabelForClass(clazz);
            //Output confusion matrix
            for (Integer clazz2 : classes) {
                int count = confusion.getCount(clazz, clazz2);
                if (count != 0) {
                    expected = resolveLabelForClass(clazz2);
                    builder.append(String.format("Examples labeled as %s classified by model as %s: %d times%n", actual, expected, count));
                }
            }

            //Output possible warnings regarding precision/recall calculation
            if (!suppressWarnings && truePositives.getCount(clazz) == 0) {
                if (falsePositives.getCount(clazz) == 0) {
                    warnings.append(String.format("Warning: class %s was never predicted by the model. This class was excluded from the average precision%n", actual));
                }
                if (falseNegatives.getCount(clazz) == 0) {
                    warnings.append(String.format("Warning: class %s has never appeared as a true label. This class was excluded from the average recall%n", actual));
                }
            }
        }
        builder.append("\n");
        builder.append(warnings);

        DecimalFormat df = new DecimalFormat("#.####");
        builder.append("\n==========================Scores========================================");
        builder.append("\n Accuracy:  ").append(df.format(accuracy()));
        builder.append("\n Precision: ").append(df.format(precision()));
        builder.append("\n Recall:    ").append(df.format(recall()));
        builder.append("\n F1 Score:  ").append(df.format(f1()));
        builder.append("\n========================================================================");
        return builder.toString();
    }

    private String resolveLabelForClass(Integer clazz) {
        if(labelsList != null && labelsList.size() > clazz ) return labelsList.get(clazz);
        return clazz.toString();
    }

    /**
     * Returns the precision for a given label
     *
     * @param classLabel the label
     * @return the precision for the label
     */
    public double precision(Integer classLabel) {
        return precision(classLabel, DEFAULT_EDGE_VALUE);
    }

    /**
     * Returns the precision for a given label
     *
     * @param classLabel the label
     * @param edgeCase   What to output in case of 0/0
     * @return the precision for the label
     */
    public double precision(Integer classLabel, double edgeCase) {
        double tpCount = truePositives.getCount(classLabel);
        double fpCount = falsePositives.getCount(classLabel);

        //Edge case
        if (tpCount == 0 && fpCount == 0) {
            return edgeCase;
        }

        return tpCount / (tpCount + fpCount);
    }

    /**
     * Precision based on guesses so far
     * Takes into account all known classes and outputs average precision across all of them
     *
     * @return the total precision based on guesses so far
     */
    public double precision() {
        double precisionAcc = 0.0;
        int classCount = 0;
        for (Integer classLabel : confusion.getClasses()) {
            double precision = precision(classLabel, -1);
            if (precision != -1) {
                precisionAcc += precision(classLabel);
                classCount++;
            }
        }
        return precisionAcc / (double) classCount;
    }

    /**
     * Returns the recall for a given label
     *
     * @param classLabel the label
     * @return Recall rate as a double
     */
    public double recall(Integer classLabel) {
        return recall(classLabel, DEFAULT_EDGE_VALUE);
    }

    /**
     * Returns the recall for a given label
     *
     * @param classLabel the label
     * @param edgeCase   What to output in case of 0/0
     * @return Recall rate as a double
     */
    public double recall(Integer classLabel, double edgeCase) {
        double tpCount = truePositives.getCount(classLabel);
        double fnCount = falseNegatives.getCount(classLabel);

        //Edge case
        if (tpCount == 0 && fnCount == 0) {
            return edgeCase;
        }

        return tpCount / (tpCount + fnCount);
    }

    /**
     * Recall based on guesses so far
     * Takes into account all known classes and outputs average recall across all of them
     *
     * @return the recall for the outcomes
     */
    public double recall() {
        double recallAcc = 0.0;
        int classCount = 0;
        for (Integer classLabel : confusion.getClasses()) {
            double recall = recall(classLabel, -1.0);
            if (recall != -1.0) {
                recallAcc += recall(classLabel);
                classCount++;
            }
        }
        return recallAcc / (double) classCount;
    }


    /**
     * Returns the false positive rate for a given label
     *
     * @param classLabel the label
     * @return fpr as a double
     */
    public double falsePositiveRate(Integer classLabel) {
        return recall(classLabel, DEFAULT_EDGE_VALUE);
    }

    /**
     * Returns the false positive rate for a given label
     *
     * @param classLabel the label
     * @param edgeCase   What to output in case of 0/0
     * @return fpr as a double
     */
    public double falsePositiveRate(Integer classLabel, double edgeCase) {
        double fpCount = falsePositives.getCount(classLabel);
        double tnCount = trueNegatives.getCount(classLabel);

        //Edge case
        if (fpCount == 0 && tnCount == 0) {
            return edgeCase;
        }

        return fpCount / (fpCount + tnCount);
    }

    /**
     * False positive rate based on guesses so far
     * Takes into account all known classes and outputs average fpr across all of them
     *
     * @return the fpr for the outcomes
     */
    public double falsePositiveRate() {
        double fprAlloc = 0.0;
        int classCount = 0;
        for (Integer classLabel : confusion.getClasses()) {
            double fpr = falsePositiveRate(classLabel, -1.0);
            if (fpr != -1.0) {
                fprAlloc += falsePositiveRate(classLabel);
                classCount++;
            }
        }
        return fprAlloc / (double) classCount;

    }

    /**
     * Returns the false negative rate for a given label
     *
     * @param classLabel the label
     * @return fnr as a double
     */
    public double falseNegativeRate(Integer classLabel) {
        return recall(classLabel, DEFAULT_EDGE_VALUE);
    }

    /**
     * Returns the false negative rate for a given label
     *
     * @param classLabel the label
     * @param edgeCase   What to output in case of 0/0
     * @return fnr as a double
     */
    public double falseNegativeRate(Integer classLabel, double edgeCase) {
        double fnCount = falseNegatives.getCount(classLabel);
        double tpCount = truePositives.getCount(classLabel);

        //Edge case
        if (fnCount == 0 && tpCount == 0) {
            return edgeCase;
        }

        return fnCount / (fnCount + tpCount);
    }

    /**
     * False negative rate based on guesses so far
     * Takes into account all known classes and outputs average fnr across all of them
     *
     * @return the fnr for the outcomes
     */
    public double falseNegativeRate() {
        double fnrAlloc = 0.0;
        int classCount = 0;
        for (Integer classLabel : confusion.getClasses()) {
            double fnr = falseNegativeRate(classLabel, -1.0);
            if (fnr != -1.0) {
                fnrAlloc += falseNegativeRate(classLabel);
                classCount++;
            }
        }
        return fnrAlloc / (double) classCount;
    }

    /**
     * False Alarm Rate (FAR) reflects rate of misclassified to classified records
     * http://ro.ecu.edu.au/cgi/viewcontent.cgi?article=1058&context=isw
     *
     * @return the fpr for the outcomes
     */
    public double falseAlarmRate() {
        return (falsePositiveRate() + falseNegativeRate()) / 2.0;
    }

    /**
     * Calculate f1 score for a given class
     *
     * @param classLabel the label to calculate f1 for
     * @return the f1 score for the given label
     */
    public double f1(Integer classLabel) {
        double precision = precision(classLabel);
        double recall = recall(classLabel);
        if (precision == 0 || recall == 0)
            return 0;
        return 2.0 * ((precision * recall / (precision + recall)));
    }

    /**
     * TP: true positive
     * FP: False Positive
     * FN: False Negative
     * F1 score: 2 * TP / (2TP + FP + FN)
     *
     * @return the f1 score or harmonic mean based on current guesses
     */
    public double f1() {
        double precision = precision();
        double recall = recall();
        if (precision == 0 || recall == 0)
            return 0;
        return 2.0 * ((precision * recall / (precision + recall)));
    }

    /**
     * Accuracy:
     * (TP + TN) / (P + N)
     *
     * @return the accuracy of the guesses so far
     */
    public double accuracy() {
        //Accuracy: sum the counts on the diagonal of the confusion matrix, divide by total
        int nClasses = confusion.getClasses().size();
        int countCorrect = 0;
        for (int i = 0; i < nClasses; i++) {
            countCorrect += confusion.getCount(i, i);
        }

        return countCorrect / (double)getNumRowCounter();
    }


    // Access counter methods

    /**
     * True positives: correctly rejected
     *
     * @return the total true positives so far
     */
    public Map<Integer, Integer> truePositives() {
        return convertToMap(truePositives, confusion.getClasses().size());
    }

    /**
     * True negatives: correctly rejected
     *
     * @return the total true negatives so far
     */
    public Map<Integer, Integer> trueNegatives() {
        return convertToMap(trueNegatives, confusion.getClasses().size());
    }

    /**
     * False positive: wrong guess
     *
     * @return the count of the false positives
     */
    public Map<Integer, Integer> falsePositives() {
        return convertToMap(falsePositives, confusion.getClasses().size());
    }

    /**
     * False negatives: correctly rejected
     *
     * @return the total false negatives so far
     */
    public Map<Integer, Integer> falseNegatives() {
        return convertToMap(falseNegatives, confusion.getClasses().size());
    }

    /**
     * Total negatives true negatives + false negatives
     *
     * @return the overall negative count
     */
    public Map<Integer, Integer> negative() {
        return addMapsByKey(trueNegatives(), falsePositives());
    }

    /**
     * Returns all of the positive guesses:
     * true positive + false negative
     */
    public Map<Integer, Integer> positive() {
        return addMapsByKey(truePositives(), falseNegatives());
    }

    private Map<Integer, Integer> convertToMap(Counter<Integer> counter, int maxCount) {
        Map<Integer, Integer> map = new HashMap<>();
        for (int i = 0; i < maxCount; i++) {
            map.put(i, (int) counter.getCount(i));
        }
        return map;
    }

    private Map<Integer, Integer> addMapsByKey(Map<Integer, Integer> first, Map<Integer, Integer> second) {
        Map<Integer, Integer> out = new HashMap<>();
        Set<Integer> keys = new HashSet<>(first.keySet());
        keys.addAll(second.keySet());

        for (Integer i : keys) {
            Integer f = first.get(i);
            Integer s = second.get(i);
            if (f == null) f = 0;
            if (s == null) s = 0;

            out.put(i, f + s);
        }

        return out;
    }


    // Incrementing counters
    public void incrementTruePositives(Integer classLabel) {
        truePositives.incrementCount(classLabel, 1.0);
    }

    public void incrementTrueNegatives(Integer classLabel) {
        trueNegatives.incrementCount(classLabel, 1.0);
    }

    public void incrementFalseNegatives(Integer classLabel) {
        falseNegatives.incrementCount(classLabel, 1.0);
    }

    public void incrementFalsePositives(Integer classLabel) {
        falsePositives.incrementCount(classLabel, 1.0);
    }


    // Other misc methods

    /**
     * Adds to the confusion matrix
     *
     * @param real  the actual guess
     * @param guess the system guess
     */
    public void addToConfusion(Integer real, Integer guess) {
        confusion.add(real, guess);
    }

    /**
     * Returns the number of times the given label
     * has actually occurred
     *
     * @param clazz the label
     * @return the number of times the label
     * actually occurred
     */
    public int classCount(Integer clazz) {
        return confusion.getActualTotal(clazz);
    }

    public int getNumRowCounter() {
        return numRowCounter;
    }

    public String getClassLabel(Integer clazz) {
        return resolveLabelForClass(clazz);
    }

    /**
     * Returns the confusion matrix variable
     *
     * @return confusion matrix variable for this evaluation
     */
    public ConfusionMatrix<Integer> getConfusionMatrix() {
        return confusion;
    }

    /**
     * Merge the other evaluation object into this one. The result is that this Evaluation instance contains the counts
     * etc from both
     *
     * @param other Evaluation object to merge into this one.
     */
    public void merge(Evaluation other) {
        if (other == null) return;

        truePositives.incrementAll(other.truePositives);
        falsePositives.incrementAll(other.falsePositives);
        trueNegatives.incrementAll(other.trueNegatives);
        falseNegatives.incrementAll(other.falseNegatives);

        if (confusion == null) {
            if (other.confusion != null) confusion = new ConfusionMatrix<>(other.confusion);
        } else {
            if (other.confusion != null) confusion.add(other.confusion);
        }
        numRowCounter += other.numRowCounter;
        if (labelsList.isEmpty()) labelsList.addAll(other.labelsList);
    }

    /**
     * Get a String representation of the confusion matrix
     */
    public String confusionToString() {
        int nClasses = confusion.getClasses().size();

        //First: work out the longest label size
        int maxLabelSize = 0;
        for (String s : labelsList) {
            maxLabelSize = Math.max(maxLabelSize, s.length());
        }

        //Build the formatting for the rows:
        int labelSize = Math.max(maxLabelSize + 5, 10);
        StringBuilder sb = new StringBuilder();
        sb.append("%-3d");
        sb.append("%-");
        sb.append(labelSize);
        sb.append("s | ");

        StringBuilder headerFormat = new StringBuilder();
        headerFormat.append("   %-").append(labelSize).append("s   ");

        for (int i = 0; i < nClasses; i++) {
            sb.append("%7d");
            headerFormat.append("%7d");
        }
        String rowFormat = sb.toString();


        StringBuilder out = new StringBuilder();
        //First: header row
        Object[] headerArgs = new Object[nClasses + 1];
        headerArgs[0] = "Predicted:";
        for (int i = 0; i < nClasses; i++) headerArgs[i + 1] = i;
        out.append(String.format(headerFormat.toString(), headerArgs)).append("\n");

        //Second: divider rows
        out.append("   Actual:\n");

        //Finally: data rows
        for (int i = 0; i < nClasses; i++) {

            Object[] args = new Object[nClasses + 2];
            args[0] = i;
            args[1] = labelsList.get(i);
            for (int j = 0; j < nClasses; j++) {
                args[j + 2] = confusion.getCount(i, j);
            }
            out.append(String.format(rowFormat, args));
            out.append("\n");
        }

        return out.toString();
    }

}
