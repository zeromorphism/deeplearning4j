package org.deeplearning4j.models.embeddings.learning.impl.sequence;

import lombok.NonNull;
import org.deeplearning4j.models.embeddings.WeightLookupTable;
import org.deeplearning4j.models.embeddings.inmemory.InMemoryLookupTable;
import org.deeplearning4j.models.embeddings.learning.SequenceLearningAlgorithm;
import org.deeplearning4j.models.embeddings.learning.impl.elements.CBOW;
import org.deeplearning4j.models.embeddings.loader.VectorsConfiguration;
import org.deeplearning4j.models.sequencevectors.interfaces.SequenceIterator;
import org.deeplearning4j.models.sequencevectors.sequence.Sequence;
import org.deeplearning4j.models.sequencevectors.sequence.SequenceElement;
import org.deeplearning4j.models.word2vec.wordstore.VocabCache;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * DM implementation for DeepLearning4j
 *
 * @author raver119@gmail.com
 */
public class DM<T extends SequenceElement> implements SequenceLearningAlgorithm<T> {
    private VocabCache<T> vocabCache;
    private WeightLookupTable<T> lookupTable;
    private VectorsConfiguration configuration;

    protected static double MAX_EXP = 6;

    protected int window;
    protected boolean useAdaGrad;
    protected double negative;
    protected double sampling;

    protected double[] expTable;

    protected INDArray syn0, syn1, syn1Neg, table;

    private CBOW<T> cbow = new CBOW<>();

    @Override
    public String getCodeName() {
        return "PV-DM";
    }

    @Override
    public void configure(@NonNull VocabCache<T> vocabCache,@NonNull WeightLookupTable<T> lookupTable,@NonNull VectorsConfiguration configuration) {
        this.vocabCache = vocabCache;
        this.lookupTable = lookupTable;
        this.configuration = configuration;

        cbow.configure(vocabCache, lookupTable, configuration);

        this.window = configuration.getWindow();
        this.useAdaGrad = configuration.isUseAdaGrad();
        this.negative = configuration.getNegative();
        this.sampling = configuration.getSampling();

        this.syn0 = ((InMemoryLookupTable<T>) lookupTable).getSyn0();
        this.syn1 = ((InMemoryLookupTable<T>) lookupTable).getSyn1();
        this.syn1Neg = ((InMemoryLookupTable<T>) lookupTable).getSyn1Neg();
        this.expTable = ((InMemoryLookupTable<T>) lookupTable).getExpTable();
        this.table = ((InMemoryLookupTable<T>) lookupTable).getTable();
    }

    @Override
    public void pretrain(SequenceIterator<T> iterator) {
        // no-op
    }

    @Override
    public double learnSequence(Sequence<T> sequence, AtomicLong nextRandom, double learningRate) {
        Sequence<T> seq = cbow.applySubsampling(sequence, nextRandom);

        List<T> labels = new ArrayList<>();
        labels.addAll(sequence.getSequenceLabels());

        if (sequence.getSequenceLabel() == null) throw new IllegalStateException("Label is NULL");

        if(seq.isEmpty() || labels.isEmpty())
            return 0;

        for (int i = 0; i < seq.size(); i++) {
            nextRandom.set(Math.abs(nextRandom.get() * 25214903917L + 11));
            dm(i, seq,  (int) nextRandom.get() % window ,nextRandom, learningRate);
        }

        return 0;
    }

    public void dm(int i, Sequence<T> sequence, int b, AtomicLong nextRandom, double alpha) {
        int end =  window * 2 + 1 - b;
        int cw = 0;
        INDArray neu1 = Nd4j.zeros(lookupTable.layerSize());


        T currentWord = sequence.getElementByIndex(i);

        for(int a = b; a < end; a++) {
            if(a != window) {
                int c = i - window + a;
                if(c >= 0 && c < sequence.size()) {
                    T lastWord = sequence.getElementByIndex(c);

                    neu1.addiRowVector(syn0.getRow(lastWord.getIndex()));
                    cw++;
                }
            }
        }

        for (T label: sequence.getSequenceLabels()) {
            neu1.addiRowVector(syn0.getRow(label.getIndex()));
            cw++;
        }

        if (cw == 0)
            return;

        neu1.divi(cw);

        INDArray neu1e = cbow.iterateSample(currentWord, neu1, nextRandom, alpha);

        for (T label: sequence.getSequenceLabels()) {
            INDArray syn0row = syn0.getRow(label.getIndex());
            Nd4j.getBlasWrapper().level1().axpy(lookupTable.layerSize(), 1.0, neu1e, syn0row);
        }

    }

    @Override
    public boolean isEarlyTerminationHit() {
        return false;
    }
}
