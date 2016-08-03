package org.deeplearning4j.models.sequencevectors.transformers.impl;

import lombok.NonNull;
import org.deeplearning4j.models.sequencevectors.sequence.Sequence;
import org.deeplearning4j.models.sequencevectors.transformers.SequenceTransformer;
import org.deeplearning4j.models.word2vec.VocabWord;
import org.deeplearning4j.models.word2vec.wordstore.VocabCache;
import org.deeplearning4j.text.documentiterator.BasicLabelAwareIterator;
import org.deeplearning4j.text.documentiterator.DocumentIterator;
import org.deeplearning4j.text.documentiterator.LabelAwareIterator;
import org.deeplearning4j.text.documentiterator.LabelledDocument;
import org.deeplearning4j.text.sentenceiterator.SentenceIterator;
import org.deeplearning4j.text.tokenization.tokenizer.Tokenizer;
import org.deeplearning4j.text.tokenization.tokenizerfactory.TokenizerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This simple class is responsible for conversion lines of text to Sequences of SequenceElements to fit them into SequenceVectors model
 *
 * @author raver119@gmail.com
 */
public class SentenceTransformer implements SequenceTransformer<VocabWord, String>, Iterable<Sequence<VocabWord>>{
    /*
            So, we must accept any SentenceIterator implementations, and build vocab out of it, and use it for further transforms between text and Sequences
     */
    protected TokenizerFactory tokenizerFactory;
    protected LabelAwareIterator iterator;
    protected boolean readOnly = false;
    protected AtomicInteger sentenceCounter = new AtomicInteger(0);

    protected static final Logger log = LoggerFactory.getLogger(SentenceTransformer.class);

    private SentenceTransformer(@NonNull LabelAwareIterator iterator) {
        this.iterator = iterator;
    }

    @Override
    public Sequence<VocabWord> transformToSequence(String object) {
        Sequence<VocabWord> sequence = new Sequence<>();

        //log.info("Tokenizing string: '" + object + "'");

        Tokenizer tokenizer = tokenizerFactory.create(object);
        List<String> list = tokenizer.getTokens();

        for (String token: list) {
            if (token == null || token.isEmpty() || token.trim().isEmpty()) continue;

           VocabWord word = new VocabWord(1.0, token);
            sequence.addElement(word);
        }

        sequence.setSequenceId(sentenceCounter.getAndIncrement());
        return sequence;
    }

    @Override
    public Iterator<Sequence<VocabWord>> iterator() {
        iterator.reset();

        return new Iterator<Sequence<VocabWord>>() {
            @Override
            public boolean hasNext() {
                return SentenceTransformer.this.iterator.hasNextDocument();
            }

            @Override
            public Sequence<VocabWord> next() {
                LabelledDocument document = iterator.nextDocument();
                if  (document.getContent() == null) return new Sequence<>();
                Sequence<VocabWord> sequence = SentenceTransformer.this.transformToSequence(document.getContent());

                if (document.getLabel() != null && !document.getLabel().isEmpty()) {
                    sequence.setSequenceLabel(new VocabWord(1.0, document.getLabel()));
                }

                return sequence;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    public static class Builder {
        protected TokenizerFactory tokenizerFactory;
        protected LabelAwareIterator iterator;
        protected VocabCache<VocabWord> vocabCache;
        protected boolean readOnly = false;

        public Builder() {

        }

        public Builder tokenizerFactory(@NonNull TokenizerFactory tokenizerFactory) {
            this.tokenizerFactory = tokenizerFactory;
            return this;
        }

        public Builder iterator(@NonNull LabelAwareIterator iterator) {
            this.iterator = iterator;
            return this;
        }

        public Builder iterator(@NonNull SentenceIterator iterator) {
            this.iterator = new BasicLabelAwareIterator.Builder(iterator).build();
            return this;
        }

        public Builder iterator(@NonNull DocumentIterator iterator) {
            this.iterator = new BasicLabelAwareIterator.Builder(iterator).build();
            return this;
        }

        public Builder readOnly(boolean readOnly) {
            this.readOnly = true;
            return this;
        }

        public SentenceTransformer build() {
            SentenceTransformer transformer = new SentenceTransformer(this.iterator);
            transformer.tokenizerFactory = this.tokenizerFactory;
            transformer.readOnly = this.readOnly;

            return transformer;
        }
    }
}
